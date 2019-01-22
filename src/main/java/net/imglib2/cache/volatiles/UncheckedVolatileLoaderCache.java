package net.imglib2.cache.volatiles;

/**
 * This is only here for consistency with the non-volatile cache interfaces.
 * There is no implementation at the moment.
 * <p>
 * TODO: REMOVE?
 */
public interface UncheckedVolatileLoaderCache< K, V > extends AbstractUncheckedVolatileCache< K, V >
{
	V get( K key, VolatileCacheLoader< ? super K, ? extends V > loader, CacheHints cacheHints );
}
