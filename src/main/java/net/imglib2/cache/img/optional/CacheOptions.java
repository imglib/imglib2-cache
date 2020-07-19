/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2020 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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
package net.imglib2.cache.img.optional;

import java.lang.ref.SoftReference;
import java.util.function.BiConsumer;
import org.scijava.optional.Options;
import org.scijava.optional.Values;

/**
 * Optional arguments that specify the in memory-cache to use.
 */
public interface CacheOptions< T > extends Options< T >
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
		return setValue( "cacheType", cacheType );
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
		return setValue( "maxCacheSize", maxCacheSize );
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
		default void forEach( BiConsumer< String, Object > action )
		{
			action.accept( "cacheType", cacheType() );
			action.accept( "maxCacheSize", maxCacheSize() );
		}

		default CacheType cacheType()
		{
			return getValueOrDefault( "cacheType", CacheType.SOFTREF );
		}

		default long maxCacheSize()
		{
			return getValueOrDefault( "maxCacheSize", 1000L );
		}
	}
}
