package net.imglib2.cache.ref;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderCache;

/**
 * A {@link LoaderCache} that is backed by a cache with strong references to
 * values. At the moment the backing cache is we use is
 * <a href="https://github.com/ben-manes/caffeine">caffeine</a>. We can easily
 * add Guava, cache2k, etc options later.
 * <p>
 * In addition we keep weak references to values. This ensures that
 * we never lose track of values that are still in use elsewhere, although they
 * have been evicted from the backing cache. Thus we never end up in a situation
 * where two distinct values are associated to the same key (associated
 * conceptually by the application, not by the cache map).
 * </p>
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 *
 * @author Tobias Pietzsch
 */
public class GuardedStrongRefLoaderCache< K, V > implements LoaderCache< K, V >
{
	final ConcurrentHashMap< K, Entry > map = new ConcurrentHashMap<>();

	final ReferenceQueue< V > queue = new ReferenceQueue<>();

	final Cache< K, V > strongCache;

	final class CacheWeakReference extends WeakReference< V >
	{
		private final Entry entry;

		public CacheWeakReference()
		{
			super( null );
			this.entry = null;
		}

		public CacheWeakReference( final V referent, final Entry entry )
		{
			super( referent, queue );
			this.entry = entry;
		}

		public void clean()
		{
			map.remove( entry.key, entry );
		}
	}

	final class Entry
	{
		final K key;

		private WeakReference< V > ref;

		boolean loaded;

		public Entry( final K key )
		{
			this.key = key;
			this.ref = new CacheWeakReference();
			this.loaded = false;
		}

		public V getValue()
		{
			return ref.get();
		}

		public void setValue( final V value )
		{
			this.loaded = true;
			this.ref = new CacheWeakReference( value, this );
		}
	}

	public GuardedStrongRefLoaderCache( final long maximumSize )
	{
		strongCache = Caffeine.newBuilder().maximumSize( maximumSize ).build();
	}

	@Override
	public V getIfPresent( final K key )
	{
		cleanUp();
		final V value = strongCache.getIfPresent( key );
		if ( value != null )
			return value;
		final Entry entry = map.get( key );
		return entry == null ? null : entry.getValue();
	}

	@Override
	public V get( final K key, final CacheLoader< ? super K, ? extends V > loader ) throws ExecutionException
	{
		cleanUp();
		V value = strongCache.getIfPresent( key );
		if ( value != null )
			return value;
		final Entry entry = map.computeIfAbsent( key, ( k ) -> new Entry( k ) );
		value = entry.getValue();
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
						map.remove( key, entry );
						value = get( key, loader );
					}
				}
				else
				{
					try
					{
						value = loader.get( key );
						entry.setValue( value );
						strongCache.put( key, value );
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
	public void invalidate( final K key )
	{
		// TODO
		throw new UnsupportedOperationException( "not implemented yet" );
	}

	@Override
	public void invalidateIf( final Predicate< K > condition )
	{
		// TODO
		throw new UnsupportedOperationException( "not implemented yet" );
	}

	@Override
	public void invalidateAll()
	{
		strongCache.invalidateAll();
		map.clear();
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
}
