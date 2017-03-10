package net.imglib2.cache.ref;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import net.imglib2.cache.LoaderCache;
import net.imglib2.cache.CacheLoader;

/**
 * A cache that forwards to some other (usually {@link WeakRefCache}) cache and
 * additionally keeps {@link SoftReference}s to the <em>N</em> most recently
 * accessed values.
 *
 * @param <K>
 * @param <V>
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class BoundedSoftRefCache< K, V > implements LoaderCache< K, V >
{
	private final LoaderCache< K, V > cache;

	private final BoundedSoftRefCache< K, V >.SoftRefs softRefs;

	public BoundedSoftRefCache( final int maxSoftRefs, final LoaderCache< K, V > cache )
	{
		this.cache = cache;
		this.softRefs = new SoftRefs( maxSoftRefs );
	}

	public BoundedSoftRefCache( final int maxSoftRefs )
	{
		this.cache = new WeakRefCache<>();
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
	public void invalidateAll()
	{
		softRefs.clear();
		cache.invalidateAll();
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
