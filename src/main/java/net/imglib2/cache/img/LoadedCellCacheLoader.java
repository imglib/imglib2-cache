/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2023 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.imglib2.cache.img;

import static net.imglib2.img.basictypeaccess.AccessFlags.DIRTY;

import java.util.Set;

import net.imglib2.Dirty;
import net.imglib2.cache.CacheLoader;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
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
import net.imglib2.type.PrimitiveType;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;

/**
 * A {@link CacheLoader} that produces cells of {@link ArrayDataAccess} type
 * {@code A} and uses a {@link CellLoader} to populate them with data.
 * <p>
 * Usually, {@link LoadedCellCacheLoader} should be created through static
 * helper methods {@link #get(CellGrid, CellLoader, NativeType, Set)}
 * or
 * {@link #get(CellGrid, CellLoader, NativeType, PrimitiveType, Set)}
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

	private final ArrayDataAccessWrapper< A, ? > wrapper;

	private final CellLoader< T > loader;

	public LoadedCellCacheLoader(
			final CellGrid grid,
			final T type,
			final A creator,
			final ArrayDataAccessWrapper< A, ? > wrapper,
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
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final SingleCellArrayImg< T, ? > img = new SingleCellArrayImg( cellDims, cellMin, wrapper.wrap( array ), wrapper.wrapDirty( array ), type );
		loader.load( img );
		return new Cell<>( cellDims, cellMin, array );
	}

	public static < T extends NativeType< T >, A extends ArrayDataAccess< A > > LoadedCellCacheLoader< T, A > get(
			final CellGrid grid,
			final CellLoader< T > loader,
			final T type,
			final Set< AccessFlags > flags )
	{
		return get( grid, loader, type, type.getNativeTypeFactory().getPrimitiveType(), flags );
	}

	public static < T extends NativeType< T >, A extends ArrayDataAccess< A > > LoadedCellCacheLoader< T, A > get(
			final CellGrid grid,
			final CellLoader< T > loader,
			final T type,
			final PrimitiveType primitiveType,
			final Set< AccessFlags > flags )
	{
		final A creator = ArrayDataAccessFactory.get( primitiveType, flags );
		final ArrayDataAccessWrapper< A, ? > wrapper = getWrapper( primitiveType, flags );
		return creator == null ? null : new LoadedCellCacheLoader<>( grid, type, creator, wrapper, loader );
	}

	@SuppressWarnings( "unchecked" )
	static < A extends ArrayDataAccess< A > > ArrayDataAccessWrapper< A, ? > getWrapper(
			final PrimitiveType primitiveType,
			final Set< AccessFlags > flags )
	{
		final boolean dirty = flags.contains( DIRTY );
		switch ( primitiveType )
		{
		case BYTE:
			return dirty
					? ( ArrayDataAccessWrapper< A, ? > ) new ByteAccessWrapper<>()
					: ( ArrayDataAccessWrapper< A, ? > ) new PassThrough<>();
		case CHAR:
			return dirty
					? ( ArrayDataAccessWrapper< A, ? > ) new CharAccessWrapper<>()
					: ( ArrayDataAccessWrapper< A, ? > ) new PassThrough<>();
		case DOUBLE:
			return dirty
					? ( ArrayDataAccessWrapper< A, ? > ) new DoubleAccessWrapper<>()
					: ( ArrayDataAccessWrapper< A, ? > ) new PassThrough<>();
		case FLOAT:
			return dirty
					? ( ArrayDataAccessWrapper< A, ? > ) new FloatAccessWrapper<>()
					: ( ArrayDataAccessWrapper< A, ? > ) new PassThrough<>();
		case INT:
			return dirty
					? ( ArrayDataAccessWrapper< A, ? > ) new IntAccessWrapper<>()
					: ( ArrayDataAccessWrapper< A, ? > ) new PassThrough<>();
		case LONG:
			return dirty
					? ( ArrayDataAccessWrapper< A, ? > ) new LongAccessWrapper<>()
					: ( ArrayDataAccessWrapper< A, ? > ) new PassThrough<>();
		case SHORT:
			return dirty
					? ( ArrayDataAccessWrapper< A, ? > ) new ShortAccessWrapper<>()
					: ( ArrayDataAccessWrapper< A, ? > ) new PassThrough<>();
		default:
			return null;
		}
	}

	/**
	 * Wraps an {@link ArrayDataAccess} of type {@code A} as another
	 * {@link ArrayDataAccess}. This is used to strip the dirty flag off
	 * {@link Dirty} accesses for initially populating a cell with data
	 * (otherwise the cell would immediately be marked dirty).
	 * <p>
	 * Additionally, {@link #wrapDirty(ArrayDataAccess)} provides access to the
	 * dirty flag (if any) to be able to selectively mark cells as dirty from a
	 * {@link CellLoader}.
	 * </p>
	 */
	public interface ArrayDataAccessWrapper< A extends ArrayDataAccess< A >, W extends ArrayDataAccess< W > >
	{
		W wrap( A access );

		Dirty wrapDirty( A access );
	}

	static class PassThrough< A extends ArrayDataAccess< A > > implements ArrayDataAccessWrapper< A, A >
	{
		@Override
		public A wrap( final A access )
		{
			return access;
		}

		@Override
		public Dirty wrapDirty( final A access )
		{
			return new Dirty()
			{
				@Override
				public boolean isDirty()
				{
					return false;
				}

				@Override
				public void setDirty()
				{}

				@Override
				public void setDirty( final boolean dirty )
				{}
			};
		}
	};

	static class ByteAccessWrapper< A extends ArrayDataAccess< A > & ByteAccess & Dirty > implements ArrayDataAccessWrapper< A, ByteArray >
	{
		@Override
		public ByteArray wrap( final A access )
		{
			return new ByteArray( ( byte[] ) access.getCurrentStorageArray() );
		}

		@Override
		public Dirty wrapDirty( final A access )
		{
			return access;
		}
	}

	static class CharAccessWrapper< A extends ArrayDataAccess< A > & CharAccess & Dirty > implements ArrayDataAccessWrapper< A, CharArray >
	{
		@Override
		public CharArray wrap( final A access )
		{
			return new CharArray( ( char[] ) access.getCurrentStorageArray() );
		}

		@Override
		public Dirty wrapDirty( final A access )
		{
			return access;
		}
	}

	static class DoubleAccessWrapper< A extends ArrayDataAccess< A > & DoubleAccess & Dirty > implements ArrayDataAccessWrapper< A, DoubleArray >
	{
		@Override
		public DoubleArray wrap( final A access )
		{
			return new DoubleArray( ( double[] ) access.getCurrentStorageArray() );
		}

		@Override
		public Dirty wrapDirty( final A access )
		{
			return access;
		}
	}

	static class FloatAccessWrapper< A extends ArrayDataAccess< A > & FloatAccess & Dirty > implements ArrayDataAccessWrapper< A, FloatArray >
	{
		@Override
		public FloatArray wrap( final A access )
		{
			return new FloatArray( ( float[] ) access.getCurrentStorageArray() );
		}

		@Override
		public Dirty wrapDirty( final A access )
		{
			return access;
		}
	}

	static class IntAccessWrapper< A extends ArrayDataAccess< A > & IntAccess & Dirty > implements ArrayDataAccessWrapper< A, IntArray >
	{
		@Override
		public IntArray wrap( final A access )
		{
			return new IntArray( ( int[] ) access.getCurrentStorageArray() );
		}

		@Override
		public Dirty wrapDirty( final A access )
		{
			return access;
		}
	}

	static class ShortAccessWrapper< A extends ArrayDataAccess< A > & ShortAccess & Dirty > implements ArrayDataAccessWrapper< A, ShortArray >
	{
		@Override
		public ShortArray wrap( final A access )
		{
			return new ShortArray( ( short[] ) access.getCurrentStorageArray() );
		}

		@Override
		public Dirty wrapDirty( final A access )
		{
			return access;
		}
	}

	static class LongAccessWrapper< A extends ArrayDataAccess< A > & LongAccess & Dirty > implements ArrayDataAccessWrapper< A, LongArray >
	{
		@Override
		public LongArray wrap( final A access )
		{
			return new LongArray( ( long[] ) access.getCurrentStorageArray() );
		}

		@Override
		public Dirty wrapDirty( final A access )
		{
			return access;
		}
	}
}
