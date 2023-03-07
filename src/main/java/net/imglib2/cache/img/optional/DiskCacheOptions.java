/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2023 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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
package net.imglib2.cache.img.optional;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.DiskCachedCellImg;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import org.scijava.optional.Options;
import org.scijava.optional.Values;

/**
 * Optional arguments that specify the properties of the disk cache for {@link DiskCachedCellImg}.
 */
public interface DiskCacheOptions< T > extends Options< T >
{
	/**
	 * The specified number of threads is started to handle asynchronous writing
	 * of values that are evicted from the memory cache.
	 *
	 * @param numIoThreads
	 *            how many writer threads to start (default is 1).
	 */
	default T numIoThreads( final int numIoThreads )
	{
		return setValue( "numIoThreads", numIoThreads );
	}

	/**
	 * Set the maximum size of the disk write queue. When the queue is full,
	 * removing entries from the cache will block until earlier values have been
	 * written.
	 * <p>
	 * Because processing of removed entries is done whenever the cache is
	 * accessed, this may also block accesses to the cache. (This is a good
	 * thing, because it avoids running out of memory because entries cannot be
	 * cleared fast enough...)
	 * </p>
	 *
	 * @param maxIoQueueSize
	 *            the maximum size of the write queue (default is 10).
	 */
	default T maxIoQueueSize( final int maxIoQueueSize )
	{
		return setValue( "maxIoQueueSize", maxIoQueueSize );
	}

	/**
	 * Set the path of the cell cache directory.
	 * <p>
	 * This is {@code null} by default, which means that a temporary directory
	 * will be created.
	 * </p>
	 * <p>
	 * Do not use the same cell cache directory for two images at the same time.
	 * Set {@code deleteCacheDirectoryOnExit(false)} if you do not want the cell
	 * cache directory to be deleted when the virtual machine shuts down.
	 * </p>
	 *
	 * @param dir
	 *            the path to the cell cache directory.
	 */
	default T cacheDirectory( final Path dir )
	{
		return setValue( "cacheDirectory", dir );
	}

	/**
	 * Set the path to the directory in which to create the temporary cell cache
	 * directory. This has no effect, if {@link #cacheDirectory(Path)} is
	 * specified.
	 * <p>
	 * This is {@code null} by default, which means that the default system
	 * temporary-file directory is used.
	 * </p>
	 *
	 * @param dir
	 *            the path to directory in which to create the temporary cell
	 *            cache directory.
	 */
	default T tempDirectory( final Path dir )
	{
		return setValue( "tempDirectory", dir );
	}

	/**
	 * Set the prefix string to be used in generating the name of the temporary
	 * cell cache directory. Note, that this is not the path in which the
	 * directory is created but a prefix to the name (e.g. "MyImg"). This has no
	 * effect, if {@link #cacheDirectory(Path)} is specified.
	 * <p>
	 * This is {@code "imglib2"} by default.
	 * </p>
	 *
	 * @param prefix
	 *            the prefix string to be used in generating the name of the
	 *            temporary cell cache directory.
	 */
	default T tempDirectoryPrefix( final String prefix )
	{
		return setValue( "tempDirectoryPrefix", prefix );
	}

	/**
	 * Specify whether the cell cache directory should be automatically deleted
	 * when the virtual machine shuts down.
	 * <p>
	 * This is {@code true} by default.
	 * </p>
	 * <p>
	 * For safety reasons, only cell cache directories that are created by the
	 * {@link DiskCachedCellImgFactory} are actually marked for deletion. This
	 * means that either no {@link #cacheDirectory(Path)} is specified (a
	 * temporary directory is created), or the specified
	 * {@link #cacheDirectory(Path)} does not exist yet.
	 * </p>
	 *
	 * @param deleteOnExit
	 *            whether the cell cache directory directory should be
	 *            automatically deleted when the virtual machine shuts down.
	 */
	default T deleteCacheDirectoryOnExit( final boolean deleteOnExit )
	{
		return setValue( "deleteCacheDirectoryOnExit", deleteOnExit );
	}

	/**
	 * Specify whether cells initialized by a {@link CellLoader} should be
	 * marked as dirty. It is useful to set this to {@code true} if
	 * initialization is a costly operation. By this, it is made sure that cells
	 * are initialized only once, and then written and retrieve from the disk
	 * cache when they are next required.
	 * <p>
	 * This is {@code false} by default.
	 * </p>
	 * <p>
	 * This option only has an effect for {@link DiskCachedCellImg} that are
	 * created with a {@link CellLoader}
	 * ({@link DiskCachedCellImgFactory#create(long[], CellLoader)}).
	 * </p>
	 *
	 * @param initializeAsDirty
	 *            whether cells initialized by a {@link CellLoader} should be
	 *            marked as dirty.
	 */
	default T initializeCellsAsDirty( final boolean initializeAsDirty )
	{
		return setValue( "initializeCellsAsDirty", initializeAsDirty );
	}

	interface Val extends Values
	{
		default void forEach( BiConsumer< String, Object > action )
		{
			action.accept( "numIoThreads", numIoThreads() );
			action.accept( "maxIoQueueSize", maxIoQueueSize() );
			action.accept( "cacheDirectory", cacheDirectory() );
			action.accept( "tempDirectory", tempDirectory() );
			action.accept( "tempDirectoryPrefix", tempDirectoryPrefix() );
			action.accept( "deleteCacheDirectoryOnExit", deleteCacheDirectoryOnExit() );
			action.accept( "initializeCellsAsDirty", initializeCellsAsDirty() );
		}

		default int numIoThreads()
		{
			return getValueOrDefault( "numIoThreads", 1 );
		}

		default int maxIoQueueSize()
		{
			return getValueOrDefault( "maxIoQueueSize", 10 );
		}

		default Path cacheDirectory()
		{
			return getValueOrDefault( "cacheDirectory", null );
		}

		default Path tempDirectory()
		{
			return getValueOrDefault( "tempDirectory", null );
		}

		default String tempDirectoryPrefix()
		{
			return getValueOrDefault( "tempDirectoryPrefix", "imglib2" );
		}

		default boolean deleteCacheDirectoryOnExit()
		{
			return getValueOrDefault( "deleteCacheDirectoryOnExit", true );
		}

		default boolean initializeCellsAsDirty()
		{
			return getValueOrDefault( "initializeCellsAsDirty", false );
		}
	}
}
