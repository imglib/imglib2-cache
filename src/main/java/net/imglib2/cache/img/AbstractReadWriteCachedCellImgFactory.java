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
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.util.Fraction;

/**
 * Abstract factory for creating {@link CachedCellImg}s. See {@link DiskCachedCellImgFactory} or 
 * {@link N5CachedCellImgFactory} for implementations.
 *
 * @author Tobias Pietzsch
 * @author Carsten Haubold
 */
public abstract class AbstractReadWriteCachedCellImgFactory<T extends NativeType<T>> extends NativeImgFactory<T> {
	/**
	 * Create a new {@link AbstractReadWriteCachedCellImgFactory} with the specified
	 * configuration.
	 *
	 * @param optional configuration options.
	 */
	public AbstractReadWriteCachedCellImgFactory(final T type) {
		super(type);
	}

	abstract AbstractReadWriteCachedCellImgOptions mergeWithFactoryOptions(final AbstractReadWriteCachedCellImgOptions userProvidedValues);

	protected <A extends ArrayDataAccess<A>> CachedCellImg<T, ? extends A> create(final long[] dimensions,
			final CacheLoader<Long, ? extends Cell<?>> cacheLoader, 
			final CellLoader<T> cellLoader, 
			final T type,
			final NativeTypeFactory<T, A> typeFactory, 
			final AbstractReadWriteCachedCellImgOptions additionalOptions) {

		final AbstractReadWriteCachedCellImgOptions options = mergeWithFactoryOptions(additionalOptions);

		final Fraction entitiesPerPixel = type.getEntitiesPerPixel();

		final CellGrid grid = createCellGrid(dimensions, entitiesPerPixel, options);

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

		final CellCache<A> cellCache = createCellCache(options, grid, backingLoader, type, entitiesPerPixel);

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

	protected abstract <A extends ArrayDataAccess<A>> CachedCellImg<T, ? extends A> createCachedCellImg(
		final CellGrid grid, 
		final Fraction entitiesPerPixel,
		final Cache<Long, Cell<A>> cache,
		final A accessType
	);

	protected abstract <A extends ArrayDataAccess<A>> CellCache<A> createCellCache(
		final AbstractReadWriteCachedCellImgOptions options, 
		final CellGrid grid, 
		final CacheLoader<Long, Cell<A>> backingLoader,
		final T type,
		final Fraction entitiesPerPixel);

	protected CellGrid createCellGrid(
		final long[] dimensions, 
		final Fraction entitiesPerPixel,
		final AbstractReadWriteCachedCellImgOptions options) 
	{
		CellImgFactory.verifyDimensions(dimensions);
		final int n = dimensions.length;
		final int[] cellDimensions = CellImgFactory.getCellDimensions(options.values().cellDimensions(), n, entitiesPerPixel);
		return new CellGrid(dimensions, cellDimensions);
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
	public AbstractReadWriteCachedCellImgFactory() {
	}
}
