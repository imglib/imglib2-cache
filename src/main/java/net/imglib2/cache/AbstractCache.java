package net.imglib2.cache;

import java.util.function.Predicate;

public interface AbstractCache< K, V > extends Invalidate< K >
{
	V getIfPresent( K key );

	/**
	 * Removes and discards the entry with the specified {@code key}.
	 * <p>
	 * Note that this will <em>not</em> call
	 * {@link CacheRemover#onRemoval(Object, Object)} for the discarded entry.
	 * <p>
	 * If this cache has a {@code CacheRemover}, calls {@link CacheRemover#invalidate(Object)} for the discarded
	 * entry, (which should in turn remove it from any backing cache).
	 * <em>Note that this applies only when the whole cache has a {@code CacheRemover},
	 * instead of each individual entry as in {@link LoaderRemoverCache}.</em>
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
	 * <p>
	 * Note that this will <em>not</em> call
	 * {@link CacheRemover#onRemoval(Object, Object)} for the discarded entries.
	 * <p>
	 * If this cache has a {@code CacheRemover}, calls {@link CacheRemover#invalidateIf(Predicate)} for the discarded
	 * entries, (which should in turn remove them from any backing cache).
	 * <em>Note that this applies only when the whole cache has a {@code CacheRemover},
	 * instead of each individual entry as in {@link LoaderRemoverCache}.</em>
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
	 * Removes and discards all entries.
	 * <p>
	 * Note that this will <em>not</em> call
	 * {@link CacheRemover#onRemoval(Object, Object)} for the discarded entries.
	 * <p>
	 * If this cache has a {@code CacheRemover}, calls {@link CacheRemover#invalidateAll(long)}
	 * (which should in turn invalidate any backing cache).
	 * <em>Note that this applies only when the whole cache has a {@code CacheRemover},
	 * instead of each individual entry as in {@link LoaderRemoverCache}.</em>
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
