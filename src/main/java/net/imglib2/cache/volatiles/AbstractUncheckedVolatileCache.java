package net.imglib2.cache.volatiles;

public interface AbstractUncheckedVolatileCache< K, V >
{
	V getIfPresent(
			K key,
			CacheHints cacheHints );

	void invalidateAll();
//	void cleanUp();
//	void invalidate( Object key );
//	long size();
}
