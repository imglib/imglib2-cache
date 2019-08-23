package net.imglib2.cache.img;

import net.imglib2.cache.Cache;
import net.imglib2.cache.IoSync;
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
public class DiskCachedCellImg< T extends NativeType< T >, A > extends CachedCellImg< T, A >
{
	private final DiskCachedCellImgFactory< T > factory;

	private final IoSync iosync;

	public DiskCachedCellImg(
			final DiskCachedCellImgFactory< T > factory,
			final CellGrid grid,
			final Fraction entitiesPerPixel,
			final Cache< Long, Cell< A > > cache,
			final IoSync iosync,
			final A accessType )
	{
		super( grid, entitiesPerPixel, cache, accessType );
		this.factory = factory;
		this.iosync = iosync;
	}

	public void shutdown()
	{
		iosync.shutdown();
	}

	@Override
	public ImgFactory< T > factory()
	{
		return factory;
	}
}
