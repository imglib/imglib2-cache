/**
 *
 */
package net.imglib2.cache;

import static net.imglib2.type.PrimitiveType.BYTE;
import static net.imglib2.type.PrimitiveType.DOUBLE;
import static net.imglib2.type.PrimitiveType.FLOAT;
import static net.imglib2.type.PrimitiveType.INT;
import static net.imglib2.type.PrimitiveType.LONG;
import static net.imglib2.type.PrimitiveType.SHORT;

import java.io.IOException;
import java.util.Set;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.LoadedCellCacheLoader;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * Convenience methods to cache existing {@link RandomAccessibleInterval}s
 *
 * @author Stephan Saalfeld
 */
public class Caches
{
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static final < T extends NativeType< T > > RandomAccessibleInterval< T > cache(
			final RandomAccessibleInterval< T > source,
			final int[] blockSize,
			final Set< AccessFlags > accessFlags ) throws IOException
	{
		final long[] dimensions = Intervals.dimensionsAsLongArray( source );
		final CellGrid grid = new CellGrid( dimensions, blockSize );

		final RandomAccessibleLoader< T > loader = new RandomAccessibleLoader< T >( Views.zeroMin( source ) );

		final T type = Util.getTypeFromInterval( source );

		final CachedCellImg< T, ? > img;
		final Cache< Long, Cell< ? > > cache = new SoftRefLoaderCache()
				.withLoader( LoadedCellCacheLoader.get( grid, loader, type, accessFlags ) );

		if ( GenericByteType.class.isInstance( type ) )
		{
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( BYTE, accessFlags ) );
		}
		else if ( GenericShortType.class.isInstance( type ) )
		{
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( SHORT, accessFlags ) );
		}
		else if ( GenericIntType.class.isInstance( type ) )
		{
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( INT, accessFlags ) );
		}
		else if ( GenericLongType.class.isInstance( type ) )
		{
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( LONG, accessFlags ) );
		}
		else if ( FloatType.class.isInstance( type ) )
		{
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( FLOAT, accessFlags ) );
		}
		else if ( DoubleType.class.isInstance( type ) )
		{
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( DOUBLE, accessFlags ) );
		}
		else
		{
			img = null;
		}

		return img;
	}
}
