/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2023 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.RemoverCache;

public class RemoverCacheKeyAdapter< K, L, V, D, C extends RemoverCache< L, V, D > >
		extends AbstractCacheKeyAdapter< K, L, V, C >
		implements RemoverCache< K, V, D >
{
	public RemoverCacheKeyAdapter( final C cache, final KeyBimap< K, L > keymap )
	{
		super( cache, keymap );
	}

	@Override
	public V get( final K key, final CacheRemover< ? super K, V, D > remover ) throws ExecutionException
	{
		/*
		 * NB: The cache has no global CacheRemover, so invalidate() calls will
		 * not invoke CacheRemover.invalidate(). Therefore, the wrapped
		 * CacheRemover does not override invalidate(). If on a higher level,
		 * the CacheRemover is made cache-global, then invalidation is handled
		 * already there.
		 */
		final CacheRemover< L, V, D > r = new CacheRemover< L, V, D >()
		{
			@Override
			public void onRemoval( final L key, final D valueData )
			{
				remover.onRemoval( keymap.getSource( key ), valueData );
			}

			@Override
			public CompletableFuture< Void > persist( final L key, final D valueData )
			{
				return remover.persist( keymap.getSource( key ), valueData );
			}

			@Override
			public D extract( final V value )
			{
				return remover.extract( value );
			}

			@Override
			public V reconstruct( final L key, final D valueData )
			{
				return remover.reconstruct( keymap.getSource( key ), valueData );
			}
		};

		return cache.get( keymap.getTarget( key ), r );
	}
}
