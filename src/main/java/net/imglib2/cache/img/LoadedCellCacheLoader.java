package net.imglib2.cache.img;

import net.imglib2.Dirty;
import net.imglib2.cache.CacheLoader;
import net.imglib2.img.basictypeaccess.ByteAccess;
import net.imglib2.img.basictypeaccess.CharAccess;
import net.imglib2.img.basictypeaccess.DoubleAccess;
import net.imglib2.img.basictypeaccess.FloatAccess;
import net.imglib2.img.basictypeaccess.IntAccess;
import net.imglib2.img.basictypeaccess.LongAccess;
import net.imglib2.img.basictypeaccess.ShortAccess;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.CharArray;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;

/**
 * A {@link CacheLoader} that produces cells of {@link ArrayDataAccess} type
 * {@code A} and uses a {@link CellLoader} to populate them with data.
 * <p>
 * Usually, {@link LoadedCellCacheLoader} should be created through static
 * helper methods {@link #get(CellGrid, CellLoader, NativeType, AccessFlags...)}
 * or
 * {@link #get(CellGrid, CellLoader, NativeType, PrimitiveType, AccessFlags...)}
 * to get the desired primitive type and dirty/volatile variant.
 * </p>
 *
 * @param <T>
 *            pixel type
 * @param <A>
 *            access type
 *
 * @author Tobias Pietzsch
 */
public class LoadedCellCacheLoader< T extends NativeType< T >, A extends ArrayDataAccess< A > > implements CacheLoader< Long, Cell< A > >
{
	private final CellGrid grid;

	private final Fraction entitiesPerPixel;

	private final T type;

	private final A creator;

	private final ArrayDataAccessWrapper< A > wrapper;

	private final CellLoader< T > loader;

	public LoadedCellCacheLoader(
			final CellGrid grid,
			final T type,
			final A creator,
			final ArrayDataAccessWrapper< A > wrapper,
			final CellLoader< T > loader )
	{
		this.grid = grid;
		this.entitiesPerPixel = type.getEntitiesPerPixel();
		this.type = type;
		this.creator = creator;
		this.wrapper = wrapper;
		this.loader = loader;
	}

	@Override
	public Cell< A > get( final Long key ) throws Exception
	{
		final long index = key;
		final long[] cellMin = new long[ grid.numDimensions() ];
		final int[] cellDims = new int[ grid.numDimensions() ];
		grid.getCellDimensions( index, cellMin, cellDims );
		final long numEntities = entitiesPerPixel.mulCeil( Intervals.numElements( cellDims ) );
		final A array = creator.createArray( ( int ) numEntities );
		loader.load( new SingleCellArrayImg<>( cellDims, cellMin, wrapper.wrap( array ), type ) );
		return new Cell<>( cellDims, cellMin, array );
	}

	public static < T extends NativeType< T >, A extends ArrayDataAccess< A > > LoadedCellCacheLoader< T, A > get(
			final CellGrid grid,
			final CellLoader< T > loader,
			final T type,
			final AccessFlags ... flags )
	{
		return get( grid, loader, type, PrimitiveType.forNativeType( type ), flags );
	}

	public static < T extends NativeType< T >, A extends ArrayDataAccess< A > > LoadedCellCacheLoader< T, A > get(
			final CellGrid grid,
			final CellLoader< T > loader,
			final T type,
			final PrimitiveType primitiveType,
			final AccessFlags ... flags )
	{
		final A creator = ArrayDataAccessFactory.get( primitiveType, flags );
		final ArrayDataAccessWrapper< A > wrapper = getWrapper( primitiveType, flags );
		return creator == null ? null : new LoadedCellCacheLoader<>( grid, type, creator, wrapper, loader );
	}

	@SuppressWarnings( "unchecked" )
	static < A extends ArrayDataAccess< A > > ArrayDataAccessWrapper< A > getWrapper(
			final PrimitiveType primitiveType,
			final AccessFlags ... flags )
	{
		final boolean dirty = AccessFlags.isDirty( flags );
		final boolean volatil = AccessFlags.isVolatile( flags );
		switch ( primitiveType )
		{
		case BYTE:
			return dirty
					? ( volatil
							? ( ArrayDataAccessWrapper< A > ) new ByteAccessWrapper<>()
							: ( ArrayDataAccessWrapper< A > ) new ByteAccessWrapper<>() )
					: ( volatil
							? ( ArrayDataAccessWrapper< A > ) new ByteAccessPassThrough<>()
							: ( ArrayDataAccessWrapper< A > ) new ByteAccessPassThrough<>() );
		case CHAR:
			return dirty
					? ( volatil
							? ( ArrayDataAccessWrapper< A > ) new CharAccessWrapper<>()
							: ( ArrayDataAccessWrapper< A > ) new CharAccessWrapper<>() )
					: ( volatil
							? ( ArrayDataAccessWrapper< A > ) new CharAccessPassThrough<>()
							: ( ArrayDataAccessWrapper< A > ) new CharAccessPassThrough<>() );
		case DOUBLE:
			return dirty
					? ( volatil
							? ( ArrayDataAccessWrapper< A > ) new DoubleAccessWrapper<>()
							: ( ArrayDataAccessWrapper< A > ) new DoubleAccessWrapper<>() )
					: ( volatil
							? ( ArrayDataAccessWrapper< A > ) new DoubleAccessPassThrough<>()
							: ( ArrayDataAccessWrapper< A > ) new DoubleAccessPassThrough<>() );
		case FLOAT:
			return dirty
					? ( volatil
							? ( ArrayDataAccessWrapper< A > ) new FloatAccessWrapper<>()
							: ( ArrayDataAccessWrapper< A > ) new FloatAccessWrapper<>() )
					: ( volatil
							? ( ArrayDataAccessWrapper< A > ) new FloatAccessPassThrough<>()
							: ( ArrayDataAccessWrapper< A > ) new FloatAccessPassThrough<>() );
		case INT:
			return dirty
					? ( volatil
							? ( ArrayDataAccessWrapper< A > ) new IntAccessWrapper<>()
							: ( ArrayDataAccessWrapper< A > ) new IntAccessWrapper<>() )
					: ( volatil
							? ( ArrayDataAccessWrapper< A > ) new IntAccessPassThrough<>()
							: ( ArrayDataAccessWrapper< A > ) new IntAccessPassThrough<>() );
		case LONG:
			return dirty
					? ( volatil
							? ( ArrayDataAccessWrapper< A > ) new LongAccessWrapper<>()
							: ( ArrayDataAccessWrapper< A > ) new LongAccessWrapper<>() )
					: ( volatil
							? ( ArrayDataAccessWrapper< A > ) new LongAccessPassThrough<>()
							: ( ArrayDataAccessWrapper< A > ) new LongAccessPassThrough<>() );
		case SHORT:
			return dirty
					? ( volatil
							? ( ArrayDataAccessWrapper< A > ) new ShortAccessWrapper<>()
							: ( ArrayDataAccessWrapper< A > ) new ShortAccessWrapper<>() )
					: ( volatil
							? ( ArrayDataAccessWrapper< A > ) new ShortAccessPassThrough<>()
							: ( ArrayDataAccessWrapper< A > ) new ShortAccessPassThrough<>() );
		default:
			return null;
		}
	}

