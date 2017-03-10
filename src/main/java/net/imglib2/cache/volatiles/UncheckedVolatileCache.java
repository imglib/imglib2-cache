package net.imglib2.cache.volatiles;

public interface UncheckedVolatileCache< K, V > extends AbstractUncheckedVolatileCache< K, V >
{
	V get( K key, CacheHints cacheHints );
}
