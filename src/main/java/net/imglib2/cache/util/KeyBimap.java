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

import java.util.function.Function;

/**
 * A bidirectional map between key types {@code S} and {@code T} (source and
 * target). This is used to create adapters exposing a {@code T→V} cache as a
 * {@code S→V} cache.
 * <p>
 * The {@code S→T} mapping must be injective. If the {@code S→T} mapping is not
 * bijective, {@link #getSource(Object)} should return {@code null} for target
 * keys with no corresponding source key.
 *
 * @param <S>
 *            source type
 * @param <T>
 *            target type
 *
 * @author Tobias Pietzsch
 */
public interface KeyBimap< S, T >
{
	/**
	 * Returns target key for a given source key.
	 *
	 * @param source
	 *            source key
	 * @return the target key corresponding to {@code source}
	 */
	T getTarget( S source );

	/**
	 * Returns source key source key mapping to a given target key, or
	 * {@code null} if there is no such source key.
	 *
	 * @param target
	 *            target key
	 * @return the source key mapping to {@code target}, or {@code null} if
	 *         there is no such source key.
	 */
	S getSource( T target );

	public static < S, T > KeyBimap< S, T > build( final Function< S, T > soureToTarget, final Function< T, S > targetToSource )
	{
		return new KeyBimap< S, T >()
		{
			@Override
			public T getTarget( final S source )
			{
				return soureToTarget.apply( source );
			}

			@Override
			public S getSource( final T target )
			{
				return targetToSource.apply( target );
			}
		};
	}
}
