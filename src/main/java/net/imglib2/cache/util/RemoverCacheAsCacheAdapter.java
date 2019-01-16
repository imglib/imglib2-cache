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
public class RemoverCacheAsCacheAdapter< K, V > implements Cache< K, V >
{
	private final RemoverCache< K, V > cache;

	private final CacheRemover< K, V > remover;

	public RemoverCacheAsCacheAdapter( final RemoverCache< K, V > cache, final CacheRemover< K, V > remover )
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
