package net.imglib2.cache.ref;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderCache;

public class WeakRefLoaderCache< K, V > implements LoaderCache< K, V >
{
	final ConcurrentHashMap< K, Entry > map = new ConcurrentHashMap<>();

	final ReferenceQueue< V > queue = new ReferenceQueue<>();

	static final class CacheWeakReference< V > extends WeakReference< V >
	{
		private final WeakRefLoaderCache< ?, V >.Entry entry;

		public CacheWeakReference()
		{
			super( null );
			this.entry = null;
		}

		public CacheWeakReference( final V referent, final ReferenceQueue< V > remove, final WeakRefLoaderCache< ?, V >.Entry entry )
		{
			super( referent, remove );
			this.entry = entry;
		}
	}

	final class Entry
	{
		final K key;

		private CacheWeakReference< V > ref;

		boolean loaded;

		public Entry( final K key )
		{
			this.key = key;
			this.ref = new CacheWeakReference<>();
			this.loaded = false;
		}

		public V getValue()
		{
			return ref.get();
		}

		public void setValue( final V value )
		{
			this.loaded = true;
			this.ref = new CacheWeakReference<>( value, queue, this );
		}

		public void remove()
		{
			map.remove( key, this );
		}
	}

	@Override
	public V getIfPresent( final K key )
	{
		cleanUp();
		final Entry entry = map.get( key );
		return entry == null ? null : entry.getValue();
	}

	@Override
	public V get( final K key, final CacheLoader< ? super K, ? extends V > loader ) throws ExecutionException
	{
		cleanUp();
		final Entry entry = map.computeIfAbsent( key, ( k ) -> new Entry( k ) );
		V value = entry.getValue();
		if ( value == null )
		{
			synchronized ( entry )
			{
				if ( entry.loaded )
				{
					value = entry.getValue();
					if ( value == null )
					{
						/*
						 * The entry was already loaded, but its value has been
						 * garbage collected. We need to create a new entry
						 */
						entry.remove();
						value = get( key, loader );
					}
				}
				else
				{
					try
					{
						value = loader.get( key );
						entry.setValue( value );
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
		}
		return value;
	}

	@Override
	public void persist( final K key )
	{}

	@Override
	public void persistIf( final Predicate< K > condition )
	{}

	@Override
	public void persistAll()
	{}

	@Override
	public void invalidate( final K key )
	{
		final Entry entry = map.remove( key );
		if ( entry != null )
		{
			final CacheWeakReference< V > ref = entry.ref;
			if ( ref != null )
				ref.clear();
			entry.ref = null;
		}
	}

	@Override
	public void invalidateIf( final long parallelismThreshold, final Predicate< K > condition )
	{
		map.forEachValue( parallelismThreshold, entry ->
		{
			if ( condition.test( entry.key ) )
			{
				entry.remove();
				final CacheWeakReference< V > ref = entry.ref;
				if ( ref != null )
					ref.clear();
				entry.ref = null;
			}
		} );
	}

	@Override
	public void invalidateAll( final long parallelismThreshold )
	{
		// TODO: We could also simply do map.clear(). Pros/Cons?

		map.forEachValue( parallelismThreshold, entry ->
		{
			entry.remove();
			final CacheWeakReference< V > ref = entry.ref;
			if ( ref != null )
				ref.clear();
			entry.ref = null;
		} );
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
			poll.entry.remove();
		}
	}
}
