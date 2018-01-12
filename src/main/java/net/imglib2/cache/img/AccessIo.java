package net.imglib2.cache.img;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.basictypeaccess.array.AbstractByteArray;
import net.imglib2.img.basictypeaccess.array.AbstractCharArray;
import net.imglib2.img.basictypeaccess.array.AbstractDoubleArray;
import net.imglib2.img.basictypeaccess.array.AbstractFloatArray;
import net.imglib2.img.basictypeaccess.array.AbstractIntArray;
import net.imglib2.img.basictypeaccess.array.AbstractLongArray;
import net.imglib2.img.basictypeaccess.array.AbstractShortArray;
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
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;

/**
 * Serialize/deserialize an access to/from a {@link ByteBuffer}.
 * <p>
 * All implementations for primitive {@link ArrayDataAccess} types as well as
 * volatile and dirty variants are implemented as static inner classes.
 * </p>
 * <p>
 * Use {@link #get(PrimitiveType, AccessFlags...)} to obtain the correct
 * {@link AccessIo} implementation for a given {@link PrimitiveType},
 * {@link AccessFlags} combination.
 * </p>
 *
 * @param <A>
 *            the access type
 *
 * @author Tobias Pietzsch
 */
public interface AccessIo< A >
{
	public A load( final ByteBuffer bytes, final int numElements );

	public int getBytesPerElement();

	public void save( final A access, final ByteBuffer out, final int numElements );

	/*
	 * Implementations, singleton instances, and get() methods
	 */

	public static < T extends NativeType< T >, A > AccessIo< A > get( final T type, final AccessFlags ... flags )
	{
		return get( type.getPrimitiveTypeInfo().getPrimitiveType(), flags );
	}

	@SuppressWarnings( "unchecked" )
	public static < A > AccessIo< A > get( final PrimitiveType primitiveType, final AccessFlags ... flags )
	{
		final boolean dirty = AccessFlags.isDirty( flags );
		final boolean volatil = AccessFlags.isVolatile( flags );
		switch ( primitiveType )
		{
		case BYTE:
			return dirty
					? ( volatil
							? ( AccessIo< A > ) dirtyVolatileByteArrayIo
							: ( AccessIo< A > ) dirtyByteArrayIo )
					: ( volatil
							? ( AccessIo< A > ) volatileByteArrayIo
							: ( AccessIo< A > ) byteArrayIo );
		case CHAR:
			return dirty
					? ( volatil
							? ( AccessIo< A > ) dirtyVolatileCharArrayIo
							: ( AccessIo< A > ) dirtyCharArrayIo )
					: ( volatil
							? ( AccessIo< A > ) volatileCharArrayIo
							: ( AccessIo< A > ) charArrayIo );
		case DOUBLE:
			return dirty
					? ( volatil
							? ( AccessIo< A > ) dirtyVolatileDoubleArrayIo
							: ( AccessIo< A > ) dirtyDoubleArrayIo )
					: ( volatil
							? ( AccessIo< A > ) volatileDoubleArrayIo
							: ( AccessIo< A > ) doubleArrayIo );
		case FLOAT:
			return dirty
					? ( volatil
							? ( AccessIo< A > ) dirtyVolatileFloatArrayIo
							: ( AccessIo< A > ) dirtyFloatArrayIo )
					: ( volatil
							? ( AccessIo< A > ) volatileFloatArrayIo
							: ( AccessIo< A > ) floatArrayIo );
		case INT:
			return dirty
					? ( volatil
							? ( AccessIo< A > ) dirtyVolatileIntArrayIo
							: ( AccessIo< A > ) dirtyIntArrayIo )
					: ( volatil
							? ( AccessIo< A > ) volatileIntArrayIo
							: ( AccessIo< A > ) intArrayIo );
		case LONG:
			return dirty
					? ( volatil
							? ( AccessIo< A > ) dirtyVolatileLongArrayIo
							: ( AccessIo< A > ) dirtyLongArrayIo )
					: ( volatil
							? ( AccessIo< A > ) volatileLongArrayIo
							: ( AccessIo< A > ) longArrayIo );
		case SHORT:
			return dirty
					? ( volatil
							? ( AccessIo< A > ) dirtyVolatileShortArrayIo
							: ( AccessIo< A > ) dirtyShortArrayIo )
					: ( volatil
							? ( AccessIo< A > ) volatileShortArrayIo
							: ( AccessIo< A > ) shortArrayIo );
		default:
			return null;
		}
	}

