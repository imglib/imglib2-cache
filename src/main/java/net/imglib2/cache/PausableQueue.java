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
package net.imglib2.cache;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

/**
 * Blocking queue that can be paused. While the queue is paused, all consumer
 * threads are held in {@code take()}. The {@code pause()} method itself blocks
 * until all (of a pre-defined number of) consumer threads have arrived in
 * {@code take()}.
 * <p>
 * While the queue is paused, all calls to {@code put()} and {@code take()}
 * block.
 *
 * @author Tobias Pietzsch
 */
class PausableQueue< E >
{
	private final ArrayDeque< E > elements;

	/** Maximum number of elements in the queue */
	private final int capacity;

	/** Whether the queue is paused */
	private boolean paused;

	/** Number of consumer threads. */
	private final int numConsumers;

	/** Number of consumer threads waiting in {@code take()} */
	private int waitCount;

	/** Main lock guarding all access */
	private final ReentrantLock lock;

	/** Condition for waiting takes */
	private final Condition notEmpty;

	/** Condition for waiting puts */
	private final Condition notFull;

	/** Condition for waiting pause() */
	private final Condition isPaused;

	/**
	 * @param capacity
	 *            the capacity of this queue
	 * @param numConsumers
	 *            how many consumer threads will serve this queue
	 * @param fair
	 *            if {@code true} then queue accesses for threads blocked on
	 *            insertion or removal, are processed in FIFO order; if
	 *            {@code false} the access order is unspecified.
	 */
	public PausableQueue( final int capacity, final int numConsumers, final boolean fair )
	{
		this.capacity = capacity;
		this.numConsumers = numConsumers;

		elements = new ArrayDeque<>( capacity );

		lock = new ReentrantLock( fair );
		notEmpty = lock.newCondition();
		notFull = lock.newCondition();
		isPaused = lock.newCondition();
	}

	public void put( final E e ) throws InterruptedException
	{
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try
		{
			while ( elements.size() >= capacity || paused )
				notFull.await();
			enqueue( e );
		}
		finally
		{
			lock.unlock();
		}
	}

	public void pause() throws InterruptedException
	{
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try
		{
			if ( !paused )
			{
				paused = true;
				while ( waitCount != numConsumers )
					isPaused.await();
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	public void resume() throws InterruptedException
	{
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try
		{
			if ( paused )
			{
				paused = false;
				notFull.signalAll();
				notEmpty.signalAll();
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	public E take() throws InterruptedException
	{
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try
		{
			if ( ++waitCount == numConsumers )
				isPaused.signal();
			while ( elements.size() == 0 || paused )
				notEmpty.await();
			--waitCount;
			return dequeue();
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Call only when holding lock.
	 */
	private void enqueue( final E x )
	{
		elements.offer( x );
		notEmpty.signal();
	}

	/**
	 * Call only when holding lock.
	 */
	private E dequeue()
	{
		final E x = elements.poll();
		notFull.signal();
		return x;
	}

	/**
	 * Remove all occurrences of {@code element}.
	 * <p>
	 * Call only when queue is paused.
	 */
	public void remove( final E element )
	{
		elements.removeIf( e -> e.equals( element ) );
	}

	/**
	 * Remove all elements.
	 * <p>
	 * Call only when queue is paused.
	 */
	public void clear()
	{
		elements.clear();
	}

	/**
	 * Remove all elements matching {@code condition}.
	 * <p>
	 * Call only when queue is paused.
	 */
	public void removeIf( final Predicate< E > condition )
	{
		elements.removeIf( condition::test );
	}
}
