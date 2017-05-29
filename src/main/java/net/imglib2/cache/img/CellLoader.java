package net.imglib2.cache.img;

import net.imglib2.img.Img;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.type.NativeType;

/**
 * Populates cells with data.
 *
 * @param <T>
 *            pixel type
 *
 * @author Tobias Pietzsch
 */
public interface CellLoader< T extends NativeType< T > >
{
	/**
	 * Fill the specified cell with data.
	 *
	 * @param cell
	 *            the cell to load. The cell is given as a {@link Img} with
	 *            minimum and maximum reflecting the part of the
	 *            {@link AbstractCellImg} that is covered.
	 * @throws Exception
	 */
	public void load( SingleCellArrayImg< T, ? > cell ) throws Exception;
}
