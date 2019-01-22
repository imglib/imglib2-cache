package net.imglib2.cache.util;

import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.LoaderRemoverCache;
import net.imglib2.cache.RemoverCache;

/**
 * Wraps a {@code LoaderRemoverCache<K,V>} as a {@code RemoverCache<K,V>}. This
 * is done by supplying a default {@code CacheLoader} (specified in the
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
public class LoaderRemoverCacheAsRemoverCacheAdapter< K, V > implements RemoverCache< K, V >
{
	private final LoaderRemoverCache< K, V > cache;

	private final CacheLoader< K, V > loader;

	public LoaderRemoverCacheAsRemoverCacheAdapter( final LoaderRemoverCache< K, V > cache, final CacheLoader< K, V > loader )
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
	public V get( final K key, final CacheRemover< ? super K, ? super V > remover ) throws ExecutionException
	{
		return cache.get( key, loader, remover );
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
