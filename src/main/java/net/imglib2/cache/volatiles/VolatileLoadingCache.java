package net.imglib2.cache.volatiles;

import java.util.concurrent.ExecutionException;

public interface VolatileLoadingCache< K, V > extends AbstractVolatileCache< K, V >
{
	V get( K key, CacheHints cacheHints ) throws ExecutionException;
}
