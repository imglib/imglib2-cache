package net.imglib2.cache.img;

import java.util.function.Function;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.cache.CacheLoader;
import net.imglib2.img.AbstractNativeImg;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.basictypeaccess.AccessFlags;
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
import net.imglib2.type.PrimitiveType;
import net.imglib2.type.PrimitiveTypeInfo;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * A {@link CacheLoader} that produces cells from a given source
 * {@link RandomAccessible}. The cells are backed by {@link ArrayDataAccess} of
 * type {@code CA}, with the correct dimensions, etc.
 * <p>
 * Usually, it should be created through static helper methods
 * {@link #get(CellGrid, RandomAccessible, AccessFlags...)} or
 * {@link #get(CellGrid, RandomAccessible, NativeType, AccessFlags...)} to get
 * the desired primitive type and dirty/volatile variant.
 * </p>
 * <p>
 * The access backing the cells is filled through an intermediate access type
 * {@code A} which is the basic variant of the final access type {@code CA}. For
 * example, this might be {@code A =} {@link ByteArray} and {@code CA =}
 * {@link DirtyByteArray}. The reason for this is that we do not want the dirty
 * flag to be triggered by filling in the initial values.
 * </p>
 *
 * @param <T>
 *            source type
 * @param <A>
 *            intermediate access type
 * @param <CA>
 *            access type of cells
 *
 * @author Tobias Pietzsch
 */
public class RandomAccessibleCacheLoader<
		T extends NativeType< T >,
		A extends ArrayDataAccess< A >,
		CA extends ArrayDataAccess< CA > > implements CacheLoader< Long, Cell< CA > >
{
	private final CellGrid grid;

	private final RandomAccessible< T > source;

	private final T type;

	private final A creator;

	private final Fraction entitiesPerPixel;

	private final Function< A, CA > rewrap;

	public RandomAccessibleCacheLoader(
			final CellGrid grid,
			final RandomAccessible< T > source,
			final T type,
			final A creator,
			final Function< A, CA > rewrap )
	{
		this.grid = grid;
		this.source = source;
		this.type = type;
		this.creator = creator;
		this.rewrap = rewrap;
		entitiesPerPixel = type.getEntitiesPerPixel();
	}

	@Override
	public Cell< CA > get( final Long key ) throws Exception
	{
		final long index = key;

		final int n = grid.numDimensions();
		final long[] cellMin = new long[ n ];
		final int[] cellDims = new int[ n ];
		final long[] cellMax = new long[ n ];

		grid.getCellDimensions( index, cellMin, cellDims );
		final long numEntities = entitiesPerPixel.mulCeil( Intervals.numElements( cellDims ) );
		final A data = creator.createArray( ( int ) numEntities );
		final T t = createType( data );
		t.updateIndex( 0 );
		for ( int d = 0; d < n; ++d )
			cellMax[ d ] = cellMin[ d ] + cellDims[ d ] - 1;
		for ( final T s : Views.interval( source, cellMin, cellMax ) )
		{
			t.set( s );
			t.incIndex();
		}
		return new Cell<>( cellDims, cellMin, rewrap.apply( data ) );
	}

	public static < T extends NativeType< T >, A extends ArrayDataAccess< A >, CA extends ArrayDataAccess< CA > > RandomAccessibleCacheLoader< T, A, CA > get(
			final CellGrid grid,
			final RandomAccessible< T > source,
			final AccessFlags... flags )
	{
		return get( grid, source, source.randomAccess().get(), flags );
	}

	@SuppressWarnings( "unchecked" )
	public static < T extends NativeType< T >, A extends ArrayDataAccess< A >, CA extends ArrayDataAccess< CA > > RandomAccessibleCacheLoader< T, A, CA > get(
			final CellGrid grid,
			final RandomAccessible< T > source,
			final T type,
			final AccessFlags... flags )
	{
		final PrimitiveType primitiveType = type.getPrimitiveTypeInfo().getPrimitiveType();
		final boolean dirty = AccessFlags.isDirty( flags );
		final boolean volatil = AccessFlags.isVolatile( flags );
		switch ( primitiveType )
		{
		case BYTE:
			return dirty
					? ( volatil
							? ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new ByteArray( 0 ), a -> new DirtyVolatileByteArray( a.getCurrentStorageArray(), true ) )
							: ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new ByteArray( 0 ), a -> new DirtyByteArray( a.getCurrentStorageArray() ) ) )
					: ( volatil
							? ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new ByteArray( 0 ), a -> new VolatileByteArray( a.getCurrentStorageArray(), true ) )
							: ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new ByteArray( 0 ), a -> a ) );
		case CHAR:
			return dirty
					? ( volatil
							? ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new CharArray( 0 ), a -> new DirtyVolatileCharArray( a.getCurrentStorageArray(), true ) )
							: ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new CharArray( 0 ), a -> new DirtyCharArray( a.getCurrentStorageArray() ) ) )
					: ( volatil
							? ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new CharArray( 0 ), a -> new VolatileCharArray( a.getCurrentStorageArray(), true ) )
							: ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new CharArray( 0 ), a -> a ) );
		case DOUBLE:
			return dirty
					? ( volatil
							? ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new DoubleArray( 0 ), a -> new DirtyVolatileDoubleArray( a.getCurrentStorageArray(), true ) )
							: ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new DoubleArray( 0 ), a -> new DirtyDoubleArray( a.getCurrentStorageArray() ) ) )
					: ( volatil
							? ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new DoubleArray( 0 ), a -> new VolatileDoubleArray( a.getCurrentStorageArray(), true ) )
							: ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new DoubleArray( 0 ), a -> a ) );
		case FLOAT:
			return dirty
					? ( volatil
							? ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new FloatArray( 0 ), a -> new DirtyVolatileFloatArray( a.getCurrentStorageArray(), true ) )
							: ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new FloatArray( 0 ), a -> new DirtyFloatArray( a.getCurrentStorageArray() ) ) )
					: ( volatil
							? ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new FloatArray( 0 ), a -> new VolatileFloatArray( a.getCurrentStorageArray(), true ) )
							: ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new FloatArray( 0 ), a -> a ) );
		case INT:
			return dirty
					? ( volatil
							? ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new IntArray( 0 ), a -> new DirtyVolatileIntArray( a.getCurrentStorageArray(), true ) )
							: ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new IntArray( 0 ), a -> new DirtyIntArray( a.getCurrentStorageArray() ) ) )
					: ( volatil
							? ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new IntArray( 0 ), a -> new VolatileIntArray( a.getCurrentStorageArray(), true ) )
							: ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new IntArray( 0 ), a -> a ) );
		case LONG:
			return dirty
					? ( volatil
							? ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new LongArray( 0 ), a -> new DirtyVolatileLongArray( a.getCurrentStorageArray(), true ) )
							: ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new LongArray( 0 ), a -> new DirtyLongArray( a.getCurrentStorageArray() ) ) )
					: ( volatil
							? ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new LongArray( 0 ), a -> new VolatileLongArray( a.getCurrentStorageArray(), true ) )
							: ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new LongArray( 0 ), a -> a ) );
		case SHORT:
			return dirty
					? ( volatil
							? ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new ShortArray( 0 ), a -> new DirtyVolatileShortArray( a.getCurrentStorageArray(), true ) )
							: ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new ShortArray( 0 ), a -> new DirtyShortArray( a.getCurrentStorageArray() ) ) )
					: ( volatil
							? ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new ShortArray( 0 ), a -> new VolatileShortArray( a.getCurrentStorageArray(), true ) )
							: ( RandomAccessibleCacheLoader< T, A, CA > ) new RandomAccessibleCacheLoader<>( grid, source, type, new ShortArray( 0 ), a -> a ) );
		default:
			return null;
		}
	}

	@SuppressWarnings( "unchecked" )
	private T createType( final A access )
	{
		return ( ( PrimitiveTypeInfo< T, ? super A > ) type.getPrimitiveTypeInfo() ).createLinkedType( new NoImg<>( access ) );
	}

	static class NoImg< T extends NativeType< T >, A > extends AbstractNativeImg< T, A >
	{
		public NoImg( final A data)
		{
			super( new long[] { 1 }, new Fraction() );
			this.data = data;
		}

		private final A data;

		@Override
		public A update( final Object updater )
		{
			return data;
		}

		@Override
		public ImgFactory< T > factory()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Img< T > copy()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public RandomAccess< T > randomAccess()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Cursor< T > cursor()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Cursor< T > localizingCursor()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Object iterationOrder()
		{
			throw new UnsupportedOperationException();
		}
	}
}
