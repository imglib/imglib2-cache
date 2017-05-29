/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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

import static net.imglib2.cache.img.PrimitiveType.BYTE;
import static net.imglib2.cache.img.PrimitiveType.CHAR;
import static net.imglib2.cache.img.PrimitiveType.DOUBLE;
import static net.imglib2.cache.img.PrimitiveType.FLOAT;
import static net.imglib2.cache.img.PrimitiveType.INT;
import static net.imglib2.cache.img.PrimitiveType.LONG;
import static net.imglib2.cache.img.PrimitiveType.SHORT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.IoSync;
import net.imglib2.cache.LoaderRemoverCache;
import net.imglib2.cache.ref.GuardedStrongRefLoaderRemoverCache;
import net.imglib2.cache.ref.SoftRefLoaderRemoverCache;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.NativeImgFactory;
import net.imglib2.img.basictypeaccess.ByteAccess;
import net.imglib2.img.basictypeaccess.CharAccess;
import net.imglib2.img.basictypeaccess.DoubleAccess;
import net.imglib2.img.basictypeaccess.FloatAccess;
import net.imglib2.img.basictypeaccess.IntAccess;
import net.imglib2.img.basictypeaccess.LongAccess;
import net.imglib2.img.basictypeaccess.ShortAccess;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.util.Fraction;

/**
 * Factory for creating {@link DiskCachedCellImg}s. See
 * {@link DiskCachedCellImgOptions} for available configuration options and
 * defaults.
 *
 * @author Tobias Pietzsch
 */
public class DiskCachedCellImgFactory< T extends NativeType< T > > extends NativeImgFactory< T >
{
	private DiskCachedCellImgOptions factoryOptions;

	/**
	 * Create a new {@link DiskCachedCellImgFactory} with default configuration.
	 */
	public DiskCachedCellImgFactory()
	{
		this( DiskCachedCellImgOptions.options() );
	}

	/**
	 * Create a new {@link DiskCachedCellImgFactory} with the specified
	 * configuration.
	 *
	 * @param optional
	 *            configuration options.
	 */
	public DiskCachedCellImgFactory( final DiskCachedCellImgOptions optional )
	{
		this.factoryOptions = optional;
	}

	@Override
	public DiskCachedCellImg< T, ? > create( final long[] dim, final T type )
	{
		return createInternal( dim, null, null, type, null );
	}

	public DiskCachedCellImg< T, ? > create( final long[] dim, final T type, final DiskCachedCellImgOptions additionalOptions )
	{
		return createInternal( dim, null, null, type, additionalOptions );
	}

	public DiskCachedCellImg< T, ? > create( final long[] dim, final T type, final CellLoader< T > loader )
	{
		return createInternal( dim, null, loader, type, null );
	}

	public DiskCachedCellImg< T, ? > create( final long[] dim, final T type, final CellLoader< T > loader, final DiskCachedCellImgOptions additionalOptions )
	{
		return createInternal( dim, null, loader, type, additionalOptions );
	}

	@SuppressWarnings( "unchecked" )
	public < A > DiskCachedCellImg< T, A > createWithCacheLoader( final long[] dim, final T type, final CacheLoader< Long, Cell< A > > backingLoader )
	{
		return ( DiskCachedCellImg< T, A > ) createInternal( dim, backingLoader, null, type, null );
	}

	@SuppressWarnings( "unchecked" )
	public < A > DiskCachedCellImg< T, A > createWithCacheLoader( final long[] dim, final T type, final CacheLoader< Long, Cell< A > > backingLoader, final DiskCachedCellImgOptions additionalOptions )
	{
		return ( DiskCachedCellImg< T, A > ) createInternal( dim, backingLoader, null, type, additionalOptions );
	}

	class CreateData
	{
		public final CacheLoader< Long, ? extends Cell< ? > > cacheLoader;

		public final CellLoader< T > cellLoader;

		public final T type;

		public final DiskCachedCellImgOptions.Values options;

		public CreateData(
				final CacheLoader< Long, ? extends Cell< ? > > cacheLoader,
				final CellLoader< T > cellLoader,
				final T type,
				final DiskCachedCellImgOptions.Values options )
		{
			this.cacheLoader = cacheLoader;
			this.cellLoader = cellLoader;
			this.type = type;
			this.options = options;
		}
	}

	private final ThreadLocal< CreateData > tlData = new ThreadLocal<>();

	/**
	 * Sets thread-local {@link #tlData} from the specified information and then
	 * calls {@code type.createSuitableNativeImg}.
	 *
	 * @param dim
	 *            dimensions of the image to create.
	 * @param cacheLoader
	 *            user-specified backing loader or {@code null}.
	 * @param cellLoader
	 *            user-specified {@link CellLoader} or {@code null}. Has no
	 *            effect if {@code cacheLoader != null}.
	 * @param type
	 *            type of the image to create
	 * @param additionalOptions
	 *            additional options that partially override general factory
	 *            options, or {@code null}.
	 */
	private DiskCachedCellImg< T, ? > createInternal(
			final long[] dim,
			final CacheLoader< Long, ? extends Cell< ? > > cacheLoader,
			final CellLoader< T > cellLoader,
			final T type,
			final DiskCachedCellImgOptions additionalOptions )
	{
		final DiskCachedCellImgOptions.Values options = ( additionalOptions == null )
				? factoryOptions.values
				: new DiskCachedCellImgOptions.Values( factoryOptions.values, additionalOptions.values );
		try {
			this.tlData.set( new CreateData( cacheLoader, cellLoader, type, options ) );
			return ( DiskCachedCellImg< T, ? > ) type.createSuitableNativeImg( this, dim );
		}
		finally
		{
			this.tlData.set( null );
		}
	}

