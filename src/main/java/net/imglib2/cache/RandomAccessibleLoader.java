package net.imglib2.cache;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.view.Views;

/**
 * A {@link CellLoader} that copies its data from a {@link RandomAccessible}.
 *
 * @param <T>
 *
 * @author Philipp Hanslovsky
 * @author Stephan Saalfeld
 */
/* TODO: move to different package?
 *  net.imglib2.cache.img?
 *  net.imglib2.cache.img.util?
 */
public class RandomAccessibleLoader< T extends NativeType< T > > implements CellLoader< T >
{
	private final RandomAccessible< T > source;

	public RandomAccessibleLoader( final RandomAccessible< T > source )
	{
		this.source = source;
	}

	@Override
	public void load( final SingleCellArrayImg< T, ? > cell )
	{
		for ( Cursor< T > s = Views.flatIterable( Views.interval( source, cell ) ).cursor(), t = cell.cursor(); s.hasNext(); )
			t.next().set( s.next() );
		// TOOD: benchmark.
//		LoopBuilder.setImages( Views.interval( source, cell ), cell ).forEachPixel( ( i, o ) -> o.set( i ) );
	}
}
