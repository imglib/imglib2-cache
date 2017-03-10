package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.LoaderCache;
import net.imglib2.cache.CacheLoader;

public class LoaderCacheKeyAdapter< K, L, V, C extends LoaderCache< L, V > >
		extends AbstractCacheKeyAdapter< K, L, V, C >
		implements LoaderCache< K, V >
{
	public LoaderCacheKeyAdapter( final C cache, final KeyBimap< K, L > keymap )
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
