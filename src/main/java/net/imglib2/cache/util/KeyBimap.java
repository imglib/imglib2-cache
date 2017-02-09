package net.imglib2.cache.util;

import java.util.function.Function;

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
