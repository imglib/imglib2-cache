/*
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
package net.imglib2.cache.queue;

import java.util.ArrayDeque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TODO revise javadoc
 *
 * Queueing structure (intended for cache entries to be fetched). There is an
 * array of {@link ArrayDeque}s, ordered by priority. Elements are
 * {@link #put(Object, int, boolean)} with a priority and added to one of the
 * queues, accordingly. {@link #take()} returns an element from the highest
 * priority non-empty queue. Furthermore, there is a prefetch deque of bounded
 * size to provides elements when all the queues are exhausted. {@link #clearToPrefetch()}
 * empties all queues, and moves the removed elements to the prefetch queue.
 * <p>
 * Locking is adapted from {@link ArrayBlockingQueue}.
 * <p>
 * {@link BlockingFetchQueues} is constructed with the number of priority levels
 * <em>n</em>. Priorities are consecutive integers <em>0 ... n-1</em>, where 0
 * is the highest priority. Priorities of {@link #put(Object, int, boolean)
 * enqueued} entries are clamped to the range <em>0 ... n-1</em>.
 *
 * @param <E>
 *            element type.
 *
 * @author Tobias Pietzsch
 */
public class BlockingFetchQueues< E >
{
	private final ArrayDeque< E >[] queues;

	private final int maxPriority;

	private final int prefetchCapacity;

	private final ArrayDeque< E > prefetch;

	/** Number of elements in the queue */
	private int count;

	/** Whether the queue is paused */
	private boolean paused;

	/** Number of consumer threads. */
	private final int numConsumers;

	/** Number of consumer threads waiting in {@code take()} */
	private int waitCount;

	/** Main lock guarding all accesses */
	private final ReentrantLock lock;

	/** Condition for waiting take()s */
	private final Condition notEmpty;

	/** Condition for waiting pause() */
	private final Condition isPaused;

	/** incremented with every {@link #clearToPrefetch()} call */
	private volatile long currentFrame = 0;

	public BlockingFetchQueues( final int numPriorities, final int numConsumers )
	{
		this( numPriorities, numConsumers, 16384 );
	}

	@SuppressWarnings( "unchecked" )
	public BlockingFetchQueues( final int numPriorities, final int numConsumers, final int prefetchCapacity )
	{
		if ( numPriorities < 1 )
			throw new IllegalArgumentException( "expected numPriorities >= 1" );
		queues = new ArrayDeque[ numPriorities ];
		maxPriority = numPriorities - 1;
		for ( int i = 0; i < numPriorities; ++i )
			queues[ i ] = new ArrayDeque<>();

		this.numConsumers = numConsumers;

		this.prefetchCapacity = prefetchCapacity;
		prefetch = new ArrayDeque<>( prefetchCapacity );

		lock = new ReentrantLock();
		notEmpty = lock.newCondition();
		isPaused = lock.newCondition();
	}

	/**
	 * Add element to the queue of the specified priority. The element can be
	 * added to the front or back of the queue.
	 *
	 * @param element
	 *            the element to enqueue
	 * @param priority
	 *            lower values mean higher priority
	 * @param enqueuToFront
	 *            if true, enqueue element at the front (LIFO). if false,
	 *            enqueue element at the back (FIFO)
	 */
	public void put( final E element, final int priority, final boolean enqueuToFront )
	{
		put_unsafe( element, Math.max( Math.min( priority, maxPriority ), 0 ), enqueuToFront );
	}

	/**
	 * Equivalent to {@link #put(Object, int, boolean)}, but priorities are not
	 * clamped to the allowed range. Will throw an
	 * {@link ArrayIndexOutOfBoundsException} if {@code priority < 0} or
	 * {@code priority >= numPriorities}.
	 * <p>
	 * Add element to the queue of the specified priority. The element can be
	 * added to the front or back of the queue.
	 *
	 * @param element
	 *            the element to enqueue
	 * @param priority
	 *            lower values mean higher priority
	 * @param enqueuToFront
	 *            if true, enqueue element at the front (LIFO). if false,
	 *            enqueue element at the back (FIFO)
	 */
	public void put_unsafe( final E element, final int priority, final boolean enqueuToFront )
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			if ( enqueuToFront )
				queues[ priority ].addFirst( element );
			else
				queues[ priority ].add( element );
			++count;
			notEmpty.signal();
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Remove and return an element from the highest priority non-empty queue. If all
	 * queues are empty, then return an element from the prefetch deque. If the
	 * prefetch deque is also empty, then block.
	 *
	 * @return element.
	 * @throws InterruptedException
	 */
	public E take() throws InterruptedException
	{
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try
		{
			if ( ++waitCount == numConsumers )
				isPaused.signal();
			while ( count == 0 || paused )
				notEmpty.await();
			--waitCount;
			--count;
			for ( final ArrayDeque< E > q : queues )
				if ( !q.isEmpty() )
					return q.remove();
			return prefetch.poll();
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Pause the queue. While the queue is paused, all consumer threads are held
	 * in {@code take()}. The {@code pause()} method itself blocks until all (of
	 * the pre-defined number of) consumer threads have arrived in
	 * {@code take()}.
	 * <p>
	 * While the queue is paused, all calls to {@code take()} block. (Calls to
	 * {@code put()} are not affected.)
	 *
	 * @throws InterruptedException
	 */
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
				notEmpty.signalAll();
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Atomically removes all of the elements from this queue. All queues will
	 * be empty after this call returns. Removed elements are moved to the
	 * {@link #prefetch} deque.
	 */
	public void clearToPrefetch()
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			++currentFrame;

//			System.out.println( "prefetch size before clear = " + prefetch.size() );

			// make room in the prefetch deque
			final int toRemoveFromPrefetch = Math.max( 0, Math.min( prefetch.size(), count - prefetchCapacity ) );
//			System.out.println( "toRemoveFromPrefetch = " + toRemoveFromPrefetch );
			if ( toRemoveFromPrefetch == prefetch.size() )
				prefetch.clear();
			else
				for ( int i = 0; i < toRemoveFromPrefetch; ++i )
					prefetch.remove();

			// move queue contents to the prefetch
			int c = prefetchCapacity; // prefetch capacity left
			// add elements of first queue to the front of the prefetch
			final ArrayDeque< E > q0 = queues[ 0 ];
			final int q0n = Math.min( q0.size(), c );
			for ( int i = 0; i < q0n; ++i )
				prefetch.addFirst( q0.removeLast() );
			q0.clear();
			c -= q0n;
			// add elements of remaining queues to the end of the prefetch
			for ( int j = 1; j < queues.length; ++j )
			{
				final ArrayDeque< E > q = queues[ j ];
				final int qn = Math.min( q.size(), c );
				for ( int i = 0; i < qn; ++i )
					prefetch.addLast( q.removeFirst() );
				q.clear();
				c -= qn;
			}

			// update count: only prefetch is non-empty now
			count = prefetch.size();

//			System.out.println( "prefetch size after clearToPrefetch = " + prefetch.size() );
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Atomically removes all of the elements from this queue. All queues, as
	 * well as the {@link #prefetch} deque, will be empty after this call
	 * returns.
	 */
	public void clear()
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			for ( final ArrayDeque< E > queue : queues )
				queue.clear();
			prefetch.clear();
			count = 0;
		}
		finally
		{
			lock.unlock();
		}
	}

	public int getNumPriorities()
	{
		return maxPriority + 1;
	}

	public long getCurrentFrame()
	{
		return currentFrame;
	}
}
