package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.UncheckedVolatileLoadingCache;
import net.imglib2.cache.volatiles.VolatileLoadingCache;

public class VolatileLoadingCacheAsUncheckedVolatileLoadingCacheAdapter< K, V > implements UncheckedVolatileLoadingCache< K, V >
{
	private final VolatileLoadingCache< K, V > cache;

	public VolatileLoadingCacheAsUncheckedVolatileLoadingCacheAdapter( final VolatileLoadingCache< K, V > cache )
	{
		this.cache = cache;
	}

	@Override
	public V getIfPresent( final K key, final CacheHints cacheHints )
	{
		try
		{
			return cache.getIfPresent( key, cacheHints );
		}
		catch ( final ExecutionException e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public V get( final K key, final CacheHints cacheHints )
	{
		try
		{
			return cache.get( key, cacheHints );
		}
		catch ( final ExecutionException e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public void invalidateAll()
	{
		cache.invalidateAll();
	}
}
