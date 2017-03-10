package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.util.LoadingCacheAsUncheckedLoadingCacheAdapter;

public interface Cache< K, V > extends AbstractCache< K, V >, CacheLoader< K, V >
{
	@Override
	V get( K key ) throws ExecutionException;

	public default UncheckedCache< K, V > unchecked()
	{
		return new LoadingCacheAsUncheckedLoadingCacheAdapter<>( this );
	}
}
