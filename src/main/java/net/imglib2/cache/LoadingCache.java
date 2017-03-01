package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.util.LoadingCacheAsUncheckedLoadingCacheAdapter;

public interface LoadingCache< K, V > extends AbstractCache< K, V >, CacheLoader< K, V >
{
	@Override
	V get( K key ) throws ExecutionException;

	public default UncheckedLoadingCache< K, V > unchecked()
	{
		return new LoadingCacheAsUncheckedLoadingCacheAdapter<>( this );
	}
}
