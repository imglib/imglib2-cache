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

import net.imglib2.Dirty;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.util.Util;

/**
 * Optional parameters for constructing a {@link DiskCachedCellImgFactory}.
 *
 * @author Tobias Pietzsch
 */
public class DiskCachedCellImgOptions
{
	public final Values values;

	DiskCachedCellImgOptions( final Values values )
	{
		this.values = values;
	}

	public DiskCachedCellImgOptions()
	{
		this( new Values() );
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
		return new DiskCachedCellImgOptions( values.copy().setMaxIoQueueSize( maxIoQueueSize ) );
	}

	/**
	 * Rough in-memory cache types.
	 *
	 * @author Tobias Pietzsch
	 */
	public static enum CacheType
	{
		/**
		 * The cache keeps SoftReferences to values (cells), basically relying
		 * on GC for removal. The advantage of this is that many caches can be
		 * created without needing to put a limit on the size of any of them. GC
		 * will take care of balancing that. The downside is that
		 * {@link OutOfMemoryError} may occur because {@link SoftReference}s are
		 * cleared too slow. SoftReferences are not collected for a certain time
		 * after they have been used. If there is heavy thrashing with cells
		 * being constantly swapped in and out from disk then OutOfMemory may
		 * happen because of this. This sounds worse than it is in practice and
		 * should only happen in pathological situations. Tuning the
		 * {@code -XX:SoftRefLRUPolicyMSPerMB} JVM flag does often help.
		 */
		SOFTREF,

		/**
		 * The cache keeps strong references to a limited number of values
		 * (cells). The advantage is that there is never OutOfMemory because of
		 * the issues described above (fingers crossed). The downside is that
		 * the number of cells that should be cached needs to be specified
		 * beforehand. So {@link OutOfMemoryError} may occur if many caches are
		 * opened and consume too much memory in total.
		 */
		BOUNDED
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
	 * Read-only {@link DiskCachedCellImgOptions} values.
	 */
	public static class Values
	{
		/**
		 * Copy constructor.
		 */
		Values( final Values that )
		{
			this.dirtyAccesses = that.dirtyAccesses;
			this.dirtyAccessesModified = that.dirtyAccessesModified;
			this.volatileAccesses = that.volatileAccesses;
			this.volatileAccessesModified = that.volatileAccessesModified;
			this.numIoThreads = that.numIoThreads;
			this.numIoThreadsModified = that.numIoThreadsModified;
			this.maxIoQueueSize = that.maxIoQueueSize;
			this.maxIoQueueSizeModified = that.maxIoQueueSizeModified;
			this.cacheType = that.cacheType;
			this.cacheTypeModified = that.cacheTypeModified;
			this.maxCacheSize = that.maxCacheSize;
			this.maxCacheSizeModified = that.maxCacheSizeModified;
			this.cellDimensions = that.cellDimensions;
			this.cellDimensionsModified = that.cellDimensionsModified;
		}

		Values()
		{}

		Values( final Values base, final Values aug )
		{
			dirtyAccesses = aug.dirtyAccessesModified
					? aug.dirtyAccesses
					: base.dirtyAccesses;
			volatileAccesses = aug.volatileAccessesModified
					? aug.volatileAccesses
					: base.volatileAccesses;
			numIoThreads = aug.numIoThreadsModified
					? aug.numIoThreads
					: base.numIoThreads;
			maxIoQueueSize = aug.maxIoQueueSizeModified
					? aug.maxIoQueueSize
					: base.maxIoQueueSize;
			cacheType = aug.cacheTypeModified
					? aug.cacheType
					: base.cacheType;
			maxCacheSize = aug.maxCacheSizeModified
					? aug.maxCacheSize
					: base.maxCacheSize;
			cellDimensions = aug.cellDimensionsModified
					? aug.cellDimensions
					: base.cellDimensions;

		}

		public DiskCachedCellImgOptions optionsFromValues()
		{
			return new DiskCachedCellImgOptions( new Values( this ) );
		}

		private boolean dirtyAccesses = true;

		private boolean volatileAccesses = true;

