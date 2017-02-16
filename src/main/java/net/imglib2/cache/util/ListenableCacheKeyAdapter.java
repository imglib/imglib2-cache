package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.ListenableCache;
import net.imglib2.cache.RemovalListener;

public class ListenableCacheKeyAdapter< K, L, V, C extends ListenableCache< L, V > >
		extends AbstractCacheKeyAdapter< K, L, V, C >
		implements ListenableCache< K, V >
{
	public ListenableCacheKeyAdapter( final C cache, final KeyBimap< K, L > keymap )
	{
		super( cache, keymap );
	}

	@Override
	public V get( final K key, final CacheLoader< ? super K, ? extends V > loader, final RemovalListener< ? super K, ? super V > remover ) throws ExecutionException
	{
		return cache.get(
				keymap.getTarget( key ),
				k -> loader.get( keymap.getSource( k ) ),
				( k, v ) -> remover.onRemoval( keymap.getSource( k ), v ) );
	}
}
