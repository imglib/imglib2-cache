package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.ListenableCache;
import net.imglib2.cache.RemovalListener;

public class ListenableCacheAsCacheAdapter< K, V > implements Cache< K, V >
{
	private final ListenableCache< K, V > cache;

	private final RemovalListener< K, V > remover;

	public ListenableCacheAsCacheAdapter( final ListenableCache< K, V > cache, final RemovalListener< K, V > remover )
	{
		this.cache = cache;
		this.remover = remover;
	}

	@Override
	public V getIfPresent( final K key )
	{
		return cache.getIfPresent( key );
	}

	@Override
	public void invalidateAll()
	{
		cache.invalidateAll();
	}

	@Override
	public V get( final K key, final CacheLoader< ? super K, ? extends V > loader ) throws ExecutionException
	{
		return cache.get( key, loader, remover );
	}
}
