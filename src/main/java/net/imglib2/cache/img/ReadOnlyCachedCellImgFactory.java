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

import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderCache;
import net.imglib2.cache.ref.GuardedStrongRefLoaderCache;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
import net.imglib2.util.Fraction;

/**
 * Factory for creating read-only {@link CachedCellImg}s that are backed by either a {@link CellLoader} or a {@link CacheLoader}.
 *
 * @author Tobias Pietzsch
 */
public class ReadOnlyCachedCellImgFactory
{
	private final ReadOnlyCachedCellImgOptions factoryOptions;

	/**
	 * Create a new {@link ReadOnlyCachedCellImgFactory} with default configuration.
	 */
	public ReadOnlyCachedCellImgFactory()
	{
		this( ReadOnlyCachedCellImgOptions.options() );
	}

	/**
	 * Create a new {@link ReadOnlyCachedCellImgFactory} with the specified
	 * configuration.
	 *
	 * @param optional
	 *            configuration options.
	 */
	public ReadOnlyCachedCellImgFactory( final ReadOnlyCachedCellImgOptions optional )
	{
		this.factoryOptions = optional;
	}

	public < T extends NativeType< T > > CachedCellImg< T, ? > create( final long[] dim, final T type, final CellLoader< T > loader )
	{
		return createInternal( dim, null, loader, type, null );
	}

	public < T extends NativeType< T > > CachedCellImg< T, ? > create( final long[] dim, final T type, final CellLoader< T > loader, final ReadOnlyCachedCellImgOptions additionalOptions )
	{
		return createInternal( dim, null, loader, type, additionalOptions );
	}

	@SuppressWarnings( "unchecked" )
	public < T extends NativeType< T >, A > CachedCellImg< T, A > createWithCacheLoader( final long[] dim, final T type, final CacheLoader< Long, Cell< A > > backingLoader )
	{
		return ( CachedCellImg< T, A > ) createInternal( dim, backingLoader, null, type, null );
	}

	@SuppressWarnings( "unchecked" )
	public < T extends NativeType< T >, A > CachedCellImg< T, A > createWithCacheLoader( final long[] dim, final T type, final CacheLoader< Long, Cell< A > > backingLoader, final ReadOnlyCachedCellImgOptions additionalOptions )
	{
		return ( CachedCellImg< T, A > ) createInternal( dim, backingLoader, null, type, additionalOptions );
	}

	/**
	 * Create {@link CachedCellImg} backed n=by either a {@link CellLoader} or a {@link CacheLoader}.
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
	private < T extends NativeType< T >, A extends ArrayDataAccess< A > > CachedCellImg< T, ? > createInternal(
			final long[] dimensions,
			final CacheLoader< Long, ? extends Cell< ? > > cacheLoader,
			final CellLoader< T > cellLoader,
			final T type,
			final ReadOnlyCachedCellImgOptions additionalOptions )
	{
		final ReadOnlyCachedCellImgOptions.Values options = ( additionalOptions == null )
				? factoryOptions.values
				: new ReadOnlyCachedCellImgOptions.Values( factoryOptions.values, additionalOptions.values );

		final PrimitiveType primitiveType = type.getNativeTypeFactory().getPrimitiveType();
		final Fraction entitiesPerPixel = type.getEntitiesPerPixel();
		final CellGrid grid = createCellGrid( dimensions, entitiesPerPixel, options );
		final A accessType = ArrayDataAccessFactory.get( primitiveType, options.accessFlags() );

		@SuppressWarnings( "unchecked" )
		final CacheLoader< Long, Cell< A > > loader = ( cacheLoader != null )
				? ( CacheLoader< Long, Cell< A > > ) cacheLoader
				: LoadedCellCacheLoader.get( grid, cellLoader, type, options.accessFlags() );

		LoaderCache< Long, Cell< A > > loaderCache;
		switch ( options.cacheType() )
		{
		case BOUNDED:
			loaderCache = new GuardedStrongRefLoaderCache<>( options.maxCacheSize() );
			break;
		case SOFTREF:
		default:
			loaderCache = new SoftRefLoaderCache<>();
			break;
		}
		final Cache< Long, Cell< A > > cache = loaderCache.withLoader( loader );

		return new CachedCellImg<>( grid, type, cache, accessType );
	}

	private CellGrid createCellGrid( final long[] dimensions, final Fraction entitiesPerPixel, final ReadOnlyCachedCellImgOptions.Values options )
	{
		CellImgFactory.verifyDimensions( dimensions );
		final int n = dimensions.length;
		final int[] cellDimensions = CellImgFactory.getCellDimensions( options.cellDimensions(), n, entitiesPerPixel );
		return new CellGrid( dimensions, cellDimensions );
	}
}
