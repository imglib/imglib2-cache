package net.imglib2.cache.volatiles;

import java.util.concurrent.ExecutionException;

public interface AbstractVolatileCache< K, V >
{
	V getIfPresent(
			K key,
			CacheHints cacheHints ) throws ExecutionException;

	void invalidateAll();
}
