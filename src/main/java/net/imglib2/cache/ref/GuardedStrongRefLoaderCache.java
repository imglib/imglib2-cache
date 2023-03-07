/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2023 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.imglib2.cache.ref;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderCache;

/**
 * A {@link LoaderCache} that is backed by a cache with strong references to
 * values. At the moment the backing cache is we use is
 * <a href="https://github.com/ben-manes/caffeine">caffeine</a>. We can easily
 * add Guava, cache2k, etc options later.
 * <p>
 * In addition we keep weak references to values. This ensures that
 * we never lose track of values that are still in use elsewhere, although they
 * have been evicted from the backing cache. Thus we never end up in a situation
 * where two distinct values are associated to the same key (associated
 * conceptually by the application, not by the cache map).
 * </p>
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 *
 * @author Tobias Pietzsch
 */
public class GuardedStrongRefLoaderCache< K, V > implements LoaderCache< K, V >
{
	final ConcurrentHashMap< K, Entry > map = new ConcurrentHashMap<>();

	final ReferenceQueue< V > queue = new ReferenceQueue<>();

	final Cache< K, V > strongCache;

	static final class CacheWeakReference< V > extends WeakReference< V >
	{
		private final GuardedStrongRefLoaderCache< ?, V >.Entry entry;

		public CacheWeakReference()
		{
			super( null );
			this.entry = null;
		}

		public CacheWeakReference( final V referent, final ReferenceQueue< V > remove, final GuardedStrongRefLoaderCache< ?, V >.Entry entry )
		{
			super( referent, remove );
			this.entry = entry;
		}
	}

	final class Entry
	{
		final K key;

		private CacheWeakReference< V > ref;

		boolean loaded;

		public Entry( final K key )
		{
			this.key = key;
			this.ref = new CacheWeakReference<>();
			this.loaded = false;
		}

		public V getValue()
		{
			return ref.get();
		}

		public void setValue( final V value )
		{
			this.loaded = true;
			this.ref = new CacheWeakReference<>( value, queue, this );
		}

		public void remove()
		{
			map.remove( key, this );
		}
	}

	public GuardedStrongRefLoaderCache( final long maximumSize )
	{
		strongCache = Caffeine.newBuilder().maximumSize( maximumSize ).build();
	}

	@Override
	public V getIfPresent( final K key )
	{
		cleanUp();
		final V value = strongCache.getIfPresent( key );
		if ( value != null )
			return value;
		final Entry entry = map.get( key );
		return entry == null ? null : entry.getValue();
	}

	@Override
	public V get( final K key, final CacheLoader< ? super K, ? extends V > loader ) throws ExecutionException
	{
		cleanUp();
		V value = strongCache.getIfPresent( key );
		if ( value != null )
			return value;
		final Entry entry = map.computeIfAbsent( key, ( k ) -> new Entry( k ) );
		value = entry.getValue();
		if ( value == null )
		{
			synchronized ( entry )
			{
				if ( entry.loaded )
				{
					value = entry.getValue();
					if ( value == null )
					{
						/*
						 * The entry was already loaded, but its value has been
						 * garbage collected. We need to create a new entry
						 */
						entry.remove();
						value = get( key, loader );
					}
				}
				else
				{
					try
					{
						value = loader.get( key );
						entry.setValue( value );
						strongCache.put( key, value );
					}
					catch ( final InterruptedException e )
					{
						Thread.currentThread().interrupt();
						throw new ExecutionException( e );
					}
					catch ( final Exception e )
					{
						throw new ExecutionException( e );
					}
				}
			}
		}
		return value;
	}

	@Override
	public void persist( final K key )
	{}

	@Override
	public void persistIf( final Predicate< K > condition )
	{}

	@Override
	public void persistAll()
	{}

	@Override
	public void invalidate( final K key )
	{
		final Entry entry = map.remove( key );
		if ( entry != null )
		{
			strongCache.invalidate( key );
			final CacheWeakReference< V > ref = entry.ref;
			if ( ref != null )
				ref.clear();
			entry.ref = null;
		}
	}

	@Override
	public void invalidateIf( final long parallelismThreshold, final Predicate< K > condition )
	{
		map.forEachValue( parallelismThreshold, entry ->
		{
			if ( condition.test( entry.key ) )
			{
				strongCache.invalidate( entry.key );
				entry.remove();
				final CacheWeakReference< V > ref = entry.ref;
				if ( ref != null )
					ref.clear();
				entry.ref = null;
			}
		} );
	}

	@Override
	public void invalidateAll( final long parallelismThreshold )
	{
		// TODO: We could also simply do map.clear(). Pros/Cons?

		map.forEachValue( parallelismThreshold, entry ->
		{
			entry.remove();
			final CacheWeakReference< V > ref = entry.ref;
			if ( ref != null )
				ref.clear();
			entry.ref = null;
		} );
		strongCache.invalidateAll();
	}

	/**
	 * Remove entries from the cache whose references have been
	 * garbage-collected.
	 */
	public void cleanUp()
	{
		while ( true )
		{
			@SuppressWarnings( "unchecked" )
			final CacheWeakReference< V > poll = ( CacheWeakReference< V > ) queue.poll();
			if ( poll == null )
				break;
			poll.entry.remove();
		}
	}
}
