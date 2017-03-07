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

/**
 * Optional parameters for constructing a {@link DiskCachedCellImgFactory}.
 *
 * @author Tobias Pietzsch
 */
public class DiskCachedCellImgOptions
{
	public final Values values = new Values();

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
		values.dirtyAccesses = dirty;
		return this;
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
		values.numIoThreads = numIoThreads;
		return this;
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
		values.maxIoQueueSize = maxIoQueueSize;
		return this;
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
		values.cacheType = cacheType;
		return this;
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
		values.maxCacheSize = maxCacheSize;
		return this;
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
		values.cellDimensions = cellDimensions;
		return this;
	}

	/**
	 * Read-only {@link DiskCachedCellImgOptions} values.
	 */
	public static class Values
	{
		public DiskCachedCellImgOptions optionsFromValues()
		{
			return options()
					.dirtyAccesses( dirtyAccesses )
					.numIoThreads( numIoThreads )
					.maxIoQueueSize( maxIoQueueSize )
					.cacheType( cacheType )
					.cellDimensions( cellDimensions );
		}

		private boolean dirtyAccesses = true;

		private int numIoThreads = 1;

		private int maxIoQueueSize = 10;

		private CacheType cacheType = CacheType.SOFTREF;

		private long maxCacheSize = 1000;

		private int[] cellDimensions = new int[] { 10 };

		public boolean dirtyAccesses()
		{
			return dirtyAccesses;
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
	}
}
