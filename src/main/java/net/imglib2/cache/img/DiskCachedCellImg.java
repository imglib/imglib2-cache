package net.imglib2.cache.img;

import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.LazyCellImg;
import net.imglib2.type.NativeType;
import net.imglib2.util.Fraction;

/**
 * A {@link LazyCellImg} that creates empty Cells lazily when they are accessed
 * and stores (modified) Cells in a disk cache when memory runs full.
 *
 * @param <T>
 *            the pixel type
 * @param <A>
 *            the underlying native access type
 *
 * @author Tobias Pietzsch
 */
public class DiskCachedCellImg< T extends NativeType< T >, A > extends LazyCellImg< T, A >
{
	private final DiskCachedCellImgFactory< T > factory;

	public DiskCachedCellImg(
			final DiskCachedCellImgFactory< T > factory,
			final CellGrid grid,
			final Fraction entitiesPerPixel,
			final Get< Cell< A > > get )
	{
		super( grid, entitiesPerPixel, get );
		this.factory = factory;
	}

	@Override
	public ImgFactory< T > factory()
	{
		return factory;
	}
}
