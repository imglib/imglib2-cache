package net.imglib2.cache;

import java.util.function.Predicate;

public interface AbstractCache< K, V >
{
	V getIfPresent( K key );

	/**
	 * Make a best-effort attempt to remove the {@code key} and discard the
	 * corresponding cached value.
	 * <p>
	 * This will <em>not</em> call
	 * {@link CacheRemover#onRemoval(Object, Object)} for the discarded entry.
	 * <p>
	 * <em>There must be no concurrent {@code get()} operations for {@code key}.
	 * This will result in cache corruption and/or a deadlock.</em>
	 */
	void invalidate( final K key );

	/**
	 * Make a best-effort attempt to remove and discard all cached values with
	 * keys matching {@code condition}.
	 * <p>
	 * This will <em>not</em> call
	 * {@link CacheRemover#onRemoval(Object, Object)} for the discarded entries.
	 * <p>
	 * <em>There must be no concurrent {@code get()} operations for keys
	 * matching {@code condition}. This will result in cache corruption and/or a
	 * deadlock.</em>
	 */
	void invalidateIf( final Predicate< K > condition );

	/**
	 * Make a best-effort attempt to remove and discard all cached values.
	 * <p>
	 * This will <em>not</em> call
	 * {@link CacheRemover#onRemoval(Object, Object)} for the discarded entries.
	 * <p>
	 * <em> There must be no concurrent {@code get()} operations. This will
	 * result in cache corruption and/or a deadlock.</em>
	 */
	void invalidateAll();

//	void cleanUp();
//	long size();
}
