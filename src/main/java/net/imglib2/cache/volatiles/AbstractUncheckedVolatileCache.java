package net.imglib2.cache.volatiles;

import java.util.function.Predicate;
import net.imglib2.cache.CacheRemover;

public interface AbstractUncheckedVolatileCache< K, V >
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
	 */
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
	 */
	void invalidateIf( final Predicate< K > condition );

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
	 */
	void invalidateAll();
}
