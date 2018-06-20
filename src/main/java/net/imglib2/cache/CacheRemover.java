package net.imglib2.cache;

import java.util.function.Predicate;

public interface CacheRemover< K, V >
{
	void onRemoval( K key, V value );

	/**
	 * Make a best-effort attempt to clean up {@code key} and the
	 * corresponding value.
	 *
	 * TODO: needs more explanation.
	 * It is surprising to find this method in the CacheRemover interface.
	 */
	default void invalidate( final K key ) {};

	/**
	 * TODO
	 */
	default void invalidateIf( final Predicate< K > condition ) {};

	/**
	 * TODO
	 */
	default void invalidateAll() {};
}
