package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

public interface LoadingCache< K, V > extends AbstractCache< K, V >, CacheLoader< K, V >
{
	@Override
	V get( K key ) throws ExecutionException;
}
