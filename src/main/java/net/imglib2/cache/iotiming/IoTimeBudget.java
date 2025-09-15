/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2025 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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
package net.imglib2.cache.iotiming;

/**
 * Budget of time that can be spent in blocking IO. The budget is grouped by
 * priority levels, where level 0 is the highest priority. The budget for
 * level <em>i &gt; j</em> must always be smaller-equal the budget for level
 * <em>j</em>.
 *
 * For BDV, the time unit of {@link IoTimeBudget} values is nanoseconds.
 */
public class IoTimeBudget
{
	private long[] budget;

	public IoTimeBudget()
	{
		budget = new long[] { 0 };
	}

	/**
	 * (Re-)initialize the IO time budget, that is, the time that can be spent
	 * in blocking IO.
	 *
	 * @param partialBudget
	 *            Initial budget for priority levels <em>0</em> through
	 *            <em>n</em>. The budget for level <em>i&gt;j</em> must always
	 *            be smaller-equal the budget for level <em>j</em>.
	 */
	public synchronized void reset( final long[] partialBudget )
	{
		if ( partialBudget == null || partialBudget.length == 0 )
			clear();
		else
		{
			if ( partialBudget.length == budget.length )
				System.arraycopy( partialBudget, 0, budget, 0, budget.length );
			else
				budget = partialBudget.clone();

			for ( int i = 1; i < budget.length; ++i )
				if ( budget[ i ] > budget[ i - 1 ] )
					budget[ i ] = budget[ i - 1 ];
		}
	}

	/**
	 * Set the budget to 0 (for all levels).
	 */
	public synchronized void clear()
	{
		for ( int i = 0; i < budget.length; ++i )
			budget[ i ] = 0;
	}

	/**
	 * Returns how much time is left for the specified priority level.
	 *
	 * @param level
	 *            priority level. must be greater &ge; 0.
	 * @return time left for the specified priority level.
	 */
	public synchronized long timeLeft( final int level )
	{
		final int blevel = Math.min( level, budget.length - 1 );
		return budget[ blevel ];
	}

	/**
	 * Use the specified amount of time of the specified level.
	 *
	 * {@code t} is subtracted from the budgets of level {@code level} and
	 * smaller. If by this, the remaining budget of {@code level} becomes
	 * smaller than the remaining budget of {@code level + 1}, then this is
	 * reduced too. (And the same for higher levels.)
	 *
	 * @param t
	 *            how much time to use.
	 * @param level
	 *            priority level. must be greater &ge; 0.
	 */
	public synchronized void use( final long t, final int level )
	{
		final int blevel = Math.min( level, budget.length - 1 );
		int l = 0;
		for ( ; l <= blevel; ++l )
			budget[ l ] -= t;
		for ( ; l < budget.length && budget[ l ] > budget[ l - 1 ]; ++l )
			budget[ l ] = budget[ l - 1 ];
	}

	/**
	 * Returns how much time is left for the specified priority level.
	 *
	 * @param level
	 *            priority level. must be greater &ge; 0.
	 * @return time left for the specified priority level.
	 */
	public long estimateTimeLeft( final int level )
	{
		final long[] b = budget;
		final int blevel = Math.min( level, b.length - 1 );
		return b[ blevel ];
	}
}
