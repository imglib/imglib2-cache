package net.imglib2.cache.img;

import net.imglib2.img.Img;
import net.imglib2.img.cell.AbstractCellImg;

/**
 * Populates cells with data.
 *
 * @param <T>
 *            pixel type
 *
 * @author Tobias Pietzsch
 */
public interface CellLoader< T >
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
	void load( Img< T > cell ) throws Exception;
}
