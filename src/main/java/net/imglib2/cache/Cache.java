package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

public interface Cache< K, V > extends AbstractCache< K, V >
{
	V get( K key, CacheLoader< ? super K, ? extends V > loader ) throws ExecutionException;
}
