/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2024 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.imglib2.cache.ref;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.imglib2.cache.LoaderCache;
import net.imglib2.cache.iotiming.CacheIoTiming;
import net.imglib2.cache.iotiming.IoStatistics;
import net.imglib2.cache.iotiming.IoTimeBudget;
import net.imglib2.cache.queue.BlockingFetchQueues;
import net.imglib2.cache.queue.FetcherThreads;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.VolatileCacheLoader;
import net.imglib2.cache.volatiles.VolatileLoaderCache;

public class WeakRefVolatileLoaderCache< K, V > implements VolatileLoaderCache< K, V >
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

	static final class CacheWeakReference< V > extends WeakReference< V >
	{
		private final WeakRefVolatileLoaderCache< ?, V >.Entry entry;

		final int loaded;

		public CacheWeakReference( final V referent )
		{
			super( referent );
			entry = null;
			loaded = NOTLOADED;
		}

		public CacheWeakReference( final V referent, final ReferenceQueue< V > remove, final WeakRefVolatileLoaderCache< ?, V >.Entry entry, final int loaded )
		{
			super( referent, remove );
			this.entry = entry;
			this.loaded = loaded;
		}

		public void clean()
		{
			if ( entry.ref == this )
				entry.remove();
		}
	}

	final class Entry
	{
		final K key;

		CacheWeakReference< V > ref;

		long enqueueFrame;

		VolatileCacheLoader< ? super K, ? extends V > loader;

		public Entry( final K key, final VolatileCacheLoader< ? super K, ? extends V > loader )
		{
			this.key = key;
			this.loader = loader;
			this.ref = new CacheWeakReference<>( null );
			this.enqueueFrame = -1;
		}

		public void setInvalid( final V value )
		{
			ref = new CacheWeakReference<>( value, queue, this, INVALID );
		}

		// Precondition: caller must hold lock on this.
		public void setValid( final V value )
		{
			ref = new CacheWeakReference<>( value, queue, this, VALID );
			loader = null;
			enqueueFrame = Long.MAX_VALUE;
			notifyAll();
		}

		public void remove()
		{
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

	public WeakRefVolatileLoaderCache(
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

		final CacheWeakReference< V > ref = entry.ref;
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

		final CacheWeakReference< V > ref = entry.ref;
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
			final CacheWeakReference< V > poll = ( CacheWeakReference< V > ) queue.poll();
			if ( poll == null )
				break;
			poll.clean();
		}
	}

	@Override
	public void invalidate( final K key )
	{
		try
		{
			// stop fetcher threads to avoid concurrent load for key
			fetchQueue.pause();

			// remove entry from this cache
			final Entry entry = map.remove( key );
			if ( entry != null )
			{
				final CacheWeakReference< V > ref = entry.ref;
				if ( ref != null )
					ref.clear();
				entry.ref = null;
				entry.loader = null;
			}

			// remove entry from backingCache
			backingCache.invalidate( key );

			// resume fetcher threads
			fetchQueue.resume();
		}
		catch ( final InterruptedException e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public void invalidateIf( final long parallelismThreshold, final Predicate< K > condition )
	{
		try
		{
			// stop fetcher threads to avoid concurrent load for key
			fetchQueue.pause();

			// remove matching entries from this cache
			map.forEachValue( parallelismThreshold, entry ->
			{
				if ( condition.test( entry.key ) )
				{
					entry.remove();
					final CacheWeakReference< V > ref = entry.ref;
					if ( ref != null )
						ref.clear();
					entry.ref = null;
					entry.loader = null;
				}
			} );

			// remove matching entries from backingCache
			backingCache.invalidateIf( parallelismThreshold, condition );

			// resume fetcher threads
			fetchQueue.resume();
		}
		catch ( final InterruptedException e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public void invalidateAll( final long parallelismThreshold )
	{
		try
		{
			// stop fetcher threads to avoid concurrent load for key
			fetchQueue.pause();

			// remove all entries from this cache
			// TODO: We could also simply do map.clear(). Pros/Cons?
			map.forEachValue( parallelismThreshold, entry -> {
				entry.remove();
				final CacheWeakReference< V > ref = entry.ref;
				if ( ref != null )
					ref.clear();
				entry.ref = null;
				entry.loader = null;
			} );

			// remove all entries from backingCache
			backingCache.invalidateAll( parallelismThreshold );

			// resume fetcher threads
			fetchQueue.resume();
		}
		catch ( final InterruptedException e )
		{
			throw new RuntimeException( e );
		}

	}

	// ================ private methods =====================

	private V getDontLoad( final Entry entry ) throws ExecutionException
	{
		synchronized( entry )
		{
			final CacheWeakReference< V > ref = entry.ref;
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

			return v;
		}
	}

	private V getVolatile( final Entry entry, final CacheHints hints ) throws ExecutionException
	{
		synchronized( entry )
		{
			final CacheWeakReference< V > ref = entry.ref;
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
			CacheWeakReference< V > ref = entry.ref;
			V v = ref.get();
			if ( v == null && ref.loaded != NOTLOADED )
			{
//				printEntryCollected( "map.remove getBudgeted 1", entry );
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
			final CacheWeakReference< V > ref = entry.ref;
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
			final CacheWeakReference< V > ref = entry.ref;
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
