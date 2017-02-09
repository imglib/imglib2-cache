package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

public interface ListenableLoadingCache< K, V > extends AbstractCache< K, V >
{
	V get( K key, RemovalListener< ? super K, ? super V > remover ) throws ExecutionException;
}
