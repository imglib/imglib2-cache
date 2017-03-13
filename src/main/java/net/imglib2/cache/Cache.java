package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.util.CacheAsUncheckedCacheAdapter;
import net.imglib2.cache.util.CacheKeyAdapter;
import net.imglib2.cache.util.KeyBimap;

public interface Cache< K, V > extends AbstractCache< K, V >, CacheLoader< K, V >
{
	@Override
	V get( K key ) throws ExecutionException;

	public default UncheckedCache< K, V > unchecked()
	{
		return new CacheAsUncheckedCacheAdapter<>( this );
	}

	public default < T > Cache< T, V > mapKeys( final KeyBimap< T, K > keymap )
	{
		return new CacheKeyAdapter<>( this, keymap );
	}
}
