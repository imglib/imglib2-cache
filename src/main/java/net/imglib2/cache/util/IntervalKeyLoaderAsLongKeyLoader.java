/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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

import java.util.stream.IntStream;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.cache.CacheLoader;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;

/**
 *
 * @author Philipp Hanslovsky
 *
 *         This {@link CacheLoader} maps a {@link Long} key into an
 *         {@link Interval} based on a {@link CellGrid}. The creation of an
 *         appropriate <code>A</code> is then delegated to a {@link CacheLoader}
 *         that generates an <code>A</code> from an interval.
 *
 *         Implementing a {@link CacheLoader} that fills {@link Cell}s of an
 *         {@link AbstractCellImg} requires a lot of boilerplate code for
 *         mapping the {@link Long} index of a {@link Cell} into the
 *         {@link Interval} that is backed by the {@link Cell}. The
 *         {@link IntervalKeyLoaderAsLongKeyLoader} generates this mapping and
 *         reduces the amount of boilerplate code for the caller.
 *
 * @param <A>
 *            Type of pixel store for {@link Cell}.
 */
public class IntervalKeyLoaderAsLongKeyLoader< A > implements CacheLoader< Long, Cell< A > >
{

	private final CellGrid grid;

	private final CacheLoader< Interval, A > functor;

	public IntervalKeyLoaderAsLongKeyLoader( final CellGrid grid, final CacheLoader< Interval, A > functor )
	{
		this.grid = grid;
		this.functor = functor;
	}

	@Override
	public Cell< A > get( final Long key ) throws Exception
	{
		final long index = key;

		final int n = grid.numDimensions();
		final long[] cellMin = new long[ n ];
		final int[] cellDims = new int[ n ];
		grid.getCellDimensions( index, cellMin, cellDims );
		final long[] cellMax = IntStream.range( 0, n ).mapToLong( d -> cellMin[ d ] + cellDims[ d ] - 1 ).toArray();
		final A result = functor.get( new FinalInterval( cellMin, cellMax ) );
		return new Cell<>( cellDims, cellMin, result );
	}

}
