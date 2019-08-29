package net.imglib2.cache.img;

import net.imglib2.Cursor;
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
						.cacheType( DiskCachedCellImgOptions.CacheType.BOUNDED )
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
