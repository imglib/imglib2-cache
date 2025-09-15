/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2025 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
