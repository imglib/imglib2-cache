package net.imglib2.cache.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.LoaderRemoverCache;

public class LoaderRemoverCacheKeyAdapter< K, L, V, D, C extends LoaderRemoverCache< L, V, D > >
		extends AbstractCacheKeyAdapter< K, L, V, C >
		implements LoaderRemoverCache< K, V, D >
{
	public LoaderRemoverCacheKeyAdapter( final C cache, final KeyBimap< K, L > keymap )
	{
		super( cache, keymap );
	}

	@Override
	public V get( final K key, final CacheLoader< ? super K, ? extends V > loader, final CacheRemover< ? super K, V, D > remover ) throws ExecutionException
	{
		/*
		 * NB: The cache has no global CacheRemover, so invalidate() calls will
		 * not invoke CacheRemover.invalidate(). Therefore, the wrapped
		 * CacheRemover does not override invalidate(). If on a higher level,
		 * the CacheRemover is made cache-global, then invalidation is handled
		 * already there.
		 */
		class LoaderRemover implements CacheRemover< L, V, D >, CacheLoader< L, V >
		{
			@Override
			public void onRemoval( final L key, final D valueData )
			{
				remover.onRemoval( keymap.getSource( key ), valueData );
			}

			@Override
			public CompletableFuture< Void > persist( final L key, final D valueData )
			{
				return remover.persist( keymap.getSource( key ), valueData );
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

			@Override
			public V get( final L key ) throws Exception
			{
				return loader.get( keymap.getSource( key ) );
			}
		};
		final LoaderRemover lr = new LoaderRemover();

		return cache.get( keymap.getTarget( key ), lr, lr );
	}
}
