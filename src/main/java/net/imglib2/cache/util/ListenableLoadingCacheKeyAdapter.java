package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.ListenableLoadingCache;
import net.imglib2.cache.RemovalListener;

public class ListenableLoadingCacheKeyAdapter< K, L, V, C extends ListenableLoadingCache< L, V > >
		extends AbstractCacheKeyAdapter< K, L, V, C >
		implements ListenableLoadingCache< K, V >
{
	public ListenableLoadingCacheKeyAdapter( final C cache, final KeyBimap< K, L > keymap )
	{
		super( cache, keymap );
	}

	@Override
	public V get( final K key, final RemovalListener< ? super K, ? super V > remover ) throws ExecutionException
	{
		return cache.get(
				keymap.getTarget( key ),
				( k, v ) -> remover.onRemoval( keymap.getSource( k ), v ) );
	}
}
