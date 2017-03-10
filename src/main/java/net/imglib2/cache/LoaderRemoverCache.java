package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.util.LoaderRemoverCacheAsLoaderCacheAdapter;

public interface LoaderRemoverCache< K, V > extends AbstractCache< K, V >
{
	V get( K key, CacheLoader< ? super K, ? extends V > loader, CacheRemover< ? super K, ? super V > remover ) throws ExecutionException;

	// TODO: add static Caches methods to Cache as interfaces? like this:
	public default LoaderCache< K, V > withRemovalListener( final CacheRemover< K, V > removalListener )
	{
		return new LoaderRemoverCacheAsLoaderCacheAdapter<>( this, removalListener );
	}
}
