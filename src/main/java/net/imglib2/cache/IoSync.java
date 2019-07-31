package net.imglib2.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import net.imglib2.cache.ref.SoftRefLoaderRemoverCache;

/**
 * Handle concurrent loading and saving of cache entries. It can be used
 * directly as a {@link CacheRemover} and {@link CacheLoader}. The
 * {@link #onRemoval(Object, Object)} method enqueues values for writing and
 * returns immediately. Actual writing is done asynchronously on separate
 * threads, calling the connected {@link CacheRemover}.
 * <p>
 * {@link IoSync} takes care of directly returning values that are reloaded
 * while they are written. It ensures that the final state of a value that is
 * enqueued for removal several times is the state that is written (eventually).
 * </p>
 * <p>
 * A crucial assumption is that only one thread calls get {@link #get(Object)}
 * {@link #onRemoval(Object, Object)} with the same key simultaneously. The
 * current {@link SoftRefLoaderRemoverCache} implementation guarantees that. The
 * same is guaranteed to the connected {@link CacheRemover} and
 * {@link CacheLoader}.
 * </p>
 * <p>
 * TODO: If we want to avoid ever having corrupt data on disk, then get() should
 * block while existing value is written, i.e., synchronize both Writer and
 * get() on entry. Otherwise the value currently being written is returned and
 * potentially modified. If we don't care about corrupt disk data, only
 * consistency of data in memory, then this is not a problem.
 * </p>
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 * @param <D>
 *            value data type
 *
 * @author Tobias Pietzsch
 */
public class IoSync< K, V, D > implements CacheLoader< K, V >, CacheRemover< K, V, D >
{
	final CacheLoader< K, V > loader;

	final CacheRemover< K, V, D > saver;

	/**
	 * A hash map containing key-value pairs that are enqueued for writing. This
	 * is used to retrieve values from writing threads and to short-circuit
	 * loading if a value that is about to be written is requested.
	 */
	final ConcurrentHashMap< K, Entry > map;

	/**
	 * Keys to be written are enqueued here. The corresponding values can be
	 * obtained from {@link #map}.
	 */
	final PausableQueue< K > queue;

	/**
	 * Create a new {@link IoSync} that asynchronously forwards to the specified
	 * {@link CacheRemover}. Uses 1 writer thread and a bounded write queue
	 * of size 10.
	 *
	 * @param io
	 *            used to asynchronously write removed values, and to load
	 *            values that are <em>not</em> currently enqueued for writing.
	 */
	public < T extends CacheLoader< K, V > & CacheRemover< K, V, D > > IoSync( final T io )
	{
		this( io, io, 1, 10 );
	}

	/**
	 * Create a new {@link IoSync} that asynchronously forwards to the specified
	 * {@link CacheRemover}. The specified number of {@link Writer} threads
	 * is started to handle writing values through {@code saver}.
	 *
	 * @param io
	 *            used to asynchronously write removed values, and to load
	 *            values that are <em>not</em> currently enqueued for writing.
	 * @param numThreads
	 *            how many writer threads to start (may be 0).
	 * @param maxQueueSize
	 *            the maximum size of the write queue. When the queue is full,
	 *            {@link CacheRemover#onRemoval(Object, Object)} will block
	 *            until earlier values have been written.
	 */
	public < T extends CacheLoader< K, V > & CacheRemover< K, V, D > > IoSync(
			final T io,
			final int numThreads,
			final int maxQueueSize )
	{
		this( io, io, numThreads, maxQueueSize );
	}

	/**
	 * Create a new {@link IoSync} that asynchronously forwards to the specified
	 * {@link CacheRemover}. The specified number of {@link Writer} threads
	 * is started to handle writing values through {@code saver}.
	 *
	 * @param loader
	 *            used to load values that are <em>not</em> currently enqueued
	 *            for writing.
	 * @param saver
	 *            used to asynchronously write removed values.
	 * @param numThreads
	 *            how many writer threads to start (may be 0).
	 * @param maxQueueSize
	 *            the maximum size of the write queue. When the queue is full,
	 *            {@link CacheRemover#onRemoval(Object, Object)} will block
	 *            until earlier values have been written.
	 */
	public IoSync(
			final CacheLoader< K, V > loader,
			final CacheRemover< K, V, D > saver,
			final int numThreads,
			final int maxQueueSize )
	{
		this.saver = saver;
		this.loader = loader;
		map = new ConcurrentHashMap<>();
		queue = new PausableQueue<>( maxQueueSize, numThreads, false );

		final String[] names = createThreadNames( numThreads );
		for ( int i = 0; i < numThreads; ++i )
		{
			final Writer t = new Writer( names[ i ] );
			t.setDaemon( true );
			t.start();
		}
	}

