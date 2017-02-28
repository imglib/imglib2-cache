package net.imglib2.cache.util;

import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.ListenableCache;
import net.imglib2.cache.ListenableLoadingCache;
import net.imglib2.cache.LoadingCache;
import net.imglib2.cache.UncheckedLoadingCache;
import net.imglib2.cache.volatiles.UncheckedVolatileLoadingCache;
import net.imglib2.cache.volatiles.VolatileCache;
import net.imglib2.cache.volatiles.VolatileCacheLoader;
import net.imglib2.cache.volatiles.VolatileLoadingCache;

public class Caches
{
	public static < K, L, V > Cache< K, V >
			mapKeys( final Cache< L, V > cache, final KeyBimap< K, L > keymap )
	{
		return new CacheKeyAdapter<>( cache, keymap );
	}

	public static < K, L, V > ListenableCache< K, V >
			mapKeys( final ListenableCache< L, V > cache, final KeyBimap< K, L > keymap )
	{
		return new ListenableCacheKeyAdapter<>( cache, keymap );
	}

	public static < K, L, V > LoadingCache< K, V >
			mapKeys( final LoadingCache< L, V > cache, final KeyBimap< K, L > keymap )
	{
		return new LoadingCacheKeyAdapter<>( cache, keymap );
	}

	public static < K, L, V > ListenableLoadingCache< K, V >
			mapKeys( final ListenableLoadingCache< L, V > cache, final KeyBimap< K, L > keymap )
	{
		return new ListenableLoadingCacheKeyAdapter<>( cache, keymap );
	}

	public static < K, V > VolatileLoadingCache< K, V >
			withLoader( final VolatileCache< K, V > cache, final VolatileCacheLoader< K, V > loader )
	{
		return new VolatileCacheAsVolatileLoadingCacheAdapter<>( cache, loader );
	}

	public static < K, V > LoadingCache< K, V >
			withLoader( final Cache< K, V > cache, final CacheLoader< K, V > loader )
	{
		return new CacheAsLoadingCacheAdapter<>( cache, loader );
	}

	public static < K, V > UncheckedLoadingCache< K, V >
			unchecked( final LoadingCache< K, V > cache )
	{
		return new LoadingCacheAsUncheckedLoadingCacheAdapter<>( cache );
	}

	public static < K, V > UncheckedVolatileLoadingCache< K, V >
			unchecked( final VolatileLoadingCache< K, V > cache )
	{
		return new VolatileLoadingCacheAsUncheckedVolatileLoadingCacheAdapter<>( cache );
	}
}
