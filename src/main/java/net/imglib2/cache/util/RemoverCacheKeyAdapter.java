package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.RemoverCache;
import net.imglib2.cache.CacheRemover;

public class RemoverCacheKeyAdapter< K, L, V, C extends RemoverCache< L, V > >
		extends AbstractCacheKeyAdapter< K, L, V, C >
		implements RemoverCache< K, V >
{
	public RemoverCacheKeyAdapter( final C cache, final KeyBimap< K, L > keymap )
	{
		super( cache, keymap );
	}

	@Override
	public V get( final K key, final CacheRemover< ? super K, ? super V > remover ) throws ExecutionException
	{
		return cache.get(
				keymap.getTarget( key ),
				( k, v ) -> remover.onRemoval( keymap.getSource( k ), v ) );
	}
}