	/*
	 * double
	 */

	static final DoubleArrayType doubleArrayIo = new DoubleArrayType();

	static final VolatileDoubleArrayType volatileDoubleArrayIo = new VolatileDoubleArrayType();

	static final DirtyDoubleArrayType dirtyDoubleArrayIo = new DirtyDoubleArrayType();

	static final DirtyVolatileDoubleArrayType dirtyVolatileDoubleArrayIo = new DirtyVolatileDoubleArrayType();

	static abstract class AbstractDoubleArrayIo< A extends AbstractDoubleArray< A > > implements AccessIo< A >
	{
		@Override
		public int getBytesPerElement()
		{
			return 8;
		}

		protected double[] loadData( final ByteBuffer bytes, final int numElements )
		{
			final double[] data = new double[ numElements ];
			DoubleBuffer.wrap( data, 0, numElements ).put( bytes.asDoubleBuffer() );
			return data;
		}

		@Override
		public void save( final A access, final ByteBuffer out, final int numElements )
		{
			final double[] data = access.getCurrentStorageArray();
			out.asDoubleBuffer().put( DoubleBuffer.wrap( data, 0, numElements ) );
		}
	}

	static class DoubleArrayType extends AbstractDoubleArrayIo< DoubleArray >
	{
		@Override
		public DoubleArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DoubleArray( loadData( bytes, numElements ) );
		}
	}

	static class VolatileDoubleArrayType extends AbstractDoubleArrayIo< VolatileDoubleArray >
	{
		@Override
		public VolatileDoubleArray load( final ByteBuffer bytes, final int numElements )
		{
			return new VolatileDoubleArray( loadData( bytes, numElements ), true );
		}
	}

	static class DirtyDoubleArrayType extends AbstractDoubleArrayIo< DirtyDoubleArray >
	{
		@Override
		public DirtyDoubleArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DirtyDoubleArray( loadData( bytes, numElements ) );
		}
	}

	static class DirtyVolatileDoubleArrayType extends AbstractDoubleArrayIo< DirtyVolatileDoubleArray >
	{
		@Override
		public DirtyVolatileDoubleArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DirtyVolatileDoubleArray( loadData( bytes, numElements ), true );
		}
	}

	/*
	 * float
	 */

	static final FloatArrayType floatArrayIo = new FloatArrayType();

	static final VolatileFloatArrayType volatileFloatArrayIo = new VolatileFloatArrayType();

	static final DirtyFloatArrayType dirtyFloatArrayIo = new DirtyFloatArrayType();

	static final DirtyVolatileFloatArrayType dirtyVolatileFloatArrayIo = new DirtyVolatileFloatArrayType();

	static abstract class AbstractFloatArrayIo< A extends AbstractFloatArray< A > > implements AccessIo< A >
	{
		@Override
		public int getBytesPerElement()
		{
			return 4;
		}

		protected float[] loadData(	final ByteBuffer bytes, final int numElements )
		{
			final float[] data = new float[ numElements ];
			FloatBuffer.wrap( data, 0, numElements ).put( bytes.asFloatBuffer() );
			return data;
		}

		@Override
		public void save( final A access, final ByteBuffer out, final int numElements )
		{
			final float[] data = access.getCurrentStorageArray();
			out.asFloatBuffer().put( FloatBuffer.wrap( data, 0, numElements ) );
		}
	}

	static class FloatArrayType extends AbstractFloatArrayIo< FloatArray >
	{
		@Override
		public FloatArray load( final ByteBuffer bytes, final int numElements )
		{
			return new FloatArray( loadData( bytes, numElements ) );
		}
	}

	static class VolatileFloatArrayType extends AbstractFloatArrayIo< VolatileFloatArray >
	{
		@Override
		public VolatileFloatArray load( final ByteBuffer bytes, final int numElements )
		{
			return new VolatileFloatArray( loadData( bytes, numElements ), true );
		}
	}

	static class DirtyFloatArrayType extends AbstractFloatArrayIo< DirtyFloatArray >
	{
		@Override
		public DirtyFloatArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DirtyFloatArray( loadData( bytes, numElements ) );
		}
	}

	static class DirtyVolatileFloatArrayType extends AbstractFloatArrayIo< DirtyVolatileFloatArray >
	{
		@Override
		public DirtyVolatileFloatArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DirtyVolatileFloatArray( loadData( bytes, numElements ), true );
		}
	}

	/*
	 * int
	 */

	static final IntArrayType intArrayIo = new IntArrayType();

	static final VolatileIntArrayType volatileIntArrayIo = new VolatileIntArrayType();

	static final DirtyIntArrayType dirtyIntArrayIo = new DirtyIntArrayType();

	static final DirtyVolatileIntArrayType dirtyVolatileIntArrayIo = new DirtyVolatileIntArrayType();

	static abstract class AbstractIntArrayIo< A extends AbstractIntArray< A > > implements AccessIo< A >
	{
		@Override
		public int getBytesPerElement()
		{
			return 4;
		}

		protected int[] loadData( final ByteBuffer bytes, final int numElements )
		{
			final int[] data = new int[ numElements ];
			IntBuffer.wrap( data, 0, numElements ).put( bytes.asIntBuffer() );
			return data;
		}

		@Override
		public void save( final A access, final ByteBuffer out, final int numElements )
		{
			final int[] data = access.getCurrentStorageArray();
			out.asIntBuffer().put( IntBuffer.wrap( data, 0, numElements ) );
		}
	}

	static class IntArrayType extends AbstractIntArrayIo< IntArray >
	{
		@Override
		public IntArray load( final ByteBuffer bytes, final int numElements )
		{
			return new IntArray( loadData( bytes, numElements ) );
		}
	}

	static class VolatileIntArrayType extends AbstractIntArrayIo< VolatileIntArray >
	{
		@Override
		public VolatileIntArray load( final ByteBuffer bytes, final int numElements )
		{
			return new VolatileIntArray( loadData( bytes, numElements ), true );
		}
	}

	static class DirtyIntArrayType extends AbstractIntArrayIo< DirtyIntArray >
	{
		@Override
		public DirtyIntArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DirtyIntArray( loadData( bytes, numElements ) );
		}
	}

	static class DirtyVolatileIntArrayType extends AbstractIntArrayIo< DirtyVolatileIntArray >
	{
		@Override
		public DirtyVolatileIntArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DirtyVolatileIntArray( loadData( bytes, numElements ), true );
		}
	}

	/*
	 * long
	 */

	static final LongArrayType longArrayIo = new LongArrayType();

	static final VolatileLongArrayType volatileLongArrayIo = new VolatileLongArrayType();

	static final DirtyLongArrayType dirtyLongArrayIo = new DirtyLongArrayType();

	static final DirtyVolatileLongArrayType dirtyVolatileLongArrayIo = new DirtyVolatileLongArrayType();

	static abstract class AbstractLongArrayIo< A extends AbstractLongArray< A > > implements AccessIo< A >
	{
		@Override
		public int getBytesPerElement()
		{
			return 8;
		}

		protected long[] loadData( final ByteBuffer bytes, final int numElements )
		{
			final long[] data = new long[ numElements ];
			LongBuffer.wrap( data, 0, numElements ).put( bytes.asLongBuffer() );
			return data;
		}

		@Override
		public void save( final A access, final ByteBuffer out, final int numElements )
		{
			final long[] data = access.getCurrentStorageArray();
			out.asLongBuffer().put( LongBuffer.wrap( data, 0, numElements ) );
		}
	}

	static class LongArrayType extends AbstractLongArrayIo< LongArray >
	{
		@Override
		public LongArray load( final ByteBuffer bytes, final int numElements )
		{
			return new LongArray( loadData( bytes, numElements ) );
		}
	}

	static class VolatileLongArrayType extends AbstractLongArrayIo< VolatileLongArray >
	{
		@Override
		public VolatileLongArray load( final ByteBuffer bytes, final int numElements )
		{
			return new VolatileLongArray( loadData( bytes, numElements ), true );
		}
	}

	static class DirtyLongArrayType extends AbstractLongArrayIo< DirtyLongArray >
	{
		@Override
		public DirtyLongArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DirtyLongArray( loadData( bytes, numElements ) );
		}
	}

	static class DirtyVolatileLongArrayType extends AbstractLongArrayIo< DirtyVolatileLongArray >
	{
		@Override
		public DirtyVolatileLongArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DirtyVolatileLongArray( loadData( bytes, numElements ), true );
		}
	}

	/*
	 * short
	 */

	static final ShortArrayType shortArrayIo = new ShortArrayType();

	static final VolatileShortArrayType volatileShortArrayIo = new VolatileShortArrayType();

	static final DirtyShortArrayType dirtyShortArrayIo = new DirtyShortArrayType();

	static final DirtyVolatileShortArrayType dirtyVolatileShortArrayIo = new DirtyVolatileShortArrayType();

	static abstract class AbstractShortArrayIo< A extends AbstractShortArray< A > > implements AccessIo< A >
	{
		@Override
		public int getBytesPerElement()
		{
			return 2;
		}

		protected short[] loadData( final ByteBuffer bytes, final int numElements )
		{
			final short[] data = new short[ numElements ];
			ShortBuffer.wrap( data, 0, numElements ).put( bytes.asShortBuffer() );
			return data;
		}

		@Override
		public void save( final A access, final ByteBuffer out, final int numElements )
		{
			final short[] data = access.getCurrentStorageArray();
			out.asShortBuffer().put( ShortBuffer.wrap( data, 0, numElements ) );
		}
	}

	static class ShortArrayType extends AbstractShortArrayIo< ShortArray >
	{
		@Override
		public ShortArray load( final ByteBuffer bytes, final int numElements )
		{
			return new ShortArray( loadData( bytes, numElements ) );
		}
	}

	static class VolatileShortArrayType extends AbstractShortArrayIo< VolatileShortArray >
	{
		@Override
		public VolatileShortArray load( final ByteBuffer bytes, final int numElements )
		{
			return new VolatileShortArray( loadData( bytes, numElements ), true );
		}
	}

	static class DirtyShortArrayType extends AbstractShortArrayIo< DirtyShortArray >
	{
		@Override
		public DirtyShortArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DirtyShortArray( loadData( bytes, numElements ) );
		}
	}

	static class DirtyVolatileShortArrayType extends AbstractShortArrayIo< DirtyVolatileShortArray >
	{
		@Override
		public DirtyVolatileShortArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DirtyVolatileShortArray( loadData( bytes, numElements ), true );
		}
	}

	/*
	 * char
	 */

	static final CharArrayType charArrayIo = new CharArrayType();

	static final VolatileCharArrayType volatileCharArrayIo = new VolatileCharArrayType();

	static final DirtyCharArrayType dirtyCharArrayIo = new DirtyCharArrayType();

	static final DirtyVolatileCharArrayType dirtyVolatileCharArrayIo = new DirtyVolatileCharArrayType();

	static abstract class AbstractCharArrayIo< A extends AbstractCharArray< A > > implements AccessIo< A >
	{
		@Override
		public int getBytesPerElement()
		{
			return 1;
		}

		protected char[] loadData( final ByteBuffer bytes, final int numElements )
		{
			final char[] data = new char[ numElements ];
			CharBuffer.wrap( data, 0, numElements ).put( bytes.asCharBuffer() );
			return data;
		}

		@Override
		public void save( final A access, final ByteBuffer out, final int numElements )
		{
			final char[] data = access.getCurrentStorageArray();
			out.asCharBuffer().put( CharBuffer.wrap( data, 0, numElements ) );
		}
	}

	static class CharArrayType extends AbstractCharArrayIo< CharArray >
	{
		@Override
		public CharArray load( final ByteBuffer bytes, final int numElements )
		{
			return new CharArray( loadData( bytes, numElements ) );
		}
	}

	static class VolatileCharArrayType extends AbstractCharArrayIo< VolatileCharArray >
	{
		@Override
		public VolatileCharArray load( final ByteBuffer bytes, final int numElements )
		{
			return new VolatileCharArray( loadData( bytes, numElements ), true );
		}
	}

	static class DirtyCharArrayType extends AbstractCharArrayIo< DirtyCharArray >
	{
		@Override
		public DirtyCharArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DirtyCharArray( loadData( bytes, numElements ) );
		}
	}

	static class DirtyVolatileCharArrayType extends AbstractCharArrayIo< DirtyVolatileCharArray >
	{
		@Override
		public DirtyVolatileCharArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DirtyVolatileCharArray( loadData( bytes, numElements ), true );
		}
	}

	/*
	 * byte
	 */

	static final ByteArrayType byteArrayIo = new ByteArrayType();

	static final VolatileByteArrayType volatileByteArrayIo = new VolatileByteArrayType();

	static final DirtyByteArrayType dirtyByteArrayIo = new DirtyByteArrayType();

	static final DirtyVolatileByteArrayType dirtyVolatileByteArrayIo = new DirtyVolatileByteArrayType();

	static abstract class AbstractByteArrayIo< A extends AbstractByteArray< A > > implements AccessIo< A >
	{
		@Override
		public int getBytesPerElement()
		{
			return 1;
		}

		protected byte[] loadData( final ByteBuffer bytes, final int numElements )
		{
			final byte[] data = new byte[ numElements ];
			ByteBuffer.wrap( data, 0, numElements ).put( bytes );
			return data;
		}

		@Override
		public void save( final A access, final ByteBuffer out, final int numElements )
		{
			final byte[] data = access.getCurrentStorageArray();
			out.put( ByteBuffer.wrap( data, 0, numElements ) );
		}
	}

	static class ByteArrayType extends AbstractByteArrayIo< ByteArray >
	{
		@Override
		public ByteArray load( final ByteBuffer bytes, final int numElements )
		{
			return new ByteArray( loadData( bytes, numElements ) );
		}
	}

	static class VolatileByteArrayType extends AbstractByteArrayIo< VolatileByteArray >
	{
		@Override
		public VolatileByteArray load( final ByteBuffer bytes, final int numElements )
		{
			return new VolatileByteArray( loadData( bytes, numElements ), true );
		}
	}

	static class DirtyByteArrayType extends AbstractByteArrayIo< DirtyByteArray >
	{
		@Override
		public DirtyByteArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DirtyByteArray( loadData( bytes, numElements ) );
		}
	}

	static class DirtyVolatileByteArrayType extends AbstractByteArrayIo< DirtyVolatileByteArray >
	{
		@Override
		public DirtyVolatileByteArray load( final ByteBuffer bytes, final int numElements )
		{
			return new DirtyVolatileByteArray( loadData( bytes, numElements ), true );
		}
	}
}