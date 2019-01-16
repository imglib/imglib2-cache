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
public interface CacheRemover< K, V >
{
	/**
	 * Called when an entry is evicted from the cache.
	 *
	 * @param key
	 * @param value
	 */
	void onRemoval( K key, V value );

	/**
	 * Called when a specific entry is invalidated from the cache. (See
	 * {@link AbstractCache#invalidate(Object)}.)
	 * <p>
	 * If this {@code CacheRemover} itself represents a (higher-level) cache,
	 * the entry should be invalidated in this cache as well.
	 */
	default void invalidate( final K key )
	{};

	/**
	 * Called when all entries with keys matching {@code condition} are
	 * invalidated from the cache. (See
	 * {@link AbstractCache#invalidateIf(Predicate)}.)
	 * <p>
	 * If this {@code CacheRemover} itself represents a (higher-level) cache,
	 * the entries should be invalidated in this cache as well.
	 */
	default void invalidateIf( final Predicate< K > condition )
	{};

	/**
	 * Called when all entries are invalidated from the cache. (See
	 * {@link AbstractCache#invalidateAll()}.)
	 * <p>
	 * If this {@code CacheRemover} itself represents a (higher-level) cache,
	 * the entries should be invalidated in this cache as well.
	 */
	default void invalidateAll()
	{};
}
