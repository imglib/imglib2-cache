package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.util.CacheAsLoadingCacheAdapter;

public interface Cache< K, V > extends AbstractCache< K, V >
{
	V get( K key, CacheLoader< ? super K, ? extends V > loader ) throws ExecutionException;

	// TODO: add static Caches methods to Cache as interfaces? like this:
	public default LoadingCache< K, V > withLoader( final CacheLoader< K, V > loader )
	{
		return new CacheAsLoadingCacheAdapter<>( this, loader );
	}
}
