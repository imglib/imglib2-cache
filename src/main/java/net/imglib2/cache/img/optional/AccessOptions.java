package net.imglib2.cache.img.optional;

import net.imglib2.Dirty;
import org.scijava.optional.AbstractValues.ValuesToString;
import org.scijava.optional.Options;
import org.scijava.optional.Values;

/**
 * Optional arguments that specify whether accesses should be
 * <ul>
 *     <li>{@link #dirtyAccesses(boolean) dirty} (default {@code false})</li>
 *     <li>{@link #volatileAccesses(boolean) volatile} (default {@code true})</li>
 * </ul>
 */
public interface AccessOptions< T extends AccessOptions< T > > extends Options< T >
{
	/**
	 * Specify whether the image should use {@link Dirty} accesses. Dirty
	 * accesses track whether cells were written to.
	 * <p>
	 * This is {@code false} by default.
	 * </p>
	 *
	 * @param dirty
	 * 		whether the image should use {@link Dirty} accesses.
	 */
	default T dirtyAccesses( final boolean dirty )
	{
		return add( "dirtyAccesses", dirty );
	}

	default T volatileAccesses( final boolean volatil )
	{
		return add( "volatileAccesses", volatil );
	}

	interface Val extends Values
	{
		default void buildToString( ValuesToString sb )
		{
			sb.append( "dirtyAccesses", dirtyAccesses() );
			sb.append( "volatileAccesses", volatileAccesses() );
		}

		default boolean dirtyAccesses()
		{
			return value( "dirtyAccesses", false );
		}

		default boolean volatileAccesses()
		{
			return value( "volatileAccesses", true );
		}
	}
}