	@Override
	public void onRemoval( final K key, final D valueData )
	{
		map.compute( key, ( k, oldEntry ) ->
		{
			if ( oldEntry == null )
			{
				return new Entry( valueData, 0 );
			}
			else
			{
				assert( oldEntry.valueData == valueData );

				oldEntry.generation++;
				return oldEntry;
			}
		} );
		try
		{
			queue.put( key );
		}
		catch( final InterruptedException e )
		{
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public D extract( final V value )
	{
		return saver.extract( value );
	}

	@Override
	public V reconstruct( final K key, final D valueData )
	{
		return saver.reconstruct( key, valueData );
	}

	@Override
	public V get( final K key ) throws Exception
	{
		final Entry entry = map.get( key );
		if ( entry != null )
			return reconstruct( key, entry.valueData );
		else
			return loader.get( key );
	}

	class Entry
	{
		final D valueData;

		volatile int generation;

		Entry( final D valueData, final int generation )
		{
			this.valueData = valueData;
			this.generation = generation;
		}

		@Override
		public int hashCode()
		{
			return valueData.hashCode();
		}

		@Override
		public boolean equals( final Object obj )
		{
			if ( obj instanceof IoSync.Entry )
			{
				@SuppressWarnings( "unchecked" )
				final Entry other = ( Entry ) obj;
				return other.valueData.equals( this.valueData ) && other.generation == this.generation;
			}
			return false;
		}
	}

	/**
	 * Cancel any enqueued write for {@code key}. Blocks until all in-progress
	 * writes for {@code key} are finished.
	 *
	 * @param key
	 *            key of the entry to remove
	 */
	@Override
	public void invalidate( final K key )
	{
		invalidateLock.lock();
		try
		{
			queue.pause();
			queue.remove( key );
			map.remove( key );
			saver.invalidate( key );
			queue.resume();
		}
		catch ( final InterruptedException e )
		{
			Thread.currentThread().interrupt();
		}
		finally
		{
			invalidateLock.unlock();
		}
	}

	/**
	 * Cancel all enqueued writes for keys matching {@code condition}. Blocks
	 * until all in-progress writes for keys matching {@code condition} are
	 * finished.
	 *
	 * @param parallelismThreshold
	 *            the (estimated) number of entries in the cache needed for this
	 *            operation to be executed in parallel
	 * @param condition
	 *            condition on keys of entries to remove
	 */
	@Override
	public void invalidateIf( final long parallelismThreshold, final Predicate< K > condition )
	{
		invalidateLock.lock();
		try
		{
			queue.pause();
			queue.removeIf( condition );
			map.keySet().removeIf( condition );
			saver.invalidateIf( parallelismThreshold, condition );
			queue.resume();
		}
		catch ( final InterruptedException e )
		{
			Thread.currentThread().interrupt();
		}
		finally
		{
			invalidateLock.unlock();
		}
	}

	/**
	 * Cancel all enqueued writes. Blocks until in-progress writes are finished.
	 *
	 * @param parallelismThreshold
	 *            the (estimated) number of entries in the cache needed for this
	 *            operation to be executed in parallel
	 */
	@Override
	public void invalidateAll( final long parallelismThreshold )
	{
		invalidateLock.lock();
		try
		{
			queue.pause();
			queue.clear();
			map.clear();
			saver.invalidateAll( parallelismThreshold );
			queue.resume();
		}
		catch ( final InterruptedException e )
		{
			Thread.currentThread().interrupt();
		}
		finally
		{
			invalidateLock.unlock();
		}
	}

	private final Lock invalidateLock = new ReentrantLock();

	class Writer extends Thread
	{
		public Writer( final String name )
		{
			super( name );
		}

		@Override
		public void run()
		{
			while ( true )
			{
				try
				{
					final K key = queue.take();
					final Entry entry = map.get( key );
					if ( entry != null )
					{
						/*
						 * Synchronization is only between Writers: Only one
						 * Writer can write data for the same key
						 * simultaneously.
						 */
						synchronized ( entry )
						{
							final int writeGeneration = entry.generation;
							final D valueData = entry.valueData;
							saver.onRemoval( key, valueData );

							/*
							 * Because of the implementation of Entry.equals,
							 * this will only remove the entry if the generation
							 * has not been incremented since we started.
							 */
							map.remove( key, new Entry( valueData, writeGeneration ) );
						}
					}
				}
				catch ( final InterruptedException e )
				{}
			}
		}
	}

	static final AtomicInteger ioSyncNumber = new AtomicInteger( 1 );

	static String[] createThreadNames( final int numThreads )
	{
		final String threadNameFormat = String.format(
				"io-sync-%d-writer-%%d",
				ioSyncNumber.getAndIncrement() );

		final String[] names = new String[ numThreads ];
		for ( int i = 0; i < numThreads; ++i )
			names[ i ] = String.format( threadNameFormat, ( i + 1 ) );

		return names;
	}
}
