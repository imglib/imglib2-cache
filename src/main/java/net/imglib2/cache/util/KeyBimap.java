package net.imglib2.cache.util;

import java.util.function.Function;

/**
 * A bidirectional map between key types {@code S} and {@code T} (source and
 * target). This is used to create adapters exposing a {@code T→V} cache as
 * a {@code S→V} cache.
 *
 * @param <S>
 *            source type
 * @param <T>
 *            target type
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface KeyBimap< S, T >
{
	T getTarget( S source );

	S getSource( T target );

	public static < S, T > KeyBimap< S, T > build( final Function< S, T > soureToTarget, final Function< T, S > targetToSource )
	{
		return new KeyBimap< S, T >()
		{
			@Override
			public T getTarget( final S source )
			{
				return soureToTarget.apply( source );
			}

			@Override
			public S getSource( final T target )
			{
				return targetToSource.apply( target );
			}
		};
	}
}
