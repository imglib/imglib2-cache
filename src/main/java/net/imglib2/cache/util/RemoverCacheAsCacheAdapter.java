package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.RemoverCache;

/**
 * Wraps a {@code RemoverCache<K,V>} as a {@code Cache<K,V>}. This is done by
 * supplying a default {@code CacheRemover} (specified in the constructor) to
 * {@link RemoverCache#get(Object, CacheRemover)}.
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 *
 * @author Tobias Pietzsch
 */
public class RemoverCacheAsCacheAdapter< K, V, D > implements Cache< K, V >
{
	private final RemoverCache< K, V, D > cache;

	private final CacheRemover< K, V, D > remover;

	public RemoverCacheAsCacheAdapter( final RemoverCache< K, V, D > cache, final CacheRemover< K, V, D > remover )
	{
		this.cache = cache;
		this.remover = remover;
	}

	@Override
	public V getIfPresent( final K key )
	{
		return cache.getIfPresent( key );
	}

	@Override
	public V get( final K key ) throws ExecutionException
	{
		return cache.get( key, remover );
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
		remover.invalidate( key );
	}

	@Override
	public void invalidateIf( final long parallelismThreshold, final Predicate< K > condition )
	{
		cache.invalidateIf( parallelismThreshold, condition );
		remover.invalidateIf( parallelismThreshold, condition );
	}

	@Override
	public void invalidateAll( final long parallelismThreshold )
	{
		cache.invalidateAll( parallelismThreshold );
		remover.invalidateAll( parallelismThreshold );
	}
}
