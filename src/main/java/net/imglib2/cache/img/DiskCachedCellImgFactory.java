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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.imglib2.Dimensions;
import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

/**
 * Factory for creating {@link DiskCachedCellImg}s. See
 * {@link DiskCachedCellImgOptions} for available configuration options and
 * defaults.
 *
 * @author Tobias Pietzsch
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public class DiskCachedCellImgFactory<T extends NativeType<T>> extends AbstractReadWriteCachedCellImgFactory<T> {
	private DiskCachedCellImgOptions factoryOptions;

	/**
	 * Create a new {@link DiskCachedCellImgFactory} with default configuration.
	 */
	public DiskCachedCellImgFactory( final T type )
	{
		this( type, DiskCachedCellImgOptions.options() );
	}

	/**
	 * Create a new {@link DiskCachedCellImgFactory} with the specified
	 * configuration.
	 *
	 * @param optional
	 *            configuration options.
	 */
	public DiskCachedCellImgFactory( final T type, final DiskCachedCellImgOptions optional )
	{
		super( type );
		this.factoryOptions = optional;
	}

	@Override
	public DiskCachedCellImg< T, ? > create( final long... dimensions )
	{
		return create( dimensions, null, null, type(), null );
	}

	@Override
	public DiskCachedCellImg< T, ? > create( final Dimensions dimensions )
	{
		return create( Intervals.dimensionsAsLongArray( dimensions ) );
	}

	@Override
	public DiskCachedCellImg< T, ? > create( final int[] dimensions )
	{
		return create( Util.int2long( dimensions ) );
	}

	public DiskCachedCellImg< T, ? > create( final long[] dimensions, final DiskCachedCellImgOptions additionalOptions )
	{
		return create( dimensions, null, null, type(), additionalOptions );
	}

	public DiskCachedCellImg< T, ? > create( final Dimensions dimensions, final DiskCachedCellImgOptions additionalOptions )
	{
		return create( Intervals.dimensionsAsLongArray( dimensions ), additionalOptions );
	}

	public DiskCachedCellImg< T, ? > create( final long[] dimensions, final CellLoader< T > loader )
	{
		return create( dimensions, null, loader, type(), null );
	}

	public DiskCachedCellImg< T, ? > create( final Dimensions dimensions, final CellLoader< T > loader )
	{
		return create( Intervals.dimensionsAsLongArray( dimensions ), null, loader, type(), null );
	}

	public DiskCachedCellImg< T, ? > create( final long[] dimensions, final CellLoader< T > loader, final DiskCachedCellImgOptions additionalOptions )
	{
		return create( dimensions, null, loader, type(), additionalOptions );
	}

	public DiskCachedCellImg< T, ? > create( final Dimensions dimensions, final CellLoader< T > loader, final DiskCachedCellImgOptions additionalOptions )
	{
		return create( Intervals.dimensionsAsLongArray( dimensions ), null, loader, type(), additionalOptions );
	}

	public < A > DiskCachedCellImg< T, A > createWithCacheLoader( final long[] dimensions, final CacheLoader< Long, Cell< A > > backingLoader )
	{
		return create( dimensions, backingLoader, null, type(), null );
	}

	public < A > DiskCachedCellImg< T, A > createWithCacheLoader( final Dimensions dimensions, final CacheLoader< Long, Cell< A > > backingLoader )
	{
		return create( Intervals.dimensionsAsLongArray( dimensions ), backingLoader, null, type(), null );
	}

	public < A > DiskCachedCellImg< T, A > createWithCacheLoader( final long[] dimensions, final CacheLoader< Long, Cell< A > > backingLoader, final DiskCachedCellImgOptions additionalOptions )
	{
		return create( dimensions, backingLoader, null, type(), additionalOptions );
	}

	public < A > DiskCachedCellImg< T, A > createWithCacheLoader( final Dimensions dimensions, final CacheLoader< Long, Cell< A > > backingLoader, final DiskCachedCellImgOptions additionalOptions )
	{
		return create( Intervals.dimensionsAsLongArray( dimensions ), backingLoader, null, type(), additionalOptions );
	}

	/**
	 * Create image.
	 *
	 * @param dimensions
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
	private <A> DiskCachedCellImg<T, A> create(final long[] dimensions,
			final CacheLoader<Long, ? extends Cell<? extends A>> cacheLoader, final CellLoader<T> cellLoader,
			final T type, final DiskCachedCellImgOptions additionalOptions) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		final DiskCachedCellImg<T, A> img = (DiskCachedCellImg<T, A>)create(dimensions, cacheLoader, cellLoader, type,
				(NativeTypeFactory) type.getNativeTypeFactory(), additionalOptions);
		return img;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public <S> ImgFactory<S> imgFactory(final S type) throws IncompatibleTypeException {
		if (NativeType.class.isInstance(type))
			return new DiskCachedCellImgFactory(factoryOptions);
		throw new IncompatibleTypeException(this,
				type.getClass().getCanonicalName() + " does not implement NativeType.");
	}

	static Path createBlockCachePath( final DiskCachedCellImgOptions.Values options )
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

	@Override
	AbstractReadWriteCachedCellImgOptions mergeWithFactoryOptions(
			AbstractReadWriteCachedCellImgOptions userProvidedOptions) {
		return ( userProvidedOptions == null ) ? factoryOptions : factoryOptions.merge(userProvidedOptions);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	protected <A extends ArrayDataAccess<A>> ReadWriteCellCache<A> createCellCache(
			AbstractReadWriteCachedCellImgOptions options,
			CellGrid grid, 
			CacheLoader<Long, Cell<A>> backingLoader,
			T type, 
			Fraction entitiesPerPixel) {
		DiskCachedCellImgOptions.Values diskCacheOptions;
		if (options instanceof DiskCachedCellImgOptions) {
			diskCacheOptions = ((DiskCachedCellImgOptions)options).values();
		} else {
			// If the given options are no DiskCachedCellImgOptions, we create default options for
			// the disk cache specifics, and merge them with the provided values.
			diskCacheOptions = DiskCachedCellImgOptions.options().merge(options).values();
		}
		
		final Path blockcache = createBlockCachePath(diskCacheOptions);
		return diskCacheOptions.dirtyAccesses()
				? (ReadWriteCellCache<A>)new DirtyDiskCellCache(blockcache, grid, backingLoader, AccessIo.get(type, diskCacheOptions.accessFlags()),
						entitiesPerPixel)
				: new DiskCellCache<>(blockcache, grid, backingLoader, AccessIo.get(type, diskCacheOptions.accessFlags()),
						entitiesPerPixel);
	}

	@Override
	protected <A extends ArrayDataAccess<A>> CachedCellImg<T, A> createCachedCellImg(
			final CellGrid grid, 
			final Fraction entitiesPerPixel,
			final Cache<Long, Cell<A>> cache,
			final A accessType
	) {
		return new DiskCachedCellImg<>(this, grid, entitiesPerPixel, cache, accessType);
	}

	/*
	 * -----------------------------------------------------------------------
	 *
	 * Deprecated API.
	 *
	 * Supports backwards compatibility with ImgFactories that are constructed
	 * without a type instance or supplier.
	 *
	 * -----------------------------------------------------------------------
	 */

	@Deprecated
	public DiskCachedCellImgFactory()
	{
		this( DiskCachedCellImgOptions.options() );
	}

	@Deprecated
	public DiskCachedCellImgFactory( final DiskCachedCellImgOptions optional )
	{
		this.factoryOptions = optional;
	}

	@Deprecated
	@Override
	public DiskCachedCellImg< T, ? > create( final long[] dim, final T type )
	{
		cache( type );
		return create( dim, null, null, type, null );
	}

	@Deprecated
	public DiskCachedCellImg< T, ? > create( final long[] dim, final T type, final DiskCachedCellImgOptions additionalOptions )
	{
		cache( type );
		return create( dim, null, null, type, additionalOptions );
	}

	@Deprecated
	public DiskCachedCellImg< T, ? > create( final long[] dim, final T type, final CellLoader< T > loader )
	{
		cache( type );
		return create( dim, null, loader, type, null );
	}

	@Deprecated
	public DiskCachedCellImg< T, ? > create( final long[] dim, final T type, final CellLoader< T > loader, final DiskCachedCellImgOptions additionalOptions )
	{
		cache( type );
		return create( dim, null, loader, type, additionalOptions );
	}

	@Deprecated
	public < A > DiskCachedCellImg< T, A > createWithCacheLoader( final long[] dim, final T type, final CacheLoader< Long, Cell< A > > backingLoader )
	{
		cache( type );
		return create( dim, backingLoader, null, type, null );
	}

	@Deprecated
	public < A > DiskCachedCellImg< T, A > createWithCacheLoader( final long[] dim, final T type, final CacheLoader< Long, Cell< A > > backingLoader, final DiskCachedCellImgOptions additionalOptions )
	{
		cache( type );
		return create( dim, backingLoader, null, type, additionalOptions );
	}
}
