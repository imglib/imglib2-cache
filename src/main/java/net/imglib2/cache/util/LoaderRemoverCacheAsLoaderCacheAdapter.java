package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.LoaderCache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderRemoverCache;
import net.imglib2.cache.CacheRemover;

public class LoaderRemoverCacheAsLoaderCacheAdapter< K, V > implements LoaderCache< K, V >
{
	private final LoaderRemoverCache< K, V > cache;

	private final CacheRemover< K, V > remover;

	public LoaderRemoverCacheAsLoaderCacheAdapter( final LoaderRemoverCache< K, V > cache, final CacheRemover< K, V > remover )
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
