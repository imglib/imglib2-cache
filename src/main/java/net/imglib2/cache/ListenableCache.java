package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

public interface ListenableCache< K, V > extends AbstractCache< K, V >
{
	V get( K key, CacheLoader< ? super K, ? extends V > loader, RemovalListener< ? super K, ? super V > remover ) throws ExecutionException;
}
