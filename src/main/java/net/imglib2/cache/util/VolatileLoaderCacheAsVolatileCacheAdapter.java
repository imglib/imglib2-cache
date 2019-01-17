package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import java.util.function.Predicate;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.VolatileLoaderCache;
import net.imglib2.cache.volatiles.VolatileCacheLoader;
import net.imglib2.cache.volatiles.VolatileCache;

public class VolatileLoaderCacheAsVolatileCacheAdapter< K, V > implements VolatileCache< K, V >
{
	private final VolatileLoaderCache< K, V > cache;

	private final VolatileCacheLoader< K, V > loader;

	public VolatileLoaderCacheAsVolatileCacheAdapter( final VolatileLoaderCache< K, V > cache, final VolatileCacheLoader< K, V > loader )
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
