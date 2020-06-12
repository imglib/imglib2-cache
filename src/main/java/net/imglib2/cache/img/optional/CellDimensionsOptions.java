package net.imglib2.cache.img.optional;

import java.util.function.BiConsumer;
import net.imglib2.Dimensions;
import org.scijava.optional.Options;
import org.scijava.optional.Values;

/**
 * Optional arguments that specify the dimensions of a Cell.
 */
public interface CellDimensionsOptions< T > extends Options< T >
{
	/**
	 * Set the dimensions of a cell. This is extended or truncated as necessary.
	 * For example if {@code cellDimensions=[64,32]} then for creating a 3D
	 * image it will be augmented to {@code [64,32,32]}. For creating a 1D image
	 * it will be truncated to {@code [64]}.
	 *
	 * @param cellDimensions
	 *            dimensions of a cell (default is 10).
	 */
	default T cellDimensions( final int... cellDimensions )
	{
		Dimensions.verify( cellDimensions );
		return setValue( "cellDimensions", cellDimensions );
	}

	interface Val extends Values
	{
		default void forEach( BiConsumer< String, Object > action )
		{
			action.accept( "cellDimensions", cellDimensions() );
		}

		default int[] cellDimensions()
		{
			return getValueOrDefault( "cellDimensions", new int[] { 10 } );
		}
	}
}
