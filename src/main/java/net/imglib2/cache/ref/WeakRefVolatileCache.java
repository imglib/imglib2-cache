package net.imglib2.cache.ref;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import net.imglib2.cache.LoaderCache;
import net.imglib2.cache.iotiming.CacheIoTiming;
import net.imglib2.cache.iotiming.IoStatistics;
import net.imglib2.cache.iotiming.IoTimeBudget;
import net.imglib2.cache.queue.BlockingFetchQueues;
import net.imglib2.cache.queue.FetcherThreads;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.VolatileLoaderCache;
import net.imglib2.cache.volatiles.VolatileCacheLoader;

public class WeakRefVolatileCache< K, V > implements VolatileLoaderCache< K, V >
{
	final ConcurrentHashMap< K, Entry > map = new ConcurrentHashMap<>();

	final ReferenceQueue< V > queue = new ReferenceQueue<>();

	final LoaderCache< K, V > backingCache;

	final BlockingFetchQueues< Callable< ? > > fetchQueue;

	/*
	 * Possible states of CacheWeakReference.loaded
	 */
	static final int NOTLOADED = 0;
	static final int INVALID = 1;
	static final int VALID = 2;

	final class CacheWeakReference extends WeakReference< V >
	{
		private final Entry entry;

		final int loaded;

		public CacheWeakReference( final V referent )
		{
			super( referent );
			entry = null;
			loaded = NOTLOADED;
		}

		public CacheWeakReference( final V referent, final Entry entry, final int loaded )
		{
			super( referent, queue );
			this.entry = entry;
			this.loaded = loaded;
		}

		public void clean()
		{
			entry.clean( this );
		}
	}

	final class Entry
	{
		final K key;

		CacheWeakReference ref;

		long enqueueFrame;

		VolatileCacheLoader< ? super K, ? extends V > loader;

		public Entry( final K key, final VolatileCacheLoader< ? super K, ? extends V > loader )
		{
			this.key = key;
			this.loader = loader;
			this.ref = new CacheWeakReference( null );
			this.enqueueFrame = -1;
		}

		public V getValue()
		{
			return ref.get();
		}

		public void setInvalid( final V value )
		{
			ref = new CacheWeakReference( value, this, INVALID );
		}

		// Precondition: caller must hold lock on this.
		public void setValid( final V value )
		{
			ref = new CacheWeakReference( value, this, VALID );
			loader = null;
			enqueueFrame = Long.MAX_VALUE;
			notifyAll();
		}

		public void clean( final CacheWeakReference ref )
		{
			if ( ref == this.ref )
				map.remove( key, this );
		}

		public V tryCreateInvalid() throws ExecutionException
		{
			try
			{
				return loader.createInvalid( key );
			}
			catch ( final InterruptedException e )
			{
				Thread.currentThread().interrupt();
				throw new ExecutionException( e );
			}
			catch ( final Exception e )
			{
				throw new ExecutionException( e );
			}
		}
	}

	public WeakRefVolatileCache(
			final LoaderCache< K, V > backingCache,
			final BlockingFetchQueues< Callable< ? > > fetchQueue )
	{
		this.fetchQueue = fetchQueue;
		this.backingCache = backingCache;
	}

	@Override
	public V getIfPresent( final Object key, final CacheHints hints ) throws ExecutionException
	{
		final Entry entry = map.get( key );
		if ( entry == null )
			return null;

		final CacheWeakReference ref = entry.ref;
		final V v = ref.get();
		if ( v != null && ref.loaded == VALID )
			return v;

		cleanUp();
		switch ( hints.getLoadingStrategy() )
		{
		case BLOCKING:
			return getBlocking( entry );
		case BUDGETED:
			if ( estimatedBugdetTimeLeft( hints ) > 0 )
				return getBudgeted( entry, hints );
		case VOLATILE:
			enqueue( entry, hints );
		case DONTLOAD:
		default:
			return v;
		}
	}

	@Override
	public V get( final K key, final VolatileCacheLoader< ? super K, ? extends V > loader, final CacheHints hints ) throws ExecutionException
	{
		/*
		 * Get existing entry for key or create it.
		 */
		final Entry entry = map.computeIfAbsent( key, k -> new Entry( k, loader ) );

		final CacheWeakReference ref = entry.ref;
		V v = ref.get();
		if ( v != null && ref.loaded == VALID )
			return v;

		cleanUp();
		switch ( hints.getLoadingStrategy() )
		{
		case BLOCKING:
			v = getBlocking( entry );
			break;
		case BUDGETED:
			v = getBudgeted( entry, hints );
			break;
		case VOLATILE:
			v = getVolatile( entry, hints );
			break;
		case DONTLOAD:
			v = getDontLoad( entry );
			break;
		}

		if ( v == null )
			return get( key, loader, hints );
		else
			return v;
	}

	/**
	 * Remove entries from the cache whose references have been
	 * garbage-collected.
	 */
	public void cleanUp()
	{
		while ( true )
		{
			@SuppressWarnings( "unchecked" )
			final CacheWeakReference poll = ( CacheWeakReference ) queue.poll();
			if ( poll == null )
				break;
			poll.clean();
		}
	}

	@Override
	public void invalidateAll()
	{
		// TODO
		throw new UnsupportedOperationException( "not implemented yet" );
	}

	// ================ private methods =====================

