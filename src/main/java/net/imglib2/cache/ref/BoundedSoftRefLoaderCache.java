package net.imglib2.cache.ref;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderCache;

/**
 * A cache that forwards to some other cache (usually
 * {@link WeakRefLoaderCache}) and additionally keeps {@link SoftReference}s to
 * the <em>N</em> most recently accessed values.
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 *
 * @author Tobias Pietzsch
 */
public class BoundedSoftRefLoaderCache< K, V > implements LoaderCache< K, V >
{
	private final LoaderCache< K, V > cache;

	private final BoundedSoftRefLoaderCache< K, V >.SoftRefs softRefs;

	public BoundedSoftRefLoaderCache( final int maxSoftRefs, final LoaderCache< K, V > cache )
	{
		this.cache = cache;
		this.softRefs = new SoftRefs( maxSoftRefs );
	}

	public BoundedSoftRefLoaderCache( final int maxSoftRefs )
	{
		this.cache = new WeakRefLoaderCache<>();
		this.softRefs = new SoftRefs( maxSoftRefs );
	}

	@Override
	public V getIfPresent( final K key )
	{
		final V value = cache.getIfPresent( key );
		if ( value != null )
			softRefs.touch( key, value );
		return value;
	}

	@Override
	public V get( final K key, final CacheLoader< ? super K, ? extends V > loader ) throws ExecutionException
	{
		final V value = cache.get( key, loader );
		softRefs.touch( key, value );
		return value;
	}

	@Override
	public void invalidate( final K key )
	{
		cache.invalidate( key );
		softRefs.remove( key );
	}

	@Override
	public void invalidateIf( final long parallelismThreshold, final Predicate< K > condition )
	{
		cache.invalidateIf( parallelismThreshold, condition );
		softRefs.keySet().removeIf( condition );
	}

	@Override
	public void invalidateAll( final long parallelismThreshold )
	{
		softRefs.clear();
		cache.invalidateAll( parallelismThreshold );
	}

	class SoftRefs extends LinkedHashMap< K, SoftReference< V > >
	{
		private static final long serialVersionUID = 1L;

		private final int maxSoftRefs;

		public SoftRefs( final int maxSoftRefs )
		{
			super( maxSoftRefs, 0.75f, true );
			this.maxSoftRefs = maxSoftRefs;
		}

		@Override
		protected boolean removeEldestEntry( final Entry< K, SoftReference< V > > eldest )
		{
			if ( size() > maxSoftRefs )
			{
				eldest.getValue().clear();
				return true;
			}
			else
				return false;
		}

		synchronized void touch( final K key, final V value )
		{
			final SoftReference< V > ref = get( key );
			if ( ref == null || ref.get() == null )
				put( key, new SoftReference<>( value ) );
		}

		@Override
		public synchronized void clear()
		{
			for ( final SoftReference< V > ref : values() )
				ref.clear();
			super.clear();
		}
	}
}
