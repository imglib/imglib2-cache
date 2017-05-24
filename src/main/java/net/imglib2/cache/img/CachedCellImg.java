package net.imglib2.cache.img;

import net.imglib2.cache.Cache;
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
public class CachedCellImg< T extends NativeType< T >, A > extends LazyCellImg< T, A >
{
	private final Cache< Long, Cell< A > > cache;

	private final A accessType;

	public CachedCellImg(
			final CellGrid grid,
			final Fraction entitiesPerPixel,
			final Cache< Long, Cell< A > > cache,
			final A accessType )
	{
		super( grid, entitiesPerPixel, cache.unchecked()::get );
		this.cache = cache;
		this.accessType = accessType;
	}

	public CachedCellImg(
			final CellGrid grid,
			final T type,
			final Cache< Long, Cell< A > > cache,
			final A accessType )
	{
		super( grid, type, cache.unchecked()::get );
		this.cache = cache;
		this.accessType = accessType;
	}

	public Cache< Long, Cell< A > > getCache()
	{
		return cache;
	}

	public A getAccessType()
	{
		return accessType;
	}
}
