package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoadingCache;

public class CacheAsLoadingCacheAdapter< K, V > implements LoadingCache< K, V >
{
	private final Cache< K, V > cache;

	private final CacheLoader< K, V > loader;

	public CacheAsLoadingCacheAdapter( final Cache< K, V > cache, final CacheLoader< K, V > loader )
	{
		this.cache = cache;
		this.loader = loader;
	}

	@Override
	public V getIfPresent( final K key )
	{
		return cache.getIfPresent( key );
	}

	@Override
	public V get( final K key ) throws ExecutionException
	{
		return cache.get( key, loader );
	}

	@Override
	public void invalidateAll()
	{
		cache.invalidateAll();
	}
}
