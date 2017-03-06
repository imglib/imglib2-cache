package net.imglib2.cache.img;

import java.util.concurrent.atomic.AtomicReference;

import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.NativeImg;
import net.imglib2.img.NativeImgFactory;
import net.imglib2.img.basictypeaccess.ByteAccess;
import net.imglib2.img.basictypeaccess.CharAccess;
import net.imglib2.img.basictypeaccess.DoubleAccess;
import net.imglib2.img.basictypeaccess.FloatAccess;
import net.imglib2.img.basictypeaccess.IntAccess;
import net.imglib2.img.basictypeaccess.LongAccess;
import net.imglib2.img.basictypeaccess.ShortAccess;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileByteArray;
import net.imglib2.type.NativeType;
import net.imglib2.util.Fraction;

/**
 * Enumeration of Java primitive types which can back {@link NativeType}s. In
 * conjunction with {@link AccessFlags} this describes a specific
 * {@link ArrayDataAccess}. For example, {@code BYTE} with flags {@code DIRTY}
 * and {@code VOLATILE} specifies {@link DirtyVolatileByteArray}.
 *
 * @author Tobias Pietzsch
 */
public enum PrimitiveType
{
	BYTE,
	CHAR,
	SHORT,
	INT,
	LONG,
	FLOAT,
	DOUBLE;

	/**
	 * Get the {@link PrimitiveType} that backs the given type.
	 *
	 * @param type
	 *            a {@link NativeType}
	 * @return the {@link PrimitiveType} that backs the given type
	 */
	public static < T extends NativeType< T > > PrimitiveType forNativeType( final T type )
	{
		final AtomicReference< PrimitiveType > primitiveType = new AtomicReference<>();
		try
		{
			type.createSuitableNativeImg( new NativeImgFactory< T >()
			{
				@Override
				public NativeImg< T, ? extends ByteAccess > createByteInstance( final long[] dimensions, final Fraction entitiesPerPixel )
				{
					primitiveType.set( BYTE );
					throw new UnsupportedOperationException();
				}

				@Override
				public NativeImg< T, ? extends CharAccess > createCharInstance( final long[] dimensions, final Fraction entitiesPerPixel )
				{
					primitiveType.set( CHAR );
					throw new UnsupportedOperationException();
				}

				@Override
				public NativeImg< T, ? extends ShortAccess > createShortInstance( final long[] dimensions, final Fraction entitiesPerPixel )
				{
					primitiveType.set( SHORT );
					throw new UnsupportedOperationException();
				}

				@Override
				public NativeImg< T, ? extends IntAccess > createIntInstance( final long[] dimensions, final Fraction entitiesPerPixel )
				{
					primitiveType.set( INT );
					throw new UnsupportedOperationException();
				}

				@Override
				public NativeImg< T, ? extends LongAccess > createLongInstance( final long[] dimensions, final Fraction entitiesPerPixel )
				{
					primitiveType.set( LONG );
					throw new UnsupportedOperationException();
				}

				@Override
				public NativeImg< T, ? extends FloatAccess > createFloatInstance( final long[] dimensions, final Fraction entitiesPerPixel )
				{
					primitiveType.set( FLOAT );
					throw new UnsupportedOperationException();
				}

				@Override
				public NativeImg< T, ? extends DoubleAccess > createDoubleInstance( final long[] dimensions, final Fraction entitiesPerPixel )
				{
					primitiveType.set( DOUBLE );
					throw new UnsupportedOperationException();
				}

				@Override
				public < S > ImgFactory< S > imgFactory( final S type ) throws IncompatibleTypeException
				{
					throw new UnsupportedOperationException();
				}
			}, null );
		}
		catch ( final Exception e )
		{}
		return primitiveType.get();
	}
}