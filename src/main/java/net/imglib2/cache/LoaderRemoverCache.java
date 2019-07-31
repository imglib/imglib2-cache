package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.util.KeyBimap;
import net.imglib2.cache.util.LoaderRemoverCacheAsLoaderCacheAdapter;
import net.imglib2.cache.util.LoaderRemoverCacheAsRemoverCacheAdapter;
import net.imglib2.cache.util.LoaderRemoverCacheKeyAdapter;

public interface LoaderRemoverCache< K, V, D > extends AbstractCache< K, V >
{
	V get( K key, CacheLoader< ? super K, ? extends V > loader, CacheRemover< ? super K, V, D > remover ) throws ExecutionException;

	default LoaderCache< K, V > withRemover( final CacheRemover< K, V, D > remover )
	{
		return new LoaderRemoverCacheAsLoaderCacheAdapter<>( this, remover );
	}

	default RemoverCache< K, V, D > withLoader( final CacheLoader< K, V > loader )
	{
		return new LoaderRemoverCacheAsRemoverCacheAdapter<>( this, loader );
	}

	default < T > LoaderRemoverCache< T, V, D > mapKeys( final KeyBimap< T, K > keymap )
	{
		return new LoaderRemoverCacheKeyAdapter<>( this, keymap );
	}
}
