package net.imglib2.cache.volatiles;

import java.util.concurrent.ExecutionException;

public interface VolatileCache< K, V > extends AbstractVolatileCache< K, V >
{
	V get( K key, VolatileCacheLoader< ? super K, ? extends V > loader, CacheHints cacheHints ) throws ExecutionException;
}
