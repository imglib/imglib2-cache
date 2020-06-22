package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.LoaderCache;
import net.imglib2.cache.LoaderRemoverCache;

/**
 * Wraps a {@code LoaderRemoverCache<K,V>} as a {@code LoaderCache<K,V>}. This
 * is done by supplying a default {@code CacheRemover} (specified in the
 * constructor) to
 * {@link LoaderRemoverCache#get(Object, CacheLoader, CacheRemover)}.
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 *
 * @author Tobias Pietzsch
 */
public class LoaderRemoverCacheAsLoaderCacheAdapter< K, V, D > implements LoaderCache< K, V >
{
	private final LoaderRemoverCache< K, V, D > cache;

	private final CacheRemover< K, V, D > remover;

	public LoaderRemoverCacheAsLoaderCacheAdapter( final LoaderRemoverCache< K, V, D > cache, final CacheRemover< K, V, D > remover )
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
	public V get( final K key, final CacheLoader< ? super K, ? extends V > loader ) throws ExecutionException
	{
		return cache.get( key, loader, remover );
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
