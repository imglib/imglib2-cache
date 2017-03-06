package net.imglib2.cache.img;

import java.util.Arrays;

import net.imglib2.cache.CacheLoader;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.CharArray;
import net.imglib2.img.basictypeaccess.array.DirtyByteArray;
import net.imglib2.img.basictypeaccess.array.DirtyCharArray;
import net.imglib2.img.basictypeaccess.array.DirtyDoubleArray;
import net.imglib2.img.basictypeaccess.array.DirtyFloatArray;
import net.imglib2.img.basictypeaccess.array.DirtyIntArray;
import net.imglib2.img.basictypeaccess.array.DirtyLongArray;
import net.imglib2.img.basictypeaccess.array.DirtyShortArray;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileCharArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileDoubleArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileIntArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileLongArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileShortArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileCharArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileDoubleArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;

/**
 * A {@link CacheLoader} that produces empty cells of {@link ArrayDataAccess}
 * type {@code A}, with the correct dimensions, etc.
 * <p>
 * Usually, it should be created through static helper methods
 * {@link #get(CellGrid, Fraction, PrimitiveType, AccessFlags...)} or
 * {@link #get(CellGrid, NativeType, AccessFlags...)} to get the desired
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
			final AccessFlags ... flags )
	{
		return get( grid, type.getEntitiesPerPixel(), PrimitiveType.forNativeType( type ), flags );
	}

	@SuppressWarnings( "unchecked" )
	public static < A extends ArrayDataAccess< A > > EmptyCellCacheLoader< A > get(
			final CellGrid grid,
			final Fraction entitiesPerPixel,
			final PrimitiveType primitiveType,
			final AccessFlags ... flags )
	{
		final boolean dirty = Arrays.asList( flags ).contains( AccessFlags.DIRTY );
		final boolean volatil = Arrays.asList( flags ).contains( AccessFlags.VOLATILE );
		switch ( primitiveType )
		{
		case BYTE:
			return dirty
					? ( volatil
							? (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DirtyVolatileByteArray( 0, true ) )
							: (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DirtyByteArray( 0 ) ) )
					: ( volatil
							? (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new VolatileByteArray( 0, true ) )
							: (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new ByteArray( 0 ) ) );
		case CHAR:
			return dirty
					? ( volatil
							? (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DirtyVolatileCharArray( 0, true ) )
							: (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DirtyCharArray( 0 ) ) )
					: ( volatil
							? (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new VolatileCharArray( 0, true ) )
							: (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new CharArray( 0 ) ) );
		case DOUBLE:
			return dirty
					? ( volatil
							? (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DirtyVolatileDoubleArray( 0, true ) )
							: (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DirtyDoubleArray( 0 ) ) )
					: ( volatil
							? (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new VolatileDoubleArray( 0, true ) )
							: (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DoubleArray( 0 ) ) );
		case FLOAT:
			return dirty
					? ( volatil
							? (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DirtyVolatileFloatArray( 0, true ) )
							: (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DirtyFloatArray( 0 ) ) )
					: ( volatil
							? (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new VolatileFloatArray( 0, true ) )
							: (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new FloatArray( 0 ) ) );
		case INT:
			return dirty
					? ( volatil
							? (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DirtyVolatileIntArray( 0, true ) )
							: (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DirtyIntArray( 0 ) ) )
					: ( volatil
							? (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new VolatileIntArray( 0, true ) )
							: (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new IntArray( 0 ) ) );
		case LONG:
			return dirty
					? ( volatil
							? (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DirtyVolatileLongArray( 0, true ) )
							: (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DirtyLongArray( 0 ) ) )
					: ( volatil
							? (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new VolatileLongArray( 0, true ) )
							: (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new LongArray( 0 ) ) );
		case SHORT:
			return dirty
					? ( volatil
							? (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DirtyVolatileShortArray( 0, true ) )
							: (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new DirtyShortArray( 0 ) ) )
					: ( volatil
							? (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new VolatileShortArray( 0, true ) )
							: (net.imglib2.cache.img.EmptyCellCacheLoader< A > ) new EmptyCellCacheLoader<>( grid, entitiesPerPixel, new ShortArray( 0 ) ) );
		default:
			return null;
		}
	}
}
