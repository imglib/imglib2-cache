package net.imglib2.cache;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.RemovalListener;
import net.imglib2.cache.ref.SoftRefListenableCache;

/**
 * Handle concurrent loading and saving of cache entries. It can be used
 * directly as a {@link RemovalListener} and {@link CacheLoader}. The
 * {@link #onRemoval(Object, Object)} method enqueues values for writing and
 * returns immediately. Actual writing is done asynchronously on separate
 * threads, calling the connected {@link RemovalListener}.
 * <p>
 * {@link IoSync} takes care of directly returning values that reloaded are
 * while they are written. It ensures that the final state of a value that is
 * enqueued for removal several times is the state that is written (eventually).
 * </p>
 * <p>
 * A crucial assumption is that only one thread calls get {@link #get(Object)}
 * {@link #onRemoval(Object, Object)} with the same key simultaneously. The
 * current {@link SoftRefListenableCache} implementation guarantees that. The
 * same is guaranteed to the connected {@link RemovalListener} and
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
 *
 * @author Tobias Pietzsch
 */
public class IoSync< K, V > implements CacheLoader< K, V >, RemovalListener< K, V >
{
	final CacheLoader< K, V > loader;

	final RemovalListener< K, V > saver;

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
	final BlockingQueue< K > queue;

	public IoSync(
			final CacheLoader< K, V > loader,
			final RemovalListener< K, V > saver )
	{
		this.saver = saver;
		this.loader = loader;
		map = new ConcurrentHashMap<>();
		queue = new LinkedBlockingQueue<>();

		new Thread( new Writer() ).start();
	}

	@Override
	public void onRemoval( final K key, final V value )
	{
		map.compute( key, ( k, oldEntry ) ->
		{
			if ( oldEntry == null )
			{
				return new Entry( value, 0 );
			}
			else
			{
				assert( oldEntry.value == value );

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
	public V get( final K key ) throws Exception
	{
		final Entry entry = map.get( key );
		if ( entry != null )
			return entry.value;
		else
			return loader.get( key );
	}

	class Entry
	{
		final V value;

		volatile int generation;

		Entry( final V value, final int generation )
		{
			this.value = value;
			this.generation = generation;
		}

		@Override
		public int hashCode()
		{
			return value.hashCode();
		}

		@Override
		public boolean equals( final Object obj )
		{
			if ( obj instanceof IoSync.Entry )
			{
				@SuppressWarnings( "unchecked" )
				final Entry other = ( Entry ) obj;
				return other.value.equals( this.value ) && other.generation == this.generation;
			}
			return false;
		}
	}

	class Writer implements Runnable
	{
		@Override
		public void run()
		{
			while ( !Thread.currentThread().isInterrupted() )
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
							final V value = entry.value;
							saver.onRemoval( key, value );

							/*
							 * Because of the implementation of Entry.equals,
							 * this will only remove the entry if the generation
							 * has not been incremented since we started.
							 */
							map.remove( key, new Entry( value, writeGeneration ) );
						}
					}
				}
				catch ( final InterruptedException e )
				{
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}
