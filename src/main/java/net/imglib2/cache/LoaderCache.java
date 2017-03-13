package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.util.KeyBimap;
import net.imglib2.cache.util.LoaderCacheAsCacheAdapter;
import net.imglib2.cache.util.LoaderCacheKeyAdapter;

public interface LoaderCache< K, V > extends AbstractCache< K, V >
{
	V get( K key, CacheLoader< ? super K, ? extends V > loader ) throws ExecutionException;

	// TODO: add static Caches methods to Cache as interfaces? like this:
	public default Cache< K, V > withLoader( final CacheLoader< K, V > loader )
	{
		return new LoaderCacheAsCacheAdapter<>( this, loader );
	}

	public default < T > LoaderCache< T, V > mapKeys( final KeyBimap< T, K > keymap )
	{
		return new LoaderCacheKeyAdapter<>( this, keymap );
	}
}
