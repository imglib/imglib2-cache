package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.VolatileCache;
import net.imglib2.cache.volatiles.VolatileCacheLoader;
import net.imglib2.cache.volatiles.VolatileLoadingCache;

public class VolatileCacheAsVolatileLoadingCacheAdapter< K, V > implements VolatileLoadingCache< K, V >
{
	private final VolatileCache< K, V > cache;

	private final VolatileCacheLoader< K, V > loader;

	public VolatileCacheAsVolatileLoadingCacheAdapter( final VolatileCache< K, V > cache, final VolatileCacheLoader< K, V > loader )
	{
		this.cache = cache;
		this.loader = loader;
	}

	@Override
	public V getIfPresent( final K key, final CacheHints cacheHints ) throws ExecutionException
	{
		return cache.getIfPresent( key, cacheHints );
	}

	@Override
	public V get( final K key, final CacheHints cacheHints ) throws ExecutionException
	{
		return cache.get( key, loader, cacheHints );
	}

	@Override
	public void invalidateAll()
	{
		cache.invalidateAll();
	}
}
