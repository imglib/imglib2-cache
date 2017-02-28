package net.imglib2.cache.volatiles;

public interface UncheckedVolatileLoadingCache< K, V > extends AbstractUncheckedVolatileCache< K, V >
{
	V get( K key, CacheHints cacheHints );
}
