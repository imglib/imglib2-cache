package net.imglib2.cache.util;

import java.util.function.Function;

/**
 * A bidirectional map between key types {@code S} and {@code T} (source and
 * target). This is used to create adapters exposing a {@code T→V} cache as a
 * {@code S→V} cache.
 * <p>
 * The {@code S→T} mapping must be injective. If the {@code S→T} mapping is not
 * bijective, {@link #getSource(Object)} should return {@code null} for target
 * keys with no corresponding source key.
 *
 * @param <S>
 *            source type
 * @param <T>
 *            target type
 *
 * @author Tobias Pietzsch
 */
public interface KeyBimap< S, T >
{
	/**
	 * Returns target key for a given source key.
	 *
	 * @param source
	 *            source key
	 * @return the target key corresponding to {@code source}
	 */
	T getTarget( S source );

	/**
	 * Returns source key source key mapping to a given target key, or
	 * {@code null} if there is no such source key.
	 *
	 * @param target
	 *            target key
	 * @return the source key mapping to {@code target}, or {@code null} if
	 *         there is no such source key.
	 */
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
