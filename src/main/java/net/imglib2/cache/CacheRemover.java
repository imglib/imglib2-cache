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
package net.imglib2.cache;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Handles entries that are removed from a cache (by propagating to a
 * higher-level cache, writing to disk, or similar).
 * <p>
 * To make this work, it must be possible to {@link #extract(Object) extract}
 * data {@code D} out of a value {@code V}, and to
 * {@link #reconstruct(Object, Object) reconstruct} an identical value {@code V}
 * from {@code D}. {@code D} must be sufficient to reconstruct an identical
 * {@code V}, and must not contain any references to {@code V}.
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 * @param <D>
 *            value data type
 *
 * @author Tobias Pietzsch
 */
public interface CacheRemover< K, V, D > extends Invalidate< K >
{
	/**
	 * Called when an entry is evicted from the cache.
	 *
	 * @param key
	 *            key of the entry to remove
	 * @param valueData
	 *            value data of the entry to remove
	 */
	void onRemoval( K key, D valueData );

	/**
	 * TODO
	 *
	 * @param key
	 * @param valueData
	 */
	CompletableFuture< Void > persist( K key, D valueData );

	/**
	 * Extract data out of {@code value}. The data must be sufficient to
	 * reconstruct an identical value, and must not contain any references to
	 * value.
	 */
	D extract( V value );

	/**
	 * Construct a value from its {@code key} and {@code valueData}.
	 */
	V reconstruct( K key, D valueData );

	/**
	 * Called when a specific entry is invalidated from the cache. (See
	 * {@link AbstractCache#invalidate(Object)}.)
	 * <p>
	 * If this {@code CacheRemover} itself represents a (higher-level) cache,
	 * the entry should be invalidated in this cache as well.
	 *
	 * @param key
	 *            key of the entry to remove
	 */
	@Override
	default void invalidate( final K key )
	{};

	/**
	 * Called when all entries with keys matching {@code condition} are
	 * invalidated from the cache. (See
	 * {@link AbstractCache#invalidateIf(long, Predicate)}.)
	 * <p>
	 * If this {@code CacheRemover} itself represents a (higher-level) cache,
	 * the entries should be invalidated in this cache as well.
	 *
	 * @param parallelismThreshold
	 *            the (estimated) number of entries in the cache needed for this
	 *            operation to be executed in parallel
	 * @param condition
	 *            condition on keys of entries to remove
	 */
	@Override
	default void invalidateIf( final long parallelismThreshold, final Predicate< K > condition )
	{};

	/**
	 * Called when all entries are invalidated from the cache. (See
	 * {@link AbstractCache#invalidateAll(long)}.)
	 * <p>
	 * If this {@code CacheRemover} itself represents a (higher-level) cache,
	 * the entries should be invalidated in this cache as well.
	 *
	 * @param parallelismThreshold
	 *            the (estimated) number of entries in the cache needed for this
	 *            operation to be executed in parallel
	 */
	@Override
	default void invalidateAll( final long parallelismThreshold )
	{};
}
