package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderCache;

public class LoaderCacheAsCacheAdapter< K, V > implements Cache< K, V >
{
	private final LoaderCache< K, V > cache;

	private final CacheLoader< K, V > loader;

	public LoaderCacheAsCacheAdapter( final LoaderCache< K, V > cache, final CacheLoader< K, V > loader )
	{
		this.cache = cache;
		this.loader = loader;
	}

	@Override
	public V getIfPresent( final K key )
	{
		return cache.getIfPresent( key );
	}

	@Override
	public V get( final K key ) throws ExecutionException
	{
		return cache.get( key, loader );
	}

	@Override
	public void invalidate( final K key )
	{
		// TODO
		throw new UnsupportedOperationException( "not implemented yet" );
	}

	@Override
	public void invalidateIf( final Predicate< K > condition )
	{
		// TODO
		throw new UnsupportedOperationException( "not implemented yet" );
	}

	@Override
	public void invalidateAll()
	{
		cache.invalidateAll();
	}
}