	/**
	 * Wraps an {@link ArrayDataAccess} of type {@code A} as another {@link ArrayDataAccess}.
	 * This is used to strip the dirty flag off {@link Dirty} accesses for initially populating a cell with data (otherwise the cell would immediately be marked dirty).
	 */
	public static interface ArrayDataAccessWrapper< A extends ArrayDataAccess< A > >
	{
		Object wrap( A access );
	}

	static class ByteAccessWrapper< A extends ArrayDataAccess< A > & ByteAccess > implements ArrayDataAccessWrapper< A >
	{
		@Override
		public ByteArray wrap( final A access )
		{
			return new ByteArray( ( byte[] ) access.getCurrentStorageArray() );
		}
	}

	static class ByteAccessPassThrough< A extends ArrayDataAccess< A > & ByteAccess > implements ArrayDataAccessWrapper< A >
	{
		@Override
		public ByteAccess wrap( final A access )
		{
			return access;
		}
	}

	static class CharAccessWrapper< A extends ArrayDataAccess< A > & CharAccess > implements ArrayDataAccessWrapper< A >
	{
		@Override
		public CharArray wrap( final A access )
		{
			return new CharArray( ( char[] ) access.getCurrentStorageArray() );
		}
	}

	static class CharAccessPassThrough< A extends ArrayDataAccess< A > & CharAccess > implements ArrayDataAccessWrapper< A >
	{
		@Override
		public CharAccess wrap( final A access )
		{
			return access;
		}
	}

	static class DoubleAccessWrapper< A extends ArrayDataAccess< A > & DoubleAccess > implements ArrayDataAccessWrapper< A >
	{
		@Override
		public DoubleArray wrap( final A access )
		{
			return new DoubleArray( ( double[] ) access.getCurrentStorageArray() );
		}
	}

	static class DoubleAccessPassThrough< A extends ArrayDataAccess< A > & DoubleAccess > implements ArrayDataAccessWrapper< A >
	{
		@Override
		public DoubleAccess wrap( final A access )
		{
			return access;
		}
	}

	static class FloatAccessWrapper< A extends ArrayDataAccess< A > & FloatAccess > implements ArrayDataAccessWrapper< A >
	{
		@Override
		public FloatArray wrap( final A access )
		{
			return new FloatArray( ( float[] ) access.getCurrentStorageArray() );
		}
	}

	static class FloatAccessPassThrough< A extends ArrayDataAccess< A > & FloatAccess > implements ArrayDataAccessWrapper< A >
	{
		@Override
		public FloatAccess wrap( final A access )
		{
			return access;
		}
	}

	static class IntAccessWrapper< A extends ArrayDataAccess< A > & IntAccess > implements ArrayDataAccessWrapper< A >
	{
		@Override
		public IntArray wrap( final A access )
		{
			return new IntArray( ( int[] ) access.getCurrentStorageArray() );
		}
	}

	static class IntAccessPassThrough< A extends ArrayDataAccess< A > & IntAccess > implements ArrayDataAccessWrapper< A >
	{
		@Override
		public IntAccess wrap( final A access )
		{
			return access;
		}
	}

	static class ShortAccessWrapper< A extends ArrayDataAccess< A > & ShortAccess > implements ArrayDataAccessWrapper< A >
	{
		@Override
		public ShortArray wrap( final A access )
		{
			return new ShortArray( ( short[] ) access.getCurrentStorageArray() );
		}
	}

	static class ShortAccessPassThrough< A extends ArrayDataAccess< A > & ShortAccess > implements ArrayDataAccessWrapper< A >
	{
		@Override
		public ShortAccess wrap( final A access )
		{
			return access;
		}
	}

	static class LongAccessWrapper< A extends ArrayDataAccess< A > & LongAccess > implements ArrayDataAccessWrapper< A >
	{
		@Override
		public LongArray wrap( final A access )
		{
			return new LongArray( ( long[] ) access.getCurrentStorageArray() );
		}
	}

	static class LongAccessPassThrough< A extends ArrayDataAccess< A > & LongAccess > implements ArrayDataAccessWrapper< A >
	{
		@Override
		public LongAccess wrap( final A access )
		{
			return access;
		}
	}
}
