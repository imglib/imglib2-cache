/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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

import java.lang.ref.SoftReference;
import java.nio.file.Path;

import net.imglib2.Dirty;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions.CacheType;
import net.imglib2.img.cell.CellImgFactory;

/**
 * Optional parameters for constructing a {@link DiskCachedCellImgFactory}.
 *
 * @author Tobias Pietzsch
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public class DiskCachedCellImgOptions extends AbstractReadWriteCachedCellImgOptions
{
	private final Values values;

	DiskCachedCellImgOptions(final Values values)
	{
		super();
		this.values = values;
	}

	public DiskCachedCellImgOptions()
	{
		this( new Values() );
	}

	@Override
	public Values values() {
		return values;
	}

	@Override
	public DiskCachedCellImgOptions merge(final AbstractReadWriteCachedCellImgOptions other) {
		return new DiskCachedCellImgOptions(new Values(values, other.values()));
	}

	/**
	 * Create default {@link DiskCachedCellImgOptions}.
	 *
	 * @return default {@link DiskCachedCellImgOptions}.
	 */
	public static DiskCachedCellImgOptions options()
	{
		return new DiskCachedCellImgOptions();
	}

	/**
	 * Specify whether the image should use {@link Dirty} accesses. Dirty
	 * accesses track whether cells were written to. Only cells that were
	 * written to are (potentially) cached to disk.
	 * <p>
	 * This is {@code true} by default.
	 * </p>
	 *
	 * @param dirty
	 *            whether the image should use {@link Dirty} accesses.
	 */
	public DiskCachedCellImgOptions dirtyAccesses( final boolean dirty )
	{
		return new DiskCachedCellImgOptions( values.copy().setDirtyAccesses( dirty ) );
	}

	public DiskCachedCellImgOptions volatileAccesses( final boolean volatil )
	{
		return new DiskCachedCellImgOptions( values.copy().setVolatileAccesses( volatil ) );
	}

	/**
	 * The specified number of threads is started to handle asynchronous writing
	 * of values that are evicted from the memory cache.
	 *
	 * @param numIoThreads
	 *            how many writer threads to start (default is 1).
	 */
	public DiskCachedCellImgOptions numIoThreads( final int numIoThreads )
	{
		return new DiskCachedCellImgOptions( values.copy().setNumIoThreads( numIoThreads ) );
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
	public DiskCachedCellImgOptions maxIoQueueSize( final int maxIoQueueSize )
	{
		return new DiskCachedCellImgOptions(values.copy().setMaxIoQueueSize( maxIoQueueSize ));
	}

	/**
	 * Which in-memory cache type to use. The options are
	 * <ul>
	 * <li>{@link CacheType#SOFTREF SOFTREF}: The cache keeps SoftReferences to
	 * values (cells), basically relying on GC for removal. The advantage of
	 * this is that many caches can be created without needing to put a limit on
	 * the size of any of them. GC will take care of balancing that. The
	 * downside is that {@link OutOfMemoryError} may occur because
	 * {@link SoftReference}s are cleared too slow. SoftReferences are not
	 * collected for a certain time after they have been used. If there is heavy
	 * thrashing with cells being constantly swapped in and out from disk then
	 * OutOfMemory may happen because of this. This sounds worse than it is in
	 * practice and should only happen in pathological situations. Tuning the
	 * {@code -XX:SoftRefLRUPolicyMSPerMB} JVM flag does often help.</li>
	 * <li>{@link CacheType#BOUNDED BOUNDED}: The cache keeps strong references
	 * to a limited number of values (cells). The advantage is that there is
	 * never OutOfMemory because of the issues described above (fingers
	 * crossed). The downside is that the number of cells that should be cached
	 * needs to be specified beforehand. So {@link OutOfMemoryError} may occur
	 * if many caches are opened and consume too much memory in total.</li>
	 * </ul>
	 *
	 * @param cacheType
	 *            which cache type to use (default is {@code SOFTREF}).
	 */
	public DiskCachedCellImgOptions cacheType( final CacheType cacheType )
	{
		return new DiskCachedCellImgOptions( values.copy().setCacheType( cacheType ) );
	}

	/**
	 * Set the maximum number of values (cells) to keep in the cache. This is
	 * only used if {@link #cacheType(CacheType)} is {@link CacheType#BOUNDED}.
	 *
	 * @param maxCacheSize
	 *            maximum number of values in the cache (default is 1000).
	 */
	public DiskCachedCellImgOptions maxCacheSize( final long maxCacheSize )
	{
		return new DiskCachedCellImgOptions( values.copy().setMaxCacheSize( maxCacheSize ) );
	}

	/**
	 * Set the dimensions of a cell. This is extended or truncated as necessary.
	 * For example if {@code cellDimensions=[64,32]} then for creating a 3D
	 * image it will be augmented to {@code [64,32,32]}. For creating a 1D image
	 * it will be truncated to {@code [64]}.
	 *
	 * @param cellDimensions
	 *            dimensions of a cell (default is 10).
	 */
	public DiskCachedCellImgOptions cellDimensions( final int... cellDimensions )
	{
		CellImgFactory.verifyDimensions( cellDimensions );
		return new DiskCachedCellImgOptions( values.copy().setCellDimensions( cellDimensions ) );
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
	public DiskCachedCellImgOptions cacheDirectory( final Path dir )
	{
		return new DiskCachedCellImgOptions( values.copy().setCacheDirectory( dir ) );
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
	public DiskCachedCellImgOptions tempDirectory( final Path dir )
	{
		return new DiskCachedCellImgOptions( values.copy().setTempDirectory( dir ) );
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
	public DiskCachedCellImgOptions tempDirectoryPrefix( final String prefix )
	{
		return new DiskCachedCellImgOptions( values.copy().setTempDirectoryPrefix( prefix ) );
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
	public DiskCachedCellImgOptions deleteCacheDirectoryOnExit( final boolean deleteOnExit )
	{
		return new DiskCachedCellImgOptions(values.copy().setDeleteCacheDirectoryOnExit( deleteOnExit ));
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
	 * created with a {@link CellLoader} ({@link DiskCachedCellImgFactory#create(net.imglib2.Dimensions, CellLoader)}})
	 * </p>
	 *
	 * @param initializeAsDirty
	 *            whether cells initialized by a {@link CellLoader} should be
	 *            marked as dirty.
	 */
	public DiskCachedCellImgOptions initializeCellsAsDirty( final boolean initializeAsDirty )
	{
		return new DiskCachedCellImgOptions(values.copy().setInitializeCellsAsDirty( initializeAsDirty ));
	}

	/**
	 * Read-only {@link DiskCachedCellImgOptions} values.
	 */
	protected static class Values extends AbstractReadWriteCachedCellImgOptions.Values
	{
		/**
		 * Copy constructor.
		 */
		Values( final Values that )
		{
			super(that);
			this.cacheDirectory = that.cacheDirectory;
			this.cacheDirectoryModified = that.cacheDirectoryModified;
			this.tempDirectory = that.tempDirectory;
			this.tempDirectoryModified = that.tempDirectoryModified;
			this.tempDirectoryPrefix = that.tempDirectoryPrefix;
			this.tempDirectoryPrefixModified = that.tempDirectoryPrefixModified;
			this.deleteCacheDirectoryOnExit = that.deleteCacheDirectoryOnExit;
			this.deleteCacheDirectoryOnExitModified = that.deleteCacheDirectoryOnExitModified;
		}

		Values()
		{}

		Values( final Values base, final Values aug )
		{
			super(base, aug);
			cacheDirectory = aug.cacheDirectoryModified
					? aug.cacheDirectory
					: base.cacheDirectory;
			tempDirectory = aug.tempDirectoryModified
					? aug.tempDirectory
					: base.tempDirectory;
			tempDirectoryPrefix = aug.tempDirectoryPrefixModified
					? aug.tempDirectoryPrefix
					: base.tempDirectoryPrefix;
			deleteCacheDirectoryOnExit = aug.deleteCacheDirectoryOnExitModified
					? aug.deleteCacheDirectoryOnExit
					: base.deleteCacheDirectoryOnExit;
		}

		Values( final Values base, final AbstractReadWriteCachedCellImgOptions.Values aug )
		{
			super(base, aug);
			cacheDirectory = base.cacheDirectory;
			tempDirectory = base.tempDirectory;
			tempDirectoryPrefix = base.tempDirectoryPrefix;
			deleteCacheDirectoryOnExit = base.deleteCacheDirectoryOnExit;
		}

		private Path cacheDirectory = null;

		private Path tempDirectory = null;

		private String tempDirectoryPrefix = "imglib2";

		private boolean deleteCacheDirectoryOnExit = true;

		public Path cacheDirectory()
		{
			return cacheDirectory;
		}

		public Path tempDirectory()
		{
			return tempDirectory;
		}

		public String tempDirectoryPrefix()
		{
			return tempDirectoryPrefix;
		}

		public boolean deleteCacheDirectoryOnExit()
		{
			return deleteCacheDirectoryOnExit;
		}

		private boolean cacheDirectoryModified = false;

		private boolean tempDirectoryModified = false;

		private boolean tempDirectoryPrefixModified = false;

		private boolean deleteCacheDirectoryOnExitModified = false;

		Values setCacheDirectory( final Path dir )
		{
			cacheDirectory = dir;
			cacheDirectoryModified = true;
			return this;
		}

		Values setTempDirectory( final Path dir )
		{
			tempDirectory = dir;
			tempDirectoryModified = true;
			return this;
		}

		Values setTempDirectoryPrefix( final String prefix )
		{
			tempDirectoryPrefix = prefix;
			tempDirectoryPrefixModified = true;
			return this;
		}

		Values setDeleteCacheDirectoryOnExit( final boolean b )
		{
			deleteCacheDirectoryOnExit = b;
			deleteCacheDirectoryOnExitModified = true;
			return this;
		}

		@Override
		Values setDirtyAccesses( final boolean b )
		{
			super.setDirtyAccesses(b);
			return this;
		}

		@Override
		Values setVolatileAccesses( final boolean b )
		{
			super.setVolatileAccesses(b);
			return this;
		}

		@Override
		Values setCacheType( final CacheType t )
		{
			super.setCacheType(t);
			return this;
		}

		@Override
		Values setMaxCacheSize( final long n )
		{
			super.setMaxCacheSize(n);
			return this;
		}

		@Override
		Values setCellDimensions( final int[] dims )
		{
			super.setCellDimensions(dims);
			return this;
		}

		@Override
		Values setNumIoThreads(final int n) {
			super.setNumIoThreads(n);
			return this;
		}

		@Override
		Values setMaxIoQueueSize(final int n) {
			super.setMaxIoQueueSize(n);
			return this;
		}

		@Override
		Values setInitializeCellsAsDirty(final boolean b) {
			super.setInitializeCellsAsDirty(b);
			return this;
		}

		@Override
		public String toString()
		{
			final StringBuilder sb = new StringBuilder();
			sb.append(super.toString());
			sb.append( "DiskCachedCellImgOptions = {" );

			sb.append( "cacheDirectory = " );
			sb.append( cacheDirectory );
			if ( cacheDirectoryModified )
				sb.append( " [m]" );
			sb.append( ", " );

			sb.append( "tempDirectory = " );
			sb.append( tempDirectory );
			if ( tempDirectoryModified )
				sb.append( " [m]" );
			sb.append( ", " );

			sb.append( "tempDirectoryPrefix = " );
			sb.append( tempDirectoryPrefix );
			if ( tempDirectoryPrefixModified )
				sb.append( " [m]" );
			sb.append( ", " );

			sb.append( "deleteCacheDirectoryOnExit = " );
			sb.append( Boolean.toString( deleteCacheDirectoryOnExit ) );
			if ( deleteCacheDirectoryOnExitModified )
				sb.append( " [m]" );

			sb.append( "}" );

			return sb.toString();
		}

		@Override
		Values copy() {
			return new Values(this);
		}
	}
}
