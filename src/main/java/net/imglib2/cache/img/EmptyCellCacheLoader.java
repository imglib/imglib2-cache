package net.imglib2.cache.img;

import java.util.Set;

import net.imglib2.cache.CacheLoader;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;

/**
 * A {@link CacheLoader} that produces empty cells of {@link ArrayDataAccess}
 * type {@code A}, with the correct dimensions, etc.
 * <p>
 * Usually, it should be created through static helper methods
 * {@link #get(CellGrid, Fraction, PrimitiveType, Set)} or
 * {@link #get(CellGrid, NativeType, Set)} to get the desired
 * primitive type and dirty/volatile variant.
 * </p>
 *
 * @param <A>
 *            access type
 *
 * @author Tobias Pietzsch
 */
public class EmptyCellCacheLoader< A extends ArrayDataAccess< A > > implements CacheLoader< Long, Cell< A > >
{
	private final CellGrid grid;

	private final Fraction entitiesPerPixel;

	private final A creator;

	public EmptyCellCacheLoader(
			final CellGrid grid,
			final Fraction entitiesPerPixel,
			final A creator )
	{
		this.grid = grid;
		this.entitiesPerPixel = entitiesPerPixel;
		this.creator = creator;
	}

	@Override
	public Cell< A > get( final Long key ) throws Exception
	{
		final long index = key;
		final long[] cellMin = new long[ grid.numDimensions() ];
		final int[] cellDims = new int[ grid.numDimensions() ];
		grid.getCellDimensions( index, cellMin, cellDims );
		final long numEntities = entitiesPerPixel.mulCeil( Intervals.numElements( cellDims ) );
		return new Cell<>( cellDims, cellMin, creator.createArray( ( int ) numEntities ) );
	}

	public static < T extends NativeType< T >, A extends ArrayDataAccess< A > > EmptyCellCacheLoader< A > get(
			final CellGrid grid,
			final T type,
			final Set< AccessFlags > flags )
	{
		return get( grid, type.getEntitiesPerPixel(), type.getNativeTypeFactory().getPrimitiveType(), flags );
	}

	public static < A extends ArrayDataAccess< A > > EmptyCellCacheLoader< A > get(
			final CellGrid grid,
			final Fraction entitiesPerPixel,
			final PrimitiveType primitiveType,
			final Set< AccessFlags > flags )
	{
		final A creator = ArrayDataAccessFactory.get( primitiveType, flags );
		return creator == null ? null : new EmptyCellCacheLoader<>( grid, entitiesPerPixel, creator );
	}
}
