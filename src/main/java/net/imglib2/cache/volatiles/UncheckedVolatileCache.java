package net.imglib2.cache.volatiles;

public interface UncheckedVolatileCache< K, V > extends AbstractVolatileCache< K, V >
{
	V get( K key, VolatileCacheLoader< ? super K, ? extends V > loader, CacheHints cacheHints );
}
