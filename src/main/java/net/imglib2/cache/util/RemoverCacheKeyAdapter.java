package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.RemoverCache;
import net.imglib2.cache.CacheRemover;

public class RemoverCacheKeyAdapter< K, L, V, D, C extends RemoverCache< L, V, D > >
		extends AbstractCacheKeyAdapter< K, L, V, C >
		implements RemoverCache< K, V, D >
{
	public RemoverCacheKeyAdapter( final C cache, final KeyBimap< K, L > keymap )
	{
		super( cache, keymap );
	}

	@Override
	public V get( final K key, final CacheRemover< ? super K, V, D > remover ) throws ExecutionException
	{
		final CacheRemover< L, V, D > r = new CacheRemover< L, V, D >()
		{
			@Override
			public void onRemoval( final L key, final D valueData )
			{
				remover.onRemoval( keymap.getSource( key ), valueData );
			}

			@Override
			public D extract( final V value )
			{
				return remover.extract( value );
			}

			@Override
			public V reconstruct( final L key, final D valueData )
			{
				return remover.reconstruct( keymap.getSource( key ), valueData );
			}
		};

		return cache.get( keymap.getTarget( key ), r );
	}
}
