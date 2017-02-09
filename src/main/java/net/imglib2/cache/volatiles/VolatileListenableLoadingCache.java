package net.imglib2.cache.volatiles;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.RemovalListener;

public interface VolatileListenableLoadingCache< K, V > extends AbstractVolatileCache< K, V >
{
	V get( K key, RemovalListener< ? super K, ? super V > remover, CacheHints cacheHints ) throws ExecutionException;
}
