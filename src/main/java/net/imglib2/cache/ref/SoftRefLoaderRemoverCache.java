package net.imglib2.cache.ref;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.LoaderRemoverCache;

/**
 * TODO: Consider periodically calling {@link #cleanUp()} from a background
 * thread. Otherwise, freeing memory depends on the cache being regularly used.
 * (This is different for PhantomRefs than for Weak/SoftRefs. PhantomRefs must
 * be explicitly clear()ed before the referent is freed.)
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 * @param <D>
 *            value data type, see {@link CacheRemover}
 *
 * @author Tobias Pietzsch
 */
public class SoftRefLoaderRemoverCache< K, V, D > implements LoaderRemoverCache< K, V, D >
{
	final ConcurrentHashMap< K, Entry > map = new ConcurrentHashMap<>();

	final ReferenceQueue< V > queue = new ReferenceQueue<>();

	static final class CachePhantomReference< V > extends PhantomReference< V >
	{
		final SoftRefLoaderRemoverCache< ?, V, ? >.Entry entry;

		public CachePhantomReference( final V referent, final ReferenceQueue< V > remove, final SoftRefLoaderRemoverCache< ?, V, ? >.Entry entry )
		{
			super( referent, remove );
			this.entry = entry;
		}
	}

	final class Entry
	{
		final K key;

		private SoftReference< V > ref;

		private CachePhantomReference< V > phantomRef;

		private CacheRemover< ? super K, V, D > remover;

		boolean loaded;

		private D valueData;

		public Entry( final K key )
		{
			this.key = key;
			this.ref = new SoftReference<>( null );
			this.phantomRef = null;
			this.remover = null;
			this.loaded = false;
		}

		public V getValue()
		{
			return ref.get();
		}

		public void setValue( final V value, final CacheRemover< ? super K, V, D > remover )
		{
			this.loaded = true;
			this.ref = new SoftReference<>( value );
			this.phantomRef = new CachePhantomReference<>( value, queue, this );
			this.remover = remover;
			this.valueData = remover.extract( value );
		}

		public synchronized void remove()
		{
			if ( remover != null )
			{
				phantomRef.clear(); // TODO: probably not necessary with Java 10 anymore
				phantomRef = null;
				remover.onRemoval( key, valueData );
				remover = null;
				valueData = null;
			}
			map.remove( key, this );
		}
	}

	@Override
	public V getIfPresent( final K key )
	{
		cleanUp();
		final Entry entry = map.get( key );
		return entry == null ? null : entry.getValue();
	}

	@Override
	public V get( final K key, final CacheLoader< ? super K, ? extends V > loader, final CacheRemover< ? super K, V, D > remover ) throws ExecutionException
	{
		cleanUp();
		final Entry entry = map.computeIfAbsent( key, ( k ) -> new Entry( k ) );
		V value = entry.getValue();
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
					}
				}
				else
				{
					try
					{
						value = loader.get( key );
						entry.setValue( value, remover );
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

		if ( value == null )
			value = get( key, loader, remover );

		return value;
	}

	@Override
	public void invalidate( final K key )
	{
		final Entry entry = map.remove( key );
		if ( entry != null )
		{
			synchronized ( entry )
			{
				entry.phantomRef.clear();
				entry.phantomRef = null;
				entry.remover = null;
			}
		}
	}

	@Override
	public void invalidateIf( final long parallelismThreshold, final Predicate< K > condition )
	{
		map.forEachValue( parallelismThreshold, entry ->
		{
			if ( condition.test( entry.key ) )
			{
				synchronized ( entry )
				{
					map.remove( entry.key, entry );
					entry.phantomRef.clear();
					entry.phantomRef = null;
					entry.remover = null;
				}
			}
		} );
	}

	@Override
	public void invalidateAll( final long parallelismThreshold )
	{
		map.forEachValue( parallelismThreshold, entry ->
		{
			synchronized ( entry )
			{
				map.remove( entry.key, entry );
				entry.phantomRef.clear();
				entry.phantomRef = null;
				entry.remover = null;
			}
		} );
	}

	/**
	 * Remove entries from the cache whose references have been
	 * garbage-collected. The {@link CacheRemover} (specified in
	 * {@link #get(Object, CacheLoader, CacheRemover)}), is notified for each
	 * removed entry.
	 */
	public void cleanUp()
	{
		while ( true )
		{
			@SuppressWarnings( "unchecked" )
			final CachePhantomReference< V > pr = ( CachePhantomReference< V > ) queue.poll();
			if ( pr == null )
				break;
			pr.entry.remove();
		}
	}
}
