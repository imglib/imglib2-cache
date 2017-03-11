package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.util.RemoverCacheAsCacheAdapter;

public interface RemoverCache< K, V > extends AbstractCache< K, V >
{
	V get( K key, CacheRemover< ? super K, ? super V > remover ) throws ExecutionException;

	public default Cache< K, V > withRemover( final CacheRemover< K, V > remover )
	{
		return new RemoverCacheAsCacheAdapter<>( this, remover );
	}
}
