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
import net.imglib2.cache.IoSync;
import net.imglib2.cache.LoaderRemoverCache;
import net.imglib2.cache.ref.GuardedStrongRefLoaderRemoverCache;
import net.imglib2.cache.ref.SoftRefLoaderRemoverCache;
import net.imglib2.img.NativeImgFactory;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.util.Fraction;

/**
 * Abstract factory for creating {@link CachedCellImg}s. Holds functionality shared by read-write-caches,
 * but leaves the implementation of the cell writing to specialized implementations.
 * 
 * See {@link DiskCachedCellImgFactory} for a specialized example.
 * 
 * @param <T> Element type of the images that can be created by this factory
 *
 * @author Tobias Pietzsch
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractReadWriteCachedCellImgFactory<T extends NativeType<T>> extends NativeImgFactory<T> {
	/**
	 * Create a new {@link AbstractReadWriteCachedCellImgFactory} that can create images of the provided type.
	 *
	 * @param type Element type of the images that can be created by this factory
	 */
	public AbstractReadWriteCachedCellImgFactory(final T type) {
		super(type);
	}

	/**
	 * Merge this factory's default options (which can be specified in the constructor) with the user provided options.
	 * User provided option values that differ from their default value take precedence over the factory's default options 
	 * 
	 * @param userProvidedOptions The options that were provided by the user when calling one of the methods of this factory.
	 * @return a new options object created by merging this factory's default options with the provided ones
	 */
	abstract AbstractReadWriteCachedCellImgOptions mergeWithFactoryOptions(final AbstractReadWriteCachedCellImgOptions userProvidedOptions);

	/**
	 * Create a cached cell img with the provided settings. Much of the work is deferred to abstract methods that
	 * must be implemented for the specific writer-backend in specialized classes.
	 * 
	 * @param dimensions Dimensions of the image that should be created
	 * @param cacheLoader Loader for already cached cells (optional)
	 * @param cellLoader Loader for Cells that are not cached yet (optional, cache will provide empty cells if this is null)
	 * @param type Instance of the element type of the image that should be created
	 * @param typeFactory A native type factory
	 * @param additionalOptions Cache options that extend this cache factory's options
	 * @param <A> Access Type
	 * @return A CachedCellImg with the given cache configuration
	 */
	protected <A extends ArrayDataAccess<A>> CachedCellImg<T, ? extends A> create(final long[] dimensions,
			final CacheLoader<Long, ? extends Cell<?>> cacheLoader, 
			final CellLoader<T> cellLoader, 
			final T type,
			final NativeTypeFactory<T, A> typeFactory, 
			final AbstractReadWriteCachedCellImgOptions additionalOptions) {

		final AbstractReadWriteCachedCellImgOptions options = mergeWithFactoryOptions(additionalOptions);

		final Fraction entitiesPerPixel = type.getEntitiesPerPixel();

		final CellGrid grid = ReadOnlyCachedCellImgFactory.createCellGrid(dimensions, options.values().cellDimensions(), entitiesPerPixel);

		@SuppressWarnings("unchecked")
		CacheLoader<Long, Cell<A>> backingLoader = (CacheLoader<Long, Cell<A>>) cacheLoader;
		if (backingLoader == null) {
			if (cellLoader != null) {
				final CellLoader<T> actualCellLoader = options.values().initializeCellsAsDirty() ? cell -> {
					cellLoader.load(cell);
					cell.setDirty();
				} : cellLoader;
				backingLoader = LoadedCellCacheLoader.get(grid, actualCellLoader, type, options.values().accessFlags());
			} else
				backingLoader = EmptyCellCacheLoader.get(grid, type, options.values().accessFlags());
		}

		final ReadWriteCellCache<A> cellCache = createCellCache(options, grid, backingLoader, type, entitiesPerPixel);

		final IoSync<Long, Cell<A>> iosync = new IoSync<>(cellCache, options.values().numIoThreads(), options.values().maxIoQueueSize());

		LoaderRemoverCache<Long, Cell<A>> listenableCache;
		switch (options.values().cacheType()) {
		case BOUNDED:
			listenableCache = new GuardedStrongRefLoaderRemoverCache<>(options.values().maxCacheSize());
			break;
		case SOFTREF:
		default:
			listenableCache = new SoftRefLoaderRemoverCache<>();
			break;
		}

		final Cache<Long, Cell<A>> cache = listenableCache.withRemover(iosync).withLoader(iosync);
		final A accessType = ArrayDataAccessFactory.get(typeFactory, options.values().accessFlags());

		final CachedCellImg<T, ? extends A> img = createCachedCellImg(grid, entitiesPerPixel, cache, accessType);
		img.setLinkedType(typeFactory.createLinkedType(img));
		return img;
	}

	/**
	 * Derived classes should create an instance of the CachedCellImg type that they support, given the provided cache and grid
	 * E.g. a {@link DiskCachedCellImgFactory} would create and return a {@link DiskCachedCellImg}.
	 * 
	 * @param grid The grid structure of the CellCache
	 * @param entitiesPerPixel 
	 * @param cache The configured cache to use as backing for the image
	 * @param accessType
	 * @return A {@link CachedCellImg}
	 */
	protected abstract <A extends ArrayDataAccess<A>> CachedCellImg<T, ? extends A> createCachedCellImg(
		final CellGrid grid, 
		final Fraction entitiesPerPixel,
		final Cache<Long, Cell<A>> cache,
		final A accessType
	);

	/**
	 * Derived classes should create a read-write cell cache with the given options, cell grid and backing loader.
	 * @param options cache creation options
	 * @param grid cell grid
	 * @param backingLoader the backing loader for cache cells
	 * @param type element type
	 * @param entitiesPerPixel
	 * @return A {@link ReadWriteCellCache}
	 */
	protected abstract <A extends ArrayDataAccess<A>> ReadWriteCellCache<A> createCellCache(
		final AbstractReadWriteCachedCellImgOptions options, 
		final CellGrid grid, 
		final CacheLoader<Long, Cell<A>> backingLoader,
		final T type,
		final Fraction entitiesPerPixel);

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
	public AbstractReadWriteCachedCellImgFactory() {
	}
}
