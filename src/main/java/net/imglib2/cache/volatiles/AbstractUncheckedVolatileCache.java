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
package net.imglib2.cache.volatiles;

import java.util.function.Predicate;

import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.Invalidate;

public interface AbstractUncheckedVolatileCache< K, V > extends Invalidate< K >
{
	V getIfPresent(
			K key,
			CacheHints cacheHints );

	/**
	 * Removes and discards the entry with the specified {@code key}. Calls
	 * {@link CacheRemover#invalidate(Object)} for the discarded entry, (which
	 * should in turn remove it from any backing cache)
	 * <p>
	 * Note that this will <em>not</em> call
	 * {@link CacheRemover#onRemoval(Object, Object)} for the discarded entry.
	 * <p>
	 * <em>There must be no concurrent {@code get()} operations for {@code key}.
	 * This may result in cache corruption and/or a deadlock.</em>
	 *
	 * @param key
	 *            key of the entry to remove
	 */
	@Override
	void invalidate( final K key );

	/**
	 * Removes and discards all entries with keys matching {@code condition}.
	 * Calls {@link CacheRemover#invalidateIf(Predicate)} for the discarded
	 * entries, (which should in turn remove them from any backing cache)
	 * <p>
	 * Note that this will <em>not</em> call
	 * {@link CacheRemover#onRemoval(Object, Object)} for the discarded entries.
	 * <p>
	 * <em>There must be no concurrent {@code get()} operations for keys
	 * matching {@code condition}. This may result in cache corruption and/or a
	 * deadlock.</em>
	 *
	 * @param parallelismThreshold
	 *            the (estimated) number of entries in the cache needed for this
	 *            operation to be executed in parallel
	 * @param condition
	 *            condition on keys of entries to remove
	 */
	@Override
	void invalidateIf( final long parallelismThreshold, final Predicate< K > condition );

	/**
	 * Removes and discards all entries. Calls
	 * {@link CacheRemover#invalidateAll()} (which should in turn invalidate any
	 * backing cache)
	 * <p>
	 * Note that this will <em>not</em> call
	 * {@link CacheRemover#onRemoval(Object, Object)} for the discarded entries.
	 * <p>
	 * <em> There must be no concurrent {@code get()} operations. This may
	 * result in cache corruption and/or a deadlock.</em>
	 *
	 * @param parallelismThreshold
	 *            the (estimated) number of entries in the cache needed for this
	 *            operation to be executed in parallel
	 */
	@Override
	void invalidateAll( final long parallelismThreshold );
}
