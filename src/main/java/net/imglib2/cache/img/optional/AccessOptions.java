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
package net.imglib2.cache.img.optional;

import java.util.Set;
import java.util.function.BiConsumer;
import net.imglib2.Dirty;
import net.imglib2.img.basictypeaccess.AccessFlags;
import org.scijava.optional.Options;
import org.scijava.optional.Values;

/**
 * Optional arguments that specify whether accesses should be
 * <ul>
 *     <li>{@link #dirtyAccesses(boolean) dirty} (default {@code false})</li>
 *     <li>{@link #volatileAccesses(boolean) volatile} (default {@code true})</li>
 * </ul>
 */
public interface AccessOptions< T > extends Options< T >
{
	/**
	 * Specify whether the image should use {@link Dirty} accesses. Dirty
	 * accesses track whether cells were written to.
	 * <p>
	 * This is {@code false} by default.
	 * </p>
	 *
	 * @param dirty
	 * 		whether the image should use {@link Dirty} accesses.
	 */
	default T dirtyAccesses( final boolean dirty )
	{
		return setValue( "dirtyAccesses", dirty );
	}

	default T volatileAccesses( final boolean volatil )
	{
		return setValue( "volatileAccesses", volatil );
	}

	default T accessFlags( final Set< AccessFlags > flags )
	{
		final boolean dirty = flags.contains( AccessFlags.DIRTY );
		final boolean volatil = flags.contains( AccessFlags.VOLATILE );
		return ( ( AccessOptions< T > ) dirtyAccesses( dirty ) ).volatileAccesses( volatil );
	}

	interface Val extends Values
	{
		default void forEach( BiConsumer< String, Object > action )
		{
			action.accept( "dirtyAccesses", dirtyAccesses() );
			action.accept( "volatileAccesses", volatileAccesses() );
		}

		default boolean dirtyAccesses()
		{
			return getValueOrDefault( "dirtyAccesses", false );
		}

		default boolean volatileAccesses()
		{
			return getValueOrDefault( "volatileAccesses", true );
		}
	}
}
