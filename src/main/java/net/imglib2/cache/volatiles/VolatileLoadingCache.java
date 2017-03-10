package net.imglib2.cache.volatiles;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.util.VolatileLoadingCacheAsUncheckedVolatileLoadingCacheAdapter;

public interface VolatileLoadingCache< K, V > extends AbstractVolatileCache< K, V >
{
	V get( K key, CacheHints cacheHints ) throws ExecutionException;

	public default UncheckedVolatileLoadingCache< K, V > unchecked()
	{
		return new VolatileLoadingCacheAsUncheckedVolatileLoadingCacheAdapter<>( this );
	}
}
