package net.imglib2.cache.util;

import java.util.function.Predicate;

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
	public void invalidate( final K key )
	{
		// TODO
		throw new UnsupportedOperationException( "not implemented yet" );
	}

	@Override
	public void invalidateIf( final Predicate< K > condition )
	{
		// TODO
		throw new UnsupportedOperationException( "not implemented yet" );
	}

	@Override
	public void invalidateAll()
	{
		cache.invalidateAll();
	}
}
