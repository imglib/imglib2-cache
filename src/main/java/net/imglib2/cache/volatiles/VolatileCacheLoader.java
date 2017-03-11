package net.imglib2.cache.volatiles;

import net.imglib2.cache.CacheLoader;

public interface VolatileCacheLoader< K, V > extends CacheLoader< K, V >, CreateInvalid< K, V >
{}
