package net.imglib2.cache.util;

import net.imglib2.cache.LoaderCache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderRemoverCache;
import net.imglib2.cache.RemoverCache;
import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.UncheckedCache;
import net.imglib2.cache.volatiles.UncheckedVolatileCache;
import net.imglib2.cache.volatiles.VolatileLoaderCache;
import net.imglib2.cache.volatiles.VolatileCacheLoader;
import net.imglib2.cache.volatiles.VolatileCache;

public class Caches
{
	public static < K, L, V > LoaderCache< K, V >
			mapKeys( final LoaderCache< L, V > cache, final KeyBimap< K, L > keymap )
	{
		return new LoaderCacheKeyAdapter<>( cache, keymap );
	}

	public static < K, L, V > LoaderRemoverCache< K, V >
			mapKeys( final LoaderRemoverCache< L, V > cache, final KeyBimap< K, L > keymap )
	{
		return new LoaderRemoverCacheKeyAdapter<>( cache, keymap );
	}

	public static < K, L, V > Cache< K, V >
			mapKeys( final Cache< L, V > cache, final KeyBimap< K, L > keymap )
	{
		return new CacheKeyAdapter<>( cache, keymap );
	}

	public static < K, L, V > RemoverCache< K, V >
			mapKeys( final RemoverCache< L, V > cache, final KeyBimap< K, L > keymap )
	{
		return new RemoverCacheKeyAdapter<>( cache, keymap );
	}

	public static < K, V > VolatileCache< K, V >
			withLoader( final VolatileLoaderCache< K, V > cache, final VolatileCacheLoader< K, V > loader )
	{
		return new VolatileLoaderCacheAsVolatileCacheAdapter<>( cache, loader );
	}

	public static < K, V > Cache< K, V >
			withLoader( final LoaderCache< K, V > cache, final CacheLoader< K, V > loader )
	{
		return new LoaderCacheAsCacheAdapter<>( cache, loader );
	}

	public static < K, V > LoaderCache< K, V >
			withRemover( final LoaderRemoverCache< K, V > cache, final CacheRemover< K, V > removalListener )
	{
		return new LoaderRemoverCacheAsLoaderCacheAdapter<>( cache, removalListener );
	}

	public static < K, V > UncheckedCache< K, V >
			unchecked( final Cache< K, V > cache )
	{
		return new CacheAsUncheckedCacheAdapter<>( cache );
	}

	public static < K, V > UncheckedVolatileCache< K, V >
			unchecked( final VolatileCache< K, V > cache )
	{
		return new VolatileCacheAsUncheckedVolatileCacheAdapter<>( cache );
	}
}
