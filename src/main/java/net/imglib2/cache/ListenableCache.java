package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.util.ListenableCacheAsCacheAdapter;

public interface ListenableCache< K, V > extends AbstractCache< K, V >
{
	V get( K key, CacheLoader< ? super K, ? extends V > loader, RemovalListener< ? super K, ? super V > remover ) throws ExecutionException;

	// TODO: add static Caches methods to Cache as interfaces? like this:
	public default Cache< K, V > withRemovalListener( final RemovalListener< K, V > removalListener )
	{
		return new ListenableCacheAsCacheAdapter<>( this, removalListener );
	}
}
