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
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions.CacheType;

/**
 * Options base class used when constructing a cell image factory derived from {@link AbstractCachedCellImgFactory}. 
 * This holds a set of default options shared by read-write-caches. Specialized writer-backends should provide their
 * own CachedCellImgFactory and -Options.
 * 
 * Must be subclassed, which usually also requires a subclass of Values that adds 
 * specific options depending on the writer-backend for the read-write-cache.
 *
 * @author Tobias Pietzsch
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractReadWriteCachedCellImgOptions {
	AbstractReadWriteCachedCellImgOptions() {
	}

	/**
	 * @return The values of the (derived) options object
	 */
	abstract Values values();

	/**
	 * @param other Options that should be merged with this option object
	 * @return New {@link AbstractReadWriteCachedCellImgOptions} containing the options of this object, 
	 * overwritten by the non-default settings in the provided other options object
	 */
	public abstract AbstractReadWriteCachedCellImgOptions merge(final AbstractReadWriteCachedCellImgOptions other);

	/**
	 * Specify whether the image should use {@link Dirty} accesses. Dirty accesses
	 * track whether cells were written to. Only cells that were written to are
	 * (potentially) cached to disk.
	 * <p>
	 * This is {@code true} by default.
	 * </p>
	 *
	 * @param dirty whether the image should use {@link Dirty} accesses.
	 */
	public abstract AbstractReadWriteCachedCellImgOptions dirtyAccesses(final boolean dirty);

	public abstract AbstractReadWriteCachedCellImgOptions volatileAccesses(final boolean volatil);

	/**
	 * The specified number of threads is started to handle asynchronous writing of
	 * values that are evicted from the memory cache.
	 *
	 * @param numIoThreads how many writer threads to start (default is 1).
	 */
	public abstract AbstractReadWriteCachedCellImgOptions numIoThreads(final int numIoThreads);

	/**
	 * Set the maximum size of the disk write queue. When the queue is full,
	 * removing entries from the cache will block until earlier values have been
	 * written.
	 * <p>
	 * Because processing of removed entries is done whenever the cache is accessed,
	 * this may also block accesses to the cache. (This is a good thing, because it
	 * avoids running out of memory because entries cannot be cleared fast
	 * enough...)
	 * </p>
	 *
	 * @param maxIoQueueSize the maximum size of the write queue (default is 10).
	 */
	public abstract AbstractReadWriteCachedCellImgOptions maxIoQueueSize(final int maxIoQueueSize);

	/**
	 * Which in-memory cache type to use. The options are
	 * <ul>
	 * <li>{@link CacheType#SOFTREF SOFTREF}: The cache keeps SoftReferences to
	 * values (cells), basically relying on GC for removal. The advantage of this is
	 * that many caches can be created without needing to put a limit on the size of
	 * any of them. GC will take care of balancing that. The downside is that
	 * {@link OutOfMemoryError} may occur because {@link SoftReference}s are cleared
	 * too slow. SoftReferences are not collected for a certain time after they have
	 * been used. If there is heavy thrashing with cells being constantly swapped in
	 * and out from disk then OutOfMemory may happen because of this. This sounds
	 * worse than it is in practice and should only happen in pathological
	 * situations. Tuning the {@code -XX:SoftRefLRUPolicyMSPerMB} JVM flag does
	 * often help.</li>
	 * <li>{@link CacheType#BOUNDED BOUNDED}: The cache keeps strong references to a
	 * limited number of values (cells). The advantage is that there is never
	 * OutOfMemory because of the issues described above (fingers crossed). The
	 * downside is that the number of cells that should be cached needs to be
	 * specified beforehand. So {@link OutOfMemoryError} may occur if many caches
	 * are opened and consume too much memory in total.</li>
	 * </ul>
	 *
	 * @param cacheType which cache type to use (default is {@code SOFTREF}).
	 */
	public abstract AbstractReadWriteCachedCellImgOptions cacheType(final CacheType cacheType);

	/**
	 * Set the maximum number of values (cells) to keep in the cache. This is only
	 * used if {@link #cacheType(CacheType)} is {@link CacheType#BOUNDED}.
	 *
	 * @param maxCacheSize maximum number of values in the cache (default is 1000).
	 */
	public abstract AbstractReadWriteCachedCellImgOptions maxCacheSize(final long maxCacheSize);

	/**
	 * Set the dimensions of a cell. This is extended or truncated as necessary. For
	 * example if {@code cellDimensions=[64,32]} then for creating a 3D image it
	 * will be augmented to {@code [64,32,32]}. For creating a 1D image it will be
	 * truncated to {@code [64]}.
	 *
	 * @param cellDimensions dimensions of a cell (default is 10).
	 */
	public abstract AbstractReadWriteCachedCellImgOptions cellDimensions(final int... cellDimensions);

	/**
	 * Specify whether cells initialized by a {@link CellLoader} should be marked as
	 * dirty. It is useful to set this to {@code true} if initialization is a costly
	 * operation. By this, it is made sure that cells are initialized only once, and
	 * then written and retrieve from the disk cache when they are next required.
	 * <p>
	 * This is {@code false} by default.
	 * </p>
	 * <p>
	 * This option only has an effect for {@link DiskCachedCellImg} that are created
	 * with a {@link CellLoader}
	 * ({@link DiskCachedCellImgFactory#create(long[], net.imglib2.type.NativeType, CellLoader)}).
	 * </p>
	 *
	 * @param initializeAsDirty whether cells initialized by a {@link CellLoader}
	 *                          should be marked as dirty.
	 */
	public abstract AbstractReadWriteCachedCellImgOptions initializeCellsAsDirty(final boolean initializeAsDirty);

	/**
	 * Read-only {@link AbstractReadWriteCachedCellImgOptions} values.
	 */
	protected static class Values extends ReadOnlyCachedCellImgOptions.Values {
		/**
		 * Copy constructor.
		 */
		Values(final Values that) {
			super(that);
			this.dirtyAccesses = true; // RW default value differs from readonly cache
			this.numIoThreads = that.numIoThreads;
			this.numIoThreadsModified = that.numIoThreadsModified;
			this.maxIoQueueSize = that.maxIoQueueSize;
			this.maxIoQueueSizeModified = that.maxIoQueueSizeModified;
			this.initializeCellsAsDirty = that.initializeCellsAsDirty;
			this.initializeCellsAsDirtyModified = that.initializeCellsAsDirtyModified;
		}

		Values() {
			super();
			dirtyAccesses = true; // RW default value differs from readonly cache
		}

		Values(final Values base, final Values aug) {
			super(base, aug);
			numIoThreads = aug.numIoThreadsModified ? aug.numIoThreads : base.numIoThreads;
			maxIoQueueSize = aug.maxIoQueueSizeModified ? aug.maxIoQueueSize : base.maxIoQueueSize;
			initializeCellsAsDirty = aug.initializeCellsAsDirtyModified ? aug.initializeCellsAsDirty
					: base.initializeCellsAsDirty;
		}

		protected int numIoThreads = 1;

		protected int maxIoQueueSize = 10;

		protected boolean initializeCellsAsDirty = false;

		public int numIoThreads() {
			return numIoThreads;
		}

		public int maxIoQueueSize() {
			return maxIoQueueSize;
		}

		public boolean initializeCellsAsDirty() {
			return initializeCellsAsDirty;
		}

		protected boolean numIoThreadsModified = false;

		protected boolean maxIoQueueSizeModified = false;

		protected boolean initializeCellsAsDirtyModified = false;

		Values setNumIoThreads(final int n) {
			numIoThreads = n;
			numIoThreadsModified = true;
			return this;
		}

		Values setMaxIoQueueSize(final int n) {
			maxIoQueueSize = n;
			maxIoQueueSizeModified = true;
			return this;
		}

		Values setInitializeCellsAsDirty(final boolean b) {
			initializeCellsAsDirty = b;
			initializeCellsAsDirtyModified = true;
			return this;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append(super.toString());
			sb.append("AbstractReadWriteCachedCellImgOptions = {");

			sb.append("numIoThreads = ");
			sb.append(numIoThreads);
			if (numIoThreadsModified)
				sb.append(" [m]");
			sb.append(", ");

			sb.append("maxIoQueueSize = ");
			sb.append(maxIoQueueSize);
			if (maxIoQueueSizeModified)
				sb.append(" [m]");
			sb.append(", ");

			sb.append("initializeCellsAsDirty = ");
			sb.append(Boolean.toString(initializeCellsAsDirty));
			if (initializeCellsAsDirtyModified)
				sb.append(" [m]");

			sb.append("}");

			return sb.toString();
		}

		@Override
		Values copy() {
			return new Values(this);
		}
	}
}
