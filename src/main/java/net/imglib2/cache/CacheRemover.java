package net.imglib2.cache;

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