	private V getDontLoad( final Entry entry ) throws ExecutionException
	{
		synchronized( entry )
		{
			V v = entry.getValue();
			if ( v != null )
				return v;

			if ( entry.ref.loaded != NOTLOADED )
			{
				map.remove( entry.key, entry );
				return null;
			}

			final V vl = backingCache.getIfPresent( entry.key );
			if ( vl != null )
			{
				entry.setValid( vl );
				return vl;
			}

			v = entry.tryCreateInvalid();
			entry.setInvalid( v );
			return v;
		}
	}

	private V getVolatile( final Entry entry, final CacheHints hints ) throws ExecutionException
	{
		synchronized( entry )
		{
			final CacheWeakReference ref = entry.ref;
			V v = ref.get();
			if ( v == null && ref.loaded != NOTLOADED )
			{
				map.remove( entry.key, entry );
				return null;
			}

			if ( ref.loaded == VALID )
				return v;

			final V vl = backingCache.getIfPresent( entry.key );
			if ( vl != null )
			{
				entry.setValid( vl );
				return vl;
			}

			if ( ref.loaded == NOTLOADED )
			{
				v = entry.tryCreateInvalid();
				entry.setInvalid( v );
			}

			enqueue( entry, hints );
			return v;
		}
	}

	private V getBudgeted( final Entry entry, final CacheHints hints ) throws ExecutionException
	{
		synchronized( entry )
		{
			CacheWeakReference ref = entry.ref;
			V v = ref.get();
			if ( v == null && ref.loaded != NOTLOADED )
			{
//				printEntryCollected( "map.remove getBudgeted 1", entry );
				map.remove( entry.key, entry );
				return null;
			}

			if ( ref.loaded == VALID )
				return v;

			enqueue( entry, hints );

			final int priority = hints.getQueuePriority();
			final IoStatistics stats = CacheIoTiming.getIoStatistics();
			final IoTimeBudget budget = stats.getIoTimeBudget();
			final long timeLeft = budget.timeLeft( priority );
			if ( timeLeft > 0 )
			{
				final long t0 = stats.getIoNanoTime();
				stats.start();
				try
				{
					entry.wait( timeLeft  / 1000000l, 1 );
					// releases and re-acquires entry lock
				}
				catch ( final InterruptedException e )
				{}
				stats.stop();
				final long t = stats.getIoNanoTime() - t0;
				budget.use( t, priority );
			}

			ref = entry.ref;
			v = ref.get();
			if ( v == null )
			{
				if ( ref.loaded == NOTLOADED )
				{
					v = entry.tryCreateInvalid();
					entry.setInvalid( v );
					return v;
				}
				else
				{
//					printEntryCollected( "map.remove getBudgeted 2", entry );
					map.remove( entry.key, entry );
					return null;
				}
			}
			return v;
		}
	}

	private V getBlocking( final Entry entry ) throws ExecutionException
	{
		VolatileCacheLoader< ? super K, ? extends V > loader;
		synchronized( entry )
		{
			final CacheWeakReference ref = entry.ref;
			final V v = ref.get();
			if ( v == null && ref.loaded != NOTLOADED )
			{
//				printEntryCollected( "map.remove getBlocking 1", entry );
				map.remove( entry.key, entry );
				return null;
			}

			if ( ref.loaded == VALID ) // v.isValid()
				return v;

			loader = entry.loader;
		}
		final V vl = backingCache.get( entry.key, loader );
		synchronized( entry )
		{
			final CacheWeakReference ref = entry.ref;
			final V v = ref.get();
			if ( v == null && ref.loaded != NOTLOADED )
			{
//				printEntryCollected( "map.remove getBlocking 2", entry );
				map.remove( entry.key, entry );
				return null;
			}

			if ( ref.loaded == VALID ) // v.isValid()
				return v;

			// entry.loaded == INVALID
			entry.setValid( vl );
			return vl;
		}
	}

	/**
	 * {@link Callable} to put into the fetch queue. Loads data for a specific key.
	 */
	final class FetchEntry implements Callable< Void >
	{
		final K key;

		public FetchEntry( final K key )
		{
			this.key = key;
		}

		/**
		 * If this key's entry is not yet valid, then load it. After the method
		 * returns, the entry is guaranteed to be valid.
		 *
		 * @throws ExecutionException
		 *             if the entry could not be loaded. If the queue is handled
		 *             by {@link FetcherThreads} then loading will be retried
		 *             until it succeeds.
		 */
		@Override
		public Void call() throws ExecutionException
		{
			final Entry entry = map.get( key );
			if ( entry != null )
				getBlocking( entry );
			return null;
		}
	}

	/**
	 * Enqueue the {@link Entry} if it hasn't been enqueued for this frame
	 * already.
	 */
	private void enqueue( final Entry entry, final CacheHints hints )
	{
		final long currentQueueFrame = fetchQueue.getCurrentFrame();
		if ( entry.enqueueFrame < currentQueueFrame )
		{
			entry.enqueueFrame = currentQueueFrame;
			fetchQueue.put( new FetchEntry( entry.key ), hints.getQueuePriority(), hints.isEnqueuToFront() );
		}
	}

	/**
	 * Estimate of how much time is left for budgeted loading.
	 *
	 * @param hints
	 *            specifies the budget priority level.
	 * @return time left for budgeted loading.
	 */
	private long estimatedBugdetTimeLeft( final CacheHints hints )
	{
		final int priority = hints.getQueuePriority();
		final IoStatistics stats = CacheIoTiming.getIoStatistics();
		final IoTimeBudget budget = stats.getIoTimeBudget();
		return budget.estimateTimeLeft( priority );
	}
}
