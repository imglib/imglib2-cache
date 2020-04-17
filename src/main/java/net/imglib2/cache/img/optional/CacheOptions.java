package net.imglib2.cache.img.optional;

import java.lang.ref.SoftReference;
import org.scijava.optional.AbstractValues.ValuesToString;
import org.scijava.optional.Options;
import org.scijava.optional.Values;

/**
 * Optional arguments that specify the in memory-cache to use.
 */
public interface CacheOptions< T extends CacheOptions< T > > extends Options< T >
{
	/**
	 * Which in-memory cache type to use. The options are
	 * <ul>
	 * <li>{@link CacheType#SOFTREF SOFTREF}: The cache keeps SoftReferences to
	 * values (cells), basically relying on GC for removal. The advantage of
	 * this is that many caches can be created without needing to put a limit on
	 * the size of any of them. GC will take care of balancing that. The
	 * downside is that {@link OutOfMemoryError} may occur because
	 * {@link SoftReference}s are cleared too slow. SoftReferences are not
	 * collected for a certain time after they have been used. If there is heavy
	 * thrashing with cells being constantly swapped in and out from disk then
	 * OutOfMemory may happen because of this. This sounds worse than it is in
	 * practice and should only happen in pathological situations. Tuning the
	 * {@code -XX:SoftRefLRUPolicyMSPerMB} JVM flag does often help.</li>
	 * <li>{@link CacheType#BOUNDED BOUNDED}: The cache keeps strong references
	 * to a limited number of values (cells). The advantage is that there is
	 * never OutOfMemory because of the issues described above (fingers
	 * crossed). The downside is that the number of cells that should be cached
	 * needs to be specified beforehand. So {@link OutOfMemoryError} may occur
	 * if many caches are opened and consume too much memory in total.</li>
	 * </ul>
	 *
	 * @param cacheType
	 *            which cache type to use (default is {@code SOFTREF}).
	 */
	default T cacheType( final CacheType cacheType )
	{
		return add( "cacheType", cacheType );
	}

	/**
	 * Set the maximum number of values (cells) to keep in the cache. This is
	 * only used if {@link #cacheType(CacheType)} is {@link CacheType#BOUNDED}.
	 *
	 * @param maxCacheSize
	 *            maximum number of values in the cache (default is 1000).
	 */
	default T maxCacheSize( final long maxCacheSize )
	{
		return add( "maxCacheSize", maxCacheSize );
	}

	/**
	 * Rough in-memory cache types.
	 *
	 * @author Tobias Pietzsch
	 */
	enum CacheType
	{
		/**
		 * The cache keeps SoftReferences to values (cells), basically relying
		 * on GC for removal. The advantage of this is that many caches can be
		 * created without needing to put a limit on the size of any of them. GC
		 * will take care of balancing that. The downside is that
		 * {@link OutOfMemoryError} may occur because {@link SoftReference}s are
		 * cleared too slow. SoftReferences are not collected for a certain time
		 * after they have been used. If there is heavy thrashing with cells
		 * being constantly swapped in and out from disk then OutOfMemory may
		 * happen because of this. This sounds worse than it is in practice and
		 * should only happen in pathological situations. Tuning the
		 * {@code -XX:SoftRefLRUPolicyMSPerMB} JVM flag does often help.
		 */
		SOFTREF,

		/**
		 * The cache keeps strong references to a limited number of values
		 * (cells). The advantage is that there is never OutOfMemory because of
		 * the issues described above (fingers crossed). The downside is that
		 * the number of cells that should be cached needs to be specified
		 * beforehand. So {@link OutOfMemoryError} may occur if many caches are
		 * opened and consume too much memory in total.
		 */
		BOUNDED
	}

	interface Val extends Values
	{
		default void buildToString( ValuesToString sb )
		{
			sb.append( "cacheType", cacheType() );
			sb.append( "maxCacheSize", maxCacheSize() );
		}

		default CacheType cacheType()
		{
			return value( "cacheType", CacheType.SOFTREF );
		}

		default long maxCacheSize()
		{
			return value( "maxCacheSize", 1000L );
		}
	}
}
