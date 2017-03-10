package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderRemoverCache;
import net.imglib2.cache.CacheRemover;

public class LoaderRemoverCacheKeyAdapter< K, L, V, C extends LoaderRemoverCache< L, V > >
		extends AbstractCacheKeyAdapter< K, L, V, C >
		implements LoaderRemoverCache< K, V >
{
	public LoaderRemoverCacheKeyAdapter( final C cache, final KeyBimap< K, L > keymap )
	{
		super( cache, keymap );
	}

	@Override
	public V get( final K key, final CacheLoader< ? super K, ? extends V > loader, final CacheRemover< ? super K, ? super V > remover ) throws ExecutionException
	{
		return cache.get(
				keymap.getTarget( key ),
				k -> loader.get( keymap.getSource( k ) ),
				( k, v ) -> remover.onRemoval( keymap.getSource( k ), v ) );
	}
}
