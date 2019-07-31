package net.imglib2.cache;

import java.util.function.Predicate;

/**
 * Handles entries that are removed from a cache (by propagating to a
 * higher-level cache, writing to disk, or similar).
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 *
 * @author Tobias Pietzsch
 */
public interface CacheRemover< K, V > extends Invalidate< K >
{
	/**
	 * Called when an entry is evicted from the cache.
	 *
	 * @param key
	 *            key of the entry to remove
	 * @param valueData
	 *            value data of the entry to remove
	 */
	void onRemoval( K key, V value );

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