		private int numIoThreads = 1;

		private int maxIoQueueSize = 10;

		private CacheType cacheType = CacheType.SOFTREF;

		private long maxCacheSize = 1000;

		private int[] cellDimensions = new int[] { 10 };

		public boolean dirtyAccesses()
		{
			return dirtyAccesses;
		}

		public boolean volatileAccesses()
		{
			return volatileAccesses;
		}

		public AccessFlags[] accessFlags()
		{
			return AccessFlags.fromBooleansDirtyVolatile( dirtyAccesses, volatileAccesses );
		}

		public int numIoThreads()
		{
			return numIoThreads;
		}

		public int maxIoQueueSize()
		{
			return maxIoQueueSize;
		}

		public CacheType cacheType()
		{
			return cacheType;
		}

		public long maxCacheSize()
		{
			return maxCacheSize;
		}

		public int[] cellDimensions()
		{
			return cellDimensions;
		}

		private boolean dirtyAccessesModified = false;

		private boolean volatileAccessesModified = false;

		private boolean numIoThreadsModified = false;

		private boolean maxIoQueueSizeModified = false;

		private boolean cacheTypeModified = false;

		private boolean maxCacheSizeModified = false;

		private boolean cellDimensionsModified = false;

		Values setDirtyAccesses( final boolean b )
		{
			dirtyAccesses = b;
			dirtyAccessesModified = true;
			return this;
		}

		Values setVolatileAccesses( final boolean b )
		{
			volatileAccesses = b;
			volatileAccessesModified = true;
			return this;
		}

		Values setNumIoThreads( final int n )
		{
			numIoThreads = n;
			numIoThreadsModified = true;
			return this;
		}

		Values setMaxIoQueueSize( final int n )
		{
			maxIoQueueSize = n;
			maxIoQueueSizeModified = true;
			return this;
		}

		Values setCacheType( final CacheType t )
		{
			cacheType = t;
			cacheTypeModified = true;
			return this;
		}

		Values setMaxCacheSize( final long n )
		{
			maxCacheSize = n;
			maxCacheSizeModified = true;
			return this;
		}

		Values setCellDimensions( final int[] dims )
		{
			cellDimensions = dims;
			cellDimensionsModified = true;
			return this;
		}

		Values copy()
		{
			return new Values( this );
		}

		@Override
		public String toString()
		{
			final StringBuilder sb = new StringBuilder();

			sb.append( "{" );
			sb.append( "\n " );

			sb.append( "dirtyAccesses = " );
			sb.append( Boolean.toString( dirtyAccesses ) );
			if ( dirtyAccessesModified )
				sb.append( " [m]" );
			sb.append( ", " );
			sb.append( "\n " );

			sb.append( "volatileAccesses = " );
			sb.append( Boolean.toString( volatileAccesses ) );
			if ( volatileAccessesModified )
				sb.append( " [m]" );
			sb.append( ", " );
			sb.append( "\n " );

			sb.append( "numIoThreads = " );
			sb.append( numIoThreads );
			if ( numIoThreadsModified )
				sb.append( " [m]" );
			sb.append( ", " );
			sb.append( "\n " );

			sb.append( "maxIoQueueSize = " );
			sb.append( maxIoQueueSize );
			if ( maxIoQueueSizeModified )
				sb.append( " [m]" );
			sb.append( ", " );
			sb.append( "\n " );

			sb.append( "cacheType = " );
			sb.append( cacheType );
			if ( cacheTypeModified )
				sb.append( " [m]" );
			sb.append( ", " );
			sb.append( "\n " );

			sb.append( "maxCacheSize = " );
			sb.append( maxCacheSize );
			if ( maxCacheSizeModified )
				sb.append( " [m]" );
			sb.append( ", " );
			sb.append( "\n " );

			sb.append( "cellDimensions = " );
			sb.append( Util.printCoordinates( cellDimensions ) );
			if ( cellDimensionsModified )
				sb.append( " [m]" );

			sb.append( "\n" );
			sb.append( "}" );

			return sb.toString();
		}


	}
}