	@Override
	public DiskCachedCellImg< T, ? extends ByteAccess > createByteInstance( final long[] dimensions, final Fraction entitiesPerPixel )
	{
		return createInstance( dimensions, entitiesPerPixel, BYTE );
	}

	@Override
	public DiskCachedCellImg< T, ? extends CharAccess > createCharInstance( final long[] dimensions, final Fraction entitiesPerPixel )
	{
		return createInstance( dimensions, entitiesPerPixel, CHAR );
	}

	@Override
	public DiskCachedCellImg< T, ? extends ShortAccess > createShortInstance( final long[] dimensions, final Fraction entitiesPerPixel )
	{
		return createInstance( dimensions, entitiesPerPixel, SHORT );
	}

	@Override
	public DiskCachedCellImg< T, ? extends IntAccess > createIntInstance( final long[] dimensions, final Fraction entitiesPerPixel )
	{
		return createInstance( dimensions, entitiesPerPixel, INT );
	}

	@Override
	public DiskCachedCellImg< T, ? extends LongAccess > createLongInstance( final long[] dimensions, final Fraction entitiesPerPixel )
	{
		return createInstance( dimensions, entitiesPerPixel, LONG );
	}

	@Override
	public DiskCachedCellImg< T, ? extends FloatAccess > createFloatInstance( final long[] dimensions, final Fraction entitiesPerPixel )
	{
		return createInstance( dimensions, entitiesPerPixel, FLOAT );
	}

	@Override
	public DiskCachedCellImg< T, ? extends DoubleAccess > createDoubleInstance( final long[] dimensions, final Fraction entitiesPerPixel )
	{
		return createInstance( dimensions, entitiesPerPixel, DOUBLE );
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public < S > ImgFactory< S > imgFactory( final S type ) throws IncompatibleTypeException
	{
		if ( NativeType.class.isInstance( type ) )
			return new DiskCachedCellImgFactory( factoryOptions );
		throw new IncompatibleTypeException( this, type.getClass().getCanonicalName() + " does not implement NativeType." );
	}

	private < A extends ArrayDataAccess< A > >
			DiskCachedCellImg< T, A >
			createInstance( final long[] dimensions, final Fraction entitiesPerPixel, final PrimitiveType primitiveType )
	{
		final CreateData data = tlData.get();
		final DiskCachedCellImgOptions.Values options = data.options;

		final CellGrid grid = createCellGrid( dimensions, entitiesPerPixel, options );

		@SuppressWarnings( "unchecked" )
		CacheLoader< Long, Cell< A > > backingLoader = ( CacheLoader< Long, Cell< A > > ) data.cacheLoader;
		if ( backingLoader == null )
		{
			if ( data.cellLoader != null )
				backingLoader = LoadedCellCacheLoader.get( grid, data.cellLoader, data.type, options.accessFlags() );
			else
				backingLoader = EmptyCellCacheLoader.get( grid, data.type, options.accessFlags() );
		}

		final Path blockcache = createBlockCachePath( options );
		final A accessType = ArrayDataAccessFactory.get( primitiveType, options.accessFlags() );

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final DiskCellCache< A > diskcache = options.dirtyAccesses()
				? new DirtyDiskCellCache(
						blockcache, grid, backingLoader,
						AccessIo.get( primitiveType, options.accessFlags() ),
						entitiesPerPixel )
				: new DiskCellCache<>(
						blockcache, grid, backingLoader,
						AccessIo.get( primitiveType, options.accessFlags() ),
						entitiesPerPixel );

		final IoSync< Long, Cell< A > > iosync = new IoSync<>(
				diskcache,
				options.numIoThreads(),
				options.maxIoQueueSize() );

		LoaderRemoverCache< Long, Cell< A > > listenableCache;
		switch ( options.cacheType() )
		{
		case BOUNDED:
			listenableCache = new GuardedStrongRefLoaderRemoverCache<>( options.maxCacheSize() );
			break;
		case SOFTREF:
		default:
			listenableCache = new SoftRefLoaderRemoverCache<>();
			break;
		}

		final Cache< Long, Cell< A > > cache = listenableCache
				.withRemover( iosync )
				.withLoader( iosync );

		return new DiskCachedCellImg<>( this, grid, entitiesPerPixel, cache, accessType );
	}

	private CellGrid createCellGrid( final long[] dimensions, final Fraction entitiesPerPixel, final DiskCachedCellImgOptions.Values options )
	{
		CellImgFactory.verifyDimensions( dimensions );
		final int n = dimensions.length;
		final int[] cellDimensions = CellImgFactory.getCellDimensions( options.cellDimensions(), n, entitiesPerPixel );
		return new CellGrid( dimensions, cellDimensions );
	}

	private Path createBlockCachePath( final DiskCachedCellImgOptions.Values options )
	{
		try
		{
			final Path cache = options.cacheDirectory();
			final Path dir = options.tempDirectory();
			final String prefix = options.tempDirectoryPrefix();
			final boolean deleteOnExit = options.deleteCacheDirectoryOnExit();
			if ( cache != null )
			{
				if ( !Files.isDirectory( cache ) )
				{
					Files.createDirectories( cache );
					if ( deleteOnExit )
						DiskCellCache.addDeleteHook( cache );
				}
				return cache;
			}
			else if ( dir != null )
				return DiskCellCache.createTempDirectory( dir, prefix, deleteOnExit );
			else
				return DiskCellCache.createTempDirectory( prefix, deleteOnExit );
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
	}
}
