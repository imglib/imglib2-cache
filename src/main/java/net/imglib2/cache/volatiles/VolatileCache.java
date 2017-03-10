package net.imglib2.cache.volatiles;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.util.VolatileCacheAsVolatileLoadingCacheAdapter;

public interface VolatileCache< K, V > extends AbstractVolatileCache< K, V >
{
	V get( K key, VolatileCacheLoader< ? super K, ? extends V > loader, CacheHints cacheHints ) throws ExecutionException;

	public default VolatileLoadingCache< K, V > withLoader( final VolatileCacheLoader< K, V > loader )
	{
		return new VolatileCacheAsVolatileLoadingCacheAdapter<>( this, loader );
	}
}
