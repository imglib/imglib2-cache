package net.imglib2.cache.volatiles;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.CacheRemover;

/**
 * This is only here for consistency with the non-volatile cache interfaces.
 * There is no implementation at the moment.
 * <p>
 * TODO: REMOVE?
 */
public interface VolatileLoaderRemoverCache< K, V, D > extends AbstractVolatileCache< K, V >
{
	V get( K key, VolatileCacheLoader< ? super K, ? extends V > loader, CacheRemover< ? super K, V, D > remover, CacheHints cacheHints ) throws ExecutionException;
}
