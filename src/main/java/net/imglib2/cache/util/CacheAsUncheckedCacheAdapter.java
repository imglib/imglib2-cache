package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.imglib2.cache.Cache;
import net.imglib2.cache.UncheckedCache;

public class CacheAsUncheckedCacheAdapter< K, V > implements UncheckedCache< K, V >
{
	private final Cache< K, V > cache;

	public CacheAsUncheckedCacheAdapter( final Cache< K, V > cache )
	{
		this.cache = cache;
	}

	@Override
	public V getIfPresent( final K key )
	{
		return cache.getIfPresent( key );
	}

	@Override
	public V get( final K key )
	{
		try
		{
			return cache.get( key );
		}
		catch ( final ExecutionException e )
		{
			throw new RuntimeException( e );
		}
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
