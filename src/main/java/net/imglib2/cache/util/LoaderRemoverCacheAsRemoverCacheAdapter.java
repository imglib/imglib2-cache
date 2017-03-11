package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.LoaderRemoverCache;
import net.imglib2.cache.RemoverCache;

public class LoaderRemoverCacheAsRemoverCacheAdapter< K, V > implements RemoverCache< K, V >
{
	private final LoaderRemoverCache< K, V > cache;

	private final CacheLoader< K, V > loader;

	public LoaderRemoverCacheAsRemoverCacheAdapter( final LoaderRemoverCache< K, V > cache, final CacheLoader< K, V > loader )
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
	public void invalidateAll()
	{
		cache.invalidateAll();
	}

	@Override
	public V get( final K key, final CacheRemover< ? super K, ? super V > remover ) throws ExecutionException
	{
		return cache.get( key, loader, remover );
	}
}
