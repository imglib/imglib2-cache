/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2025 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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
package net.imglib2.cache;

import java.util.function.Predicate;

/**
 * A collection of methods that invalidate cache entries. It is possible to
 * invalidate everything, entries matching some condition, or an individual
 * entry.
 * <p>
 * <em>"Invalidate"</em> means: Remove from the cache, and discard any changes
 * that have been made in this cache or any backing cache. In particular, if
 * changes have been persisted to disk, remove those as well.
 * <p>
 * This interface is extended both by the caches and by {@code CacheRemover}.
 * See {@link AbstractCache} and {@link CacheRemover}, for more specific
 * documentation for each case.
 *
 * @param <K>
 *            key type (identifies entries to remove)
 *
 * @author Tobias Pietzsch
 */
public interface Invalidate< K >
{
	/**
	 * Removes and discards the entry with the specified {@code key}.
	 * <p>
	 * See {@link AbstractCache#invalidate(Object)},
	 * {@link CacheRemover#invalidate(Object)}.
	 *
	 * @param key
	 *            key of the entry to remove
	 */
	void invalidate( final K key );

	/**
	 * Removes and discards all entries with keys matching {@code condition}.
	 * <p>
	 * See {@link AbstractCache#invalidateIf(long, Predicate)},
	 * {@link CacheRemover#invalidateIf(long, Predicate)}.
	 *
	 * @param condition
	 *            condition on keys of entries to remove
	 * @param parallelismThreshold
	 *            the (estimated) number of entries in the cache needed for this
	 *            operation to be executed in parallel
	 */
	void invalidateIf( final long parallelismThreshold, final Predicate< K > condition );

	/**
	 * Removes and discards all entries.
	 * <p>
	 * See {@link AbstractCache#invalidateAll(long)},
	 * {@link CacheRemover#invalidateAll(long)}.
	 *
	 * @param parallelismThreshold
	 *            the (estimated) number of entries in the cache needed for this
	 *            operation to be executed in parallel
	 */
	void invalidateAll( final long parallelismThreshold );

	/**
	 * Removes and discards all entries with keys matching {@code condition}.
	 * (Calls {@link #invalidateIf(long, Predicate)} with
	 * {@link #defaultParallelismThreshold}.)
	 *
	 * @param condition
	 *            condition on keys of entries to remove
	 */
	default void invalidateIf( final Predicate< K > condition )
	{
		invalidateIf( defaultParallelismThreshold, condition );
	}

	/**
	 * Removes and discards all entries.
	 * (Calls {@link #invalidateAll(long)} with {@link #defaultParallelismThreshold}.)
	 */
	default void invalidateAll()
	{
		invalidateAll( defaultParallelismThreshold );
	}

	/**
	 * TODO: Should do some benchmarks. This is picked completely arbitrarily.
	 */
	long defaultParallelismThreshold = 5000;
}
