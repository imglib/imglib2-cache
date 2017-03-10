package net.imglib2.cache.volatiles;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.CacheRemover;

public interface VolatileListenableCache< K, V > extends AbstractVolatileCache< K, V >
{
	V get( K key, VolatileCacheLoader< ? super K, ? extends V > loader, CacheRemover< ? super K, ? super V > remover, CacheHints cacheHints ) throws ExecutionException;
}
