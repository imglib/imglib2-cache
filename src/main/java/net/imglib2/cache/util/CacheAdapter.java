package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;

public class CacheAdapter< K, L, V, C extends Cache< L, V > >
		extends AbstractCacheAdapter< K, L, V, C >
		implements Cache< K, V >
{
	public CacheAdapter( final C cache, final KeyBimap< K, L > keymap )
	{
		super( cache, keymap );
	}

	@Override
	public V get( final K key, final CacheLoader< ? super K, ? extends V > loader ) throws ExecutionException
	{
		return cache.get(
				keymap.getTarget( key ),
				k -> loader.get( keymap.getSource( k ) ) );
	}
}
