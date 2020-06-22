package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderCache;

/**
 * Wraps a {@code LoaderCache<K,V>} as a {@code Cache<K,V>}. This is done by
 * supplying a default {@code CacheLoader} (specified in the constructor) to
 * {@link LoaderCache#get(Object, CacheLoader)}.
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 *
 * @author Tobias Pietzsch
 */
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
	public void persist( final K key )
	{
		cache.persist( key );
	}

	@Override
	public void persistIf( final Predicate< K > condition )
	{
		cache.persistIf( condition );
	}

	@Override
	public void persistAll()
	{
		cache.persistAll();
	}

	@Override
	public void invalidate( final K key )
	{
		cache.invalidate( key );
	}

	@Override
	public void invalidateIf( final long parallelismThreshold, final Predicate< K > condition )
	{
		cache.invalidateIf( parallelismThreshold, condition );
	}

	@Override
	public void invalidateAll( final long parallelismThreshold )
	{
		cache.invalidateAll( parallelismThreshold );
	}
}
