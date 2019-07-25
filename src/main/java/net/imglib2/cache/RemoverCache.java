package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.util.KeyBimap;
import net.imglib2.cache.util.RemoverCacheAsCacheAdapter;
import net.imglib2.cache.util.RemoverCacheKeyAdapter;

public interface RemoverCache< K, V, D > extends AbstractCache< K, V >
{
	V get( K key, CacheRemover< ? super K, V, D > remover ) throws ExecutionException;

	default Cache< K, V > withRemover( final CacheRemover< K, V, D > remover )
	{
		return new RemoverCacheAsCacheAdapter<>( this, remover );
	}

	default < T > RemoverCache< T, V, D > mapKeys( final KeyBimap< T, K > keymap )
	{
		return new RemoverCacheKeyAdapter<>( this, keymap );
	}
}
