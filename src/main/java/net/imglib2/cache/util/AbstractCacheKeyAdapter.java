package net.imglib2.cache.util;

import net.imglib2.cache.AbstractCache;

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
	public void invalidateAll()
	{
		cache.invalidateAll();
	}
}
