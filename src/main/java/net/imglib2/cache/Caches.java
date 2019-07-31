/**
 *
 */
package net.imglib2.cache;

import static net.imglib2.cache.img.DiskCachedCellImgOptions.CacheType.SOFTREF;
import static net.imglib2.cache.img.ReadOnlyCachedCellImgOptions.options;
import static net.imglib2.img.basictypeaccess.AccessFlags.DIRTY;
import static net.imglib2.img.basictypeaccess.AccessFlags.VOLATILE;
import static net.imglib2.type.PrimitiveType.BYTE;
import static net.imglib2.type.PrimitiveType.DOUBLE;
import static net.imglib2.type.PrimitiveType.FLOAT;
import static net.imglib2.type.PrimitiveType.INT;
import static net.imglib2.type.PrimitiveType.LONG;
import static net.imglib2.type.PrimitiveType.SHORT;

import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.cache.img.LoadedCellCacheLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
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
/*
	TODO rename or otherwise resolve clash with net.imglib2.cache.util.Caches
		Caching?
 */
public class Caches
{
	private Caches()
	{}

	@SuppressWarnings( "rawtypes" )
	public static < T extends NativeType< T > > RandomAccessibleInterval< T > cache(
			final RandomAccessibleInterval< T > source,
			final int[] blockSize,
			final Set< AccessFlags > accessFlags )
	{
		final long[] dimensions = Intervals.dimensionsAsLongArray( source );
		final T type = Util.getTypeFromInterval( source );
		final RandomAccessibleLoader< T > loader = new RandomAccessibleLoader< T >( Views.zeroMin( source ) );
		final CachedCellImg< T, ? > img = new ReadOnlyCachedCellImgFactory().create( dimensions, type, loader, options()
//				.cacheType( SOFTREF )
				.cellDimensions( blockSize )
				.dirtyAccesses( accessFlags.contains( DIRTY ) )
				.volatileAccesses( accessFlags.contains( VOLATILE ) ) );
		return Views.isZeroMin( source ) ? img : Views.translate( img, Intervals.minAsLongArray( source ) );
	}

	/**
	 * Create a memory {@link CachedCellImg} with a cell {@link Cache}.
	 *
	 * @param grid
	 * @param cache
	 * @param type
	 * @param accessFlags
	 * @return
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static < T extends NativeType< T > > CachedCellImg< T, ? > createImg(
			final CellGrid grid,
			final Cache< Long, Cell< ? > > cache,
			final T type,
			final Set< AccessFlags > accessFlags )
	{
		return new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( type, accessFlags ) );
	}

	/**
	 * Create a memory {@link CachedCellImg} with a {@link CellLoader}.
	 *
	 * @param targetInterval
	 * @param blockSize
	 * @param type
	 * @param accessFlags
	 * @param loader
	 * @return
	 */
	public static < T extends NativeType< T > > CachedCellImg< T, ? > createImg(
			final Interval targetInterval,
			final int[] blockSize,
			final T type,
			final Set< AccessFlags > accessFlags,
			final CellLoader< T > loader )
	{
		final long[] dimensions = Intervals.dimensionsAsLongArray( targetInterval );
		final CachedCellImg< T, ? > img = new ReadOnlyCachedCellImgFactory().create( dimensions, type, loader, options()
//				.cacheType( SOFTREF )
				.cellDimensions( blockSize )
				.dirtyAccesses( accessFlags.contains( DIRTY ) )
				.volatileAccesses( accessFlags.contains( VOLATILE ) ) );
		return img;
	}

	/**
	 * Create a memory {@link CachedCellImg} with a cell generator
	 * {@link Consumer}.
	 *
	 * @param targetInterval
	 * @param blockSize
	 * @param type
	 * @param accessFlags
	 * @param op
	 * @return
	 */
	public static < T extends NativeType< T > > CachedCellImg< T, ? > process(
			final Interval targetInterval,
			final int[] blockSize,
			final T type,
			final Set< AccessFlags > accessFlags,
			final Consumer< RandomAccessibleInterval< T > > op )
	{
		return createImg(
				targetInterval,
				blockSize,
				type,
				accessFlags,
				op::accept );
	}
}
