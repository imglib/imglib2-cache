/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2020 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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
package net.imglib2.cache.ref;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderCache;

/**
 * A cache that forwards to some other cache (usually
 * {@link WeakRefLoaderCache}) and additionally keeps {@link SoftReference}s to
 * the <em>N</em> most recently accessed values.
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 *
 * @author Tobias Pietzsch
 */
public class BoundedSoftRefLoaderCache< K, V > implements LoaderCache< K, V >
{
	private final LoaderCache< K, V > cache;

	private final BoundedSoftRefLoaderCache< K, V >.SoftRefs softRefs;

	public BoundedSoftRefLoaderCache( final int maxSoftRefs, final LoaderCache< K, V > cache )
	{
		this.cache = cache;
		this.softRefs = new SoftRefs( maxSoftRefs );
	}

	public BoundedSoftRefLoaderCache( final int maxSoftRefs )
	{
		this.cache = new WeakRefLoaderCache<>();
		this.softRefs = new SoftRefs( maxSoftRefs );
	}

	@Override
	public V getIfPresent( final K key )
	{
		final V value = cache.getIfPresent( key );
		if ( value != null )
			softRefs.touch( key, value );
		return value;
	}

	@Override
	public V get( final K key, final CacheLoader< ? super K, ? extends V > loader ) throws ExecutionException
	{
		final V value = cache.get( key, loader );
		softRefs.touch( key, value );
		return value;
	}

	@Override
	public void persist( final K key )
	{}

	@Override
	public void persistIf( final Predicate< K > condition )
	{}

	@Override
	public void persistAll()
	{}

	@Override
	public void invalidate( final K key )
	{
		cache.invalidate( key );
		softRefs.remove( key );
	}

	@Override
	public void invalidateIf( final long parallelismThreshold, final Predicate< K > condition )
	{
		cache.invalidateIf( parallelismThreshold, condition );
		softRefs.keySet().removeIf( condition );
	}

	@Override
	public void invalidateAll( final long parallelismThreshold )
	{
		softRefs.clear();
		cache.invalidateAll( parallelismThreshold );
	}

	class SoftRefs extends LinkedHashMap< K, SoftReference< V > >
	{
		private static final long serialVersionUID = 1L;

		private final int maxSoftRefs;

		public SoftRefs( final int maxSoftRefs )
		{
			super( maxSoftRefs, 0.75f, true );
			this.maxSoftRefs = maxSoftRefs;
		}

		@Override
		protected boolean removeEldestEntry( final Entry< K, SoftReference< V > > eldest )
		{
			if ( size() > maxSoftRefs )
			{
				eldest.getValue().clear();
				return true;
			}
			else
				return false;
		}

		synchronized void touch( final K key, final V value )
		{
			final SoftReference< V > ref = get( key );
			if ( ref == null || ref.get() == null )
				put( key, new SoftReference<>( value ) );
		}

		@Override
		public synchronized void clear()
		{
			for ( final SoftReference< V > ref : values() )
				ref.clear();
			super.clear();
		}
	}
}
