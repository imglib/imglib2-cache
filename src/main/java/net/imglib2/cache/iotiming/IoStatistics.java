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
package net.imglib2.cache.iotiming;

import java.util.concurrent.ConcurrentHashMap;

import net.imglib2.util.StopWatch;

public class IoStatistics
{
	private final ConcurrentHashMap< Thread, StopWatch > perThreadStopWatches = new ConcurrentHashMap<>();

	private final StopWatch stopWatch;

	private int numRunningThreads;

	private long ioBytes;

	private final IoTimeBudget ioTimeBudget;

	public IoStatistics()
	{
		stopWatch = StopWatch.createStopped();
		ioBytes = 0;
		numRunningThreads = 0;
		ioTimeBudget = new IoTimeBudget();
	}

	public synchronized void start()
	{
		getThreadStopWatch().start();
		if( numRunningThreads++ == 0 )
			stopWatch.start();
	}

	public synchronized void stop()
	{
		getThreadStopWatch().stop();
		if( --numRunningThreads == 0 )
			stopWatch.stop();
	}

	public void incIoBytes( final long n )
	{
		ioBytes += n;
	}

	public long getIoBytes()
	{
		return ioBytes;
	}

	public long getIoNanoTime()
	{
		return stopWatch.nanoTime();
	}

	public long getCumulativeIoNanoTime()
	{
		long sum = 0;
		for ( final StopWatch w : perThreadStopWatches.values() )
			sum += w.nanoTime();
		return sum;
	}

	public IoTimeBudget getIoTimeBudget()
	{
		return ioTimeBudget;
	}

	private StopWatch getThreadStopWatch()
	{
		return perThreadStopWatches.computeIfAbsent( Thread.currentThread(), k -> StopWatch.createStopped() );
	}
}
