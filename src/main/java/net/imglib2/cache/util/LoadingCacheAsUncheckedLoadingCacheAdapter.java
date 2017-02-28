package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.LoadingCache;
import net.imglib2.cache.UncheckedLoadingCache;

public class LoadingCacheAsUncheckedLoadingCacheAdapter< K, V > implements UncheckedLoadingCache< K, V >
{
	private final LoadingCache< K, V > cache;

	public LoadingCacheAsUncheckedLoadingCacheAdapter( final LoadingCache< K, V > cache )
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
	public void invalidateAll()
	{
		cache.invalidateAll();
	}
}
