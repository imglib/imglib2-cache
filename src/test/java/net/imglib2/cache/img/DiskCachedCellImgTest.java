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
package net.imglib2.cache.img;

import net.imglib2.Cursor;
import net.imglib2.cache.img.optional.CacheOptions;
import net.imglib2.img.Img;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.junit.Test;

public class DiskCachedCellImgTest
{
	/**
	 * Test whether caching evicted cells to disk (and reading back) works correctly.
	 */
	@Test
	public void testDiskCachedCellImg()
	{
		final long[] dims = new long[] { 20_000_000 };

		FunctionRandomAccessible< FloatType > src = new FunctionRandomAccessible<>( 1, ( pos, type ) -> type.set( pos.getFloatPosition( 0 ) ), FloatType::new );
		final Img< FloatType > dst = new DiskCachedCellImgFactory<>( new FloatType(),
				DiskCachedCellImgOptions.options()
						.cacheType( CacheOptions.CacheType.BOUNDED )
						.maxCacheSize( 2 )
						.cellDimensions( 256 ) ).create( dims );

		final Cursor< FloatType > srcCursor = Views.interval( src, dst ).cursor();
		final Cursor< FloatType > dstCursor = dst.cursor();

		while ( srcCursor.hasNext() )
			dstCursor.next().set( srcCursor.next() );

		srcCursor.reset();
		dstCursor.reset();

		while ( srcCursor.hasNext() )
			if ( dstCursor.next().get() != srcCursor.next().get() )
				throw new RuntimeException();
	}
}
