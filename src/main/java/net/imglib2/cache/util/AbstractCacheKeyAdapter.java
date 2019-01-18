package net.imglib2.cache.util;

import java.util.function.Predicate;

import net.imglib2.cache.AbstractCache;

/**
 * Wraps a {@code Cache<L,V>} as a {@code Cache<K,V>}, using a
 * {@code KeyBimap<K,L>} to translate keys.
 *
 * @param <K>
 *            key type of this cache
 * @param <L>
 *            key type of wrapped cache
 * @param <V>
 *            value type (of both of this cache and the wrapped cache)
 * @param <C>
 *            wrapped cache type
 *
 * @author Tobias Pietzsch
 */
public class AbstractCacheKeyAdapter< K, L, V, C extends AbstractCache< L, V > >
		implements AbstractCache< K, V >
{
	protected final C cache;

	protected final KeyBimap< K, L > keymap;

	public AbstractCacheKeyAdapter( final C cache, final KeyBimap< K, L > keymap )
	{
		this.cache = cache;
		this.keymap = keymap;
	}

	@Override
	public V getIfPresent( final K key )
	{
		return cache.getIfPresent( keymap.getTarget( key ) );
	}

	@Override
	public void invalidate( final K key )
	{
		cache.invalidate( keymap.getTarget( key ) );
	}

	@Override
	public void invalidateIf( final Predicate< K > condition )
	{
		cache.invalidateIf( l ->
		{
			final K k = keymap.getSource( l );
			return k != null && condition.test( k );
		} );
	}

	@Override
	public void invalidateAll()
	{
		cache.invalidateIf( l -> keymap.getSource( l ) != null );
	}
}
