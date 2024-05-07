/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2024 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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

import java.util.function.Predicate;

import net.imglib2.cache.AbstractCache;

/**
 * Wraps a {@code Cache<L,V>} as a {@code Cache<K,V>}, using a
 * {@code KeyBimap<K,L>} to translate keys.
 *
 * @param <K>
 *            key type of this cache
 * @param <L>
 *            key type of wrapped cache
 * @param <V>
 *            value type (of both of this cache and the wrapped cache)
 * @param <C>
 *            wrapped cache type
 *
 * @author Tobias Pietzsch
 */
public class AbstractCacheKeyAdapter< K, L, V, C extends AbstractCache< L, V > >
		implements AbstractCache< K, V >
{
	protected final C cache;

	protected final KeyBimap< K, L > keymap;

	public AbstractCacheKeyAdapter( final C cache, final KeyBimap< K, L > keymap )
	{
		this.cache = cache;
		this.keymap = keymap;
	}

	@Override
	public V getIfPresent( final K key )
	{
		return cache.getIfPresent( keymap.getTarget( key ) );
	}

	@Override
	public void persist( final K key )
	{
		cache.persist( keymap.getTarget( key ) );
	}

	@Override
	public void persistIf( final Predicate< K > condition )
	{
		cache.persistIf( l -> {
			final K k = keymap.getSource( l );
			return k != null && condition.test( k );
		} );
	}

	@Override
	public void persistAll()
	{
		cache.persistIf( l -> keymap.getSource( l ) != null );
	}

	@Override
	public void invalidate( final K key )
	{
		cache.invalidate( keymap.getTarget( key ) );
	}

	@Override
	public void invalidateIf( final long parallelismThreshold, final Predicate< K > condition )
	{
		cache.invalidateIf( parallelismThreshold, l -> {
			final K k = keymap.getSource( l );
			return k != null && condition.test( k );
		} );
	}

	@Override
	public void invalidateAll( final long parallelismThreshold )
	{
		cache.invalidateIf( parallelismThreshold, l -> keymap.getSource( l ) != null );
	}
}
