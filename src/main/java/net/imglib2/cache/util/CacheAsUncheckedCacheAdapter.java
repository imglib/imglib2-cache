package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.imglib2.cache.Cache;
import net.imglib2.cache.UncheckedCache;

/**
 * Wraps a {@code Cache<K,V>} as an {@code UncheckedCache<K,V>}. This is done by
 * wrapping {@code ExecutionException} thrown by {@link Cache#get(Object)} as a
 * {@code RuntimeException}.
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 *
 * @author Tobias Pietzsch
 */
public class CacheAsUncheckedCacheAdapter< K, V > implements UncheckedCache< K, V >
{
	private final Cache< K, V > cache;

	public CacheAsUncheckedCacheAdapter( final Cache< K, V > cache )
	{
		this.cache = cache;
	}

	@Override
	public V getIfPresent( final K key )
	{
		return cache.getIfPresent( key );
	}

	@Override
	public V get( final K key )
	{
		try
		{
			return cache.get( key );
		}
		catch ( final ExecutionException e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public void invalidate( final K key )
	{
		cache.invalidate( key );
	}

	@Override
	public void invalidateIf( final Predicate< K > condition )
	{
		cache.invalidateIf( condition );
	}

	@Override
	public void invalidateAll()
	{
		cache.invalidateAll();
	}
}
