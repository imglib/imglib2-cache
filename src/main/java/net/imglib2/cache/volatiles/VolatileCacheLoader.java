package net.imglib2.cache.volatiles;

import net.imglib2.cache.CacheLoader;

public interface VolatileCacheLoader< K, V > extends CacheLoader< K, V >
{
	public V createInvalid( K key ) throws Exception;
}
