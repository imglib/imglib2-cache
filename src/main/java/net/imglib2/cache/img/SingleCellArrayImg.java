/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2020 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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

import java.util.Iterator;

import net.imglib2.AbstractCursor;
import net.imglib2.AbstractInterval;
import net.imglib2.AbstractLocalizable;
import net.imglib2.AbstractLocalizingCursor;
import net.imglib2.Cursor;
import net.imglib2.Dirty;
import net.imglib2.FlatIterationOrder;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.NativeImg;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.util.IntervalIndexer;

/**
 * A {@link NativeImg} representing a single cell of an {@link AbstractCellImg}.
 * It is similar to {@link ArrayImg} except that minimum is not at the origin
 * but at the minimum of the cell within the {@link AbstractCellImg}.
 *
 * @param <T>
 *            pixel type
 * @param <A>
 *            access type
 *
 * @author Tobias Pietzsch
 */
public class SingleCellArrayImg< T extends NativeType< T >, A extends ArrayDataAccess< A > >
	extends AbstractInterval
	implements NativeImg< T, A >
{
	final int[] steps;

	final int[] dimensions;

	private long size;

	private A data;

	private Dirty dirty;

	private T linkedType;

	@Override
	public void setLinkedType( final T type )
	{
		this.linkedType = type;
	}

	@Override
	public T createLinkedType()
	{
		try
		{
			return linkedType.duplicateTypeOnSameNativeImg();
		}
		catch ( final NullPointerException e )
		{
			return null;
		}
	}

	@Override
	public A update( final Object updater )
	{
		return data;
	}

	public SingleCellArrayImg( final int n )
	{
		super( n );
		steps = new int[ n ];
		dimensions = new int[ n ];
	}

	public SingleCellArrayImg( final int[] cellDims, final long[] cellMin, final A cellData, final Dirty dirtyFlag )
	{
		this( cellDims.length );
		reset( cellDims, cellMin, cellData, dirtyFlag );
	}

	/**
	 * Get the single primitive array containing the data of this cell.
	 *
	 * @return primitive array with the cell data. Actual type will be
	 *         {@code char[]}, {@code byte[]}, {@code short[]}, {@code int[]},
	 *         {@code long[]}, {@code double[]}, or {@code float[]}, depending
	 *         on the pixel type.
	 */
	public Object getStorageArray()
	{
		return data.getCurrentStorageArray();
	}

	/**
	 * If the cell is backed by a {@link Dirty}-capable access, flag it as
	 * dirty. (Otherwise, do nothing.)
	 */
	public void setDirty()
	{
		dirty.setDirty();
	}

	SingleCellArrayImg( final int[] cellDims, final long[] cellMin, final A cellData, final Dirty dirtyFlag, final T type )
	{
		this( cellDims, cellMin, cellData, dirtyFlag );

		@SuppressWarnings( "unchecked" )
		final NativeTypeFactory< T, ? super A > info = ( NativeTypeFactory< T, ? super A > ) type.getNativeTypeFactory();
		setLinkedType( info.createLinkedType( this ) );
	}

	void reset(	final int[] cellDims, final long[] cellMin, final A cellData, final Dirty dirtyFlag )
	{
		for ( int d = 0; d < n; ++d )
		{
			min[ d ] = cellMin[ d ];
			max[ d ] = min[ d ] + cellDims[ d ] - 1;
			dimensions[ d ] = cellDims[ d ];
		}
		IntervalIndexer.createAllocationSteps( cellDims, steps );
		size = steps[ n - 1 ] * cellDims[ n - 1 ];
		data = cellData;
		dirty = dirtyFlag;
	}

	@Override
	public RandomAccess< T > randomAccess()
	{
		return new CellArrayRandomAccess();
	}

	@Override
	public RandomAccess< T > randomAccess( final Interval interval )
	{
		return randomAccess();
	}

	class CellArrayRandomAccess extends AbstractLocalizable implements RandomAccess< T >
	{
		final T type;

		CellArrayRandomAccess( final CellArrayRandomAccess randomAccess )
		{
			super( SingleCellArrayImg.this.n );
			type = createLinkedType();
			type.updateContainer( this );
			type.updateIndex( randomAccess.type.getIndex() );
			System.arraycopy( randomAccess.position, 0, position, 0, n );
		}

		CellArrayRandomAccess()
		{
			super( SingleCellArrayImg.this.n );
			type = createLinkedType();

			System.arraycopy( min, 0, position, 0, n );

			type.updateContainer( this );
			type.updateIndex( 0 );
		}

		@Override
		public T get()
		{
			return type;
		}

		@Override
		public void fwd( final int d )
		{
			type.incIndex( steps[ d ] );
			++position[ d ];
		}

		@Override
		public void bck( final int d )
		{
			type.decIndex( steps[ d ] );
			--position[ d ];
		}

		@Override
		public void move( final int distance, final int d )
		{
			type.incIndex( steps[ d ] * distance );
			position[ d ] += distance;
		}

		@Override
		public void move( final long distance, final int d )
		{
			type.incIndex( steps[ d ] * ( int ) distance );
			position[ d ] += distance;
		}

		@Override
		public void move( final Localizable localizable )
		{
			int index = 0;
			for ( int d = 0; d < n; ++d )
			{
				final int distance = localizable.getIntPosition( d );
				position[ d ] += distance;
				index += distance * steps[ d ];
			}
			type.incIndex( index );
		}

		@Override
		public void move( final int[] distance )
		{
			int index = 0;
			for ( int d = 0; d < n; ++d )
			{
				position[ d ] += distance[ d ];
				index += distance[ d ] * steps[ d ];
			}
			type.incIndex( index );
		}

		@Override
		public void move( final long[] distance )
		{
			int index = 0;
			for ( int d = 0; d < n; ++d )
			{
				position[ d ] += distance[ d ];
				index += ( int ) distance[ d ] * steps[ d ];
			}
			type.incIndex( index );
		}

		@Override
		public void setPosition( final Localizable localizable )
		{
			localizable.localize( position );
			int index = 0;
			for ( int d = 0; d < n; ++d )
				index += ( int ) ( position[ d ] - min[ d ] ) * steps[ d ];
			type.updateIndex( index );
		}

		@Override
		public void setPosition( final int[] pos )
		{
			int index = 0;
			for ( int d = 0; d < n; ++d )
			{
				position[ d ] = pos[ d ];
				index += ( int ) ( pos[ d ] - min[ d ] ) * steps[ d ];
			}
			type.updateIndex( index );
		}

		@Override
		public void setPosition( final long[] pos )
		{
			int index = 0;
			for ( int d = 0; d < n; ++d )
			{
				position[ d ] = pos[ d ];
				index += ( int ) ( pos[ d ] - min[ d ] ) * steps[ d ];
			}
			type.updateIndex( index );
		}

		@Override
		public void setPosition( final int pos, final int d )
		{
			type.incIndex( ( int ) ( pos - position[ d ] ) * steps[ d ] );
			position[ d ] = pos;
		}

		@Override
		public void setPosition( final long pos, final int d )
		{
			type.incIndex( ( int ) ( pos - position[ d ] ) * steps[ d ] );
			position[ d ] = pos;
		}

		@Override
		public CellArrayRandomAccess copy()
		{
			return new CellArrayRandomAccess( this );
		}

		@Override
		public CellArrayRandomAccess copyRandomAccess()
		{
			return copy();
		}
	}

	@Override
	public Cursor< T > cursor()
	{
		return new CellArrayCursor();
	}

	class CellArrayCursor extends AbstractCursor< T >
	{
		final T type;

		final int lastIndex;

		CellArrayCursor()
		{
			super( SingleCellArrayImg.this.n );
			type = createLinkedType();
			lastIndex = ( int ) SingleCellArrayImg.this.size() - 1;
			reset();
		}

		CellArrayCursor(final CellArrayCursor other)
		{
			super( SingleCellArrayImg.this.n );
			type = createLinkedType();
			lastIndex = other.lastIndex;
			type.updateIndex( other.type.getIndex() );
			type.updateContainer( this );
		}

		@Override
		public T get()
		{
			return type;
		}

		@Override
		public boolean hasNext()
		{
			return type.getIndex() < lastIndex;
		}

		@Override
		public void jumpFwd( final long steps )
		{
			type.incIndex( ( int ) steps );
		}

		@Override
		public T next()
		{
			fwd();
			return get();
		}

		@Override
		public void fwd()
		{
			type.incIndex();
		}

		@Override
		public void reset()
		{
			type.updateIndex( -1 );
			type.updateContainer( this );
		}

		@Override
		public void localize( final long[] position )
		{
			IntervalIndexer.indexToPositionWithOffset( type.getIndex(), dimensions, min, position );
		}

		@Override
		public long getLongPosition( final int d )
		{
			return IntervalIndexer.indexToPositionWithOffset( type.getIndex(), dimensions, steps, min, d );
		}

		@Override
		public CellArrayCursor copy()
		{
			return new CellArrayCursor( this );
		}

		@Override
		public CellArrayCursor copyCursor()
		{
			return copy();
		}
	}

	@Override
	public Cursor< T > localizingCursor()
	{
		return new CellArrayLocalizingCursor();
	}

	class CellArrayLocalizingCursor extends AbstractLocalizingCursor< T >
	{
		final T type;

		final int lastIndex;

		CellArrayLocalizingCursor()
		{
			super( SingleCellArrayImg.this.n );
			type = createLinkedType();
			lastIndex = ( int ) SingleCellArrayImg.this.size() - 1;
			reset();
		}

		CellArrayLocalizingCursor( final CellArrayLocalizingCursor other )
		{
			super( other.n );
			type = createLinkedType();
			lastIndex = other.lastIndex;
			System.arraycopy( other.position, 0, position, 0, n );
			type.updateIndex( other.type.getIndex() );
			type.updateContainer( this );
		}

		@Override
		public T get()
		{
			return type;
		}

		@Override
		public void fwd()
		{
			type.incIndex();
			for ( int d = 0; d < n && ++position[ d ] > max[ d ]; ++d )
				position[ d ] = min[ d ];
		}

		@Override
		public void jumpFwd( final long steps )
		{
			type.incIndex( ( int ) steps );
			IntervalIndexer.indexToPositionWithOffset( type.getIndex(), dimensions, min, position );
		}

		@Override
		public T next()
		{
			fwd();
			return get();
		}

		@Override
		public boolean hasNext()
		{
			return type.getIndex() < lastIndex;
		}

		@Override
		public void reset()
		{
			type.updateIndex( -1 );
			type.updateContainer( this );

			System.arraycopy( min, 0, position, 0, n );
			position[ 0 ]--;
		}

		@Override
		public CellArrayLocalizingCursor copy()
		{
			return new CellArrayLocalizingCursor( this );
		}

		@Override
		public CellArrayLocalizingCursor copyCursor()
		{
			return copy();
		}
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
	public long size()
	{
		return size;
	}

	@Override
	public T firstElement()
	{
		return cursor().next();
	}

	@Override
	public Object iterationOrder()
	{
		return new FlatIterationOrder( this );
	}

	@Override
	public Iterator< T > iterator()
	{
		return cursor();
	}
}
