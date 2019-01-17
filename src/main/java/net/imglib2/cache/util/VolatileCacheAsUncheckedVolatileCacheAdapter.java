package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;

import java.util.function.Predicate;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.UncheckedVolatileCache;
import net.imglib2.cache.volatiles.VolatileCache;

public class VolatileCacheAsUncheckedVolatileCacheAdapter< K, V > implements UncheckedVolatileCache< K, V >
{
	private final VolatileCache< K, V > cache;

	public VolatileCacheAsUncheckedVolatileCacheAdapter( final VolatileCache< K, V > cache )
	{
		this.cache = cache;
	}

	@Override
	public V getIfPresent( final K key, final CacheHints cacheHints )
	{
		try
		{
			return cache.getIfPresent( key, cacheHints );
		}
		catch ( final ExecutionException e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public V get( final K key, final CacheHints cacheHints )
	{
		try
		{
			return cache.get( key, cacheHints );
		}
		catch ( final ExecutionException e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public void invalidate( final K key )
	{
		cache.invalidate( key );
	}

	@Override
	public void invalidateIf( final Predicate< K > condition )
	{
		cache.invalidateIf( condition );
	}

	@Override
	public void invalidateAll()
	{
		cache.invalidateAll();
	}
}
