package net.imglib2.cache.volatiles;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.CacheRemover;

/**
 * This is only here for consistency with the non-volatile cache interfaces.
 * There is no implementation at the moment.
 * <p>
 * TODO: REMOVE?
 */
public interface VolatileRemoverCache< K, V > extends AbstractVolatileCache< K, V >
{
	V get( K key, CacheRemover< ? super K, ? super V > remover, CacheHints cacheHints ) throws ExecutionException;
}
