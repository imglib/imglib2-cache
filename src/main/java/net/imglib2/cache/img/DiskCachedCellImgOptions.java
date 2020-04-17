/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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
package net.imglib2.cache.img;

import java.util.Set;
import net.imglib2.Dirty;
import net.imglib2.cache.img.optional.AccessOptions;
import net.imglib2.cache.img.optional.CacheOptions;
import net.imglib2.cache.img.optional.CellDimensionsOptions;
import net.imglib2.cache.img.optional.DiskCacheOptions;
import net.imglib2.img.basictypeaccess.AccessFlags;
import org.scijava.optional.AbstractOptions;
import org.scijava.optional.AbstractValues;

/**
 * Optional parameters for constructing a {@link DiskCachedCellImgFactory}.
 *
 * @author Tobias Pietzsch
 */
public class DiskCachedCellImgOptions extends AbstractOptions< DiskCachedCellImgOptions >
		implements
		AccessOptions< DiskCachedCellImgOptions >,
		CellDimensionsOptions< DiskCachedCellImgOptions >,
		CacheOptions< DiskCachedCellImgOptions >,
		DiskCacheOptions< DiskCachedCellImgOptions >
{
	public final Values values = new Values( this );

	public DiskCachedCellImgOptions()
	{
	}

	/**
	 * Create default {@link DiskCachedCellImgOptions}.
	 *
	 * @return default {@link DiskCachedCellImgOptions}.
	 */
	public static DiskCachedCellImgOptions options()
	{
		return new DiskCachedCellImgOptions();
	}

	// NB overrides default value
	/**
	 * Specify whether the image should use {@link Dirty} accesses. Dirty
	 * accesses track whether cells were written to. Only cells that were
	 * written to are (potentially) cached to disk.
	 * <p>
	 * This is {@code true} by default.
	 * </p>
	 *
	 * @param dirty
	 *            whether the image should use {@link Dirty} accesses.
	 */
	@Override
	public DiskCachedCellImgOptions dirtyAccesses( final boolean dirty )
	{
		return AccessOptions.super.dirtyAccesses( dirty );
	}

	@Override
	protected DiskCachedCellImgOptions append( final DiskCachedCellImgOptions additionalOptions )
	{
		return super.append( additionalOptions );
	}

	private DiskCachedCellImgOptions( DiskCachedCellImgOptions that )
	{
		super( that );
	}

	@Override
	protected DiskCachedCellImgOptions copyOrThis()
	{
		return new DiskCachedCellImgOptions( this );
	}

	public static class Values extends AbstractValues implements
			AccessOptions.Val,
			CellDimensionsOptions.Val,
			CacheOptions.Val,
			DiskCacheOptions.Val
	{
		public Values( final DiskCachedCellImgOptions options )
		{
			super( options );
		}

		// NB overrides default value
		@Override
		public boolean dirtyAccesses()
		{
			return value( "dirtyAccesses", true );
		}

		public Set< AccessFlags > accessFlags()
		{
			return AccessFlags.fromBooleansDirtyVolatile( dirtyAccesses(), volatileAccesses() );
		}

		@Override
		public String toString()
		{
			final ValuesToString sb = new ValuesToString();
			AccessOptions.Val.super.buildToString( sb );
			CellDimensionsOptions.Val.super.buildToString( sb );
			CacheOptions.Val.super.buildToString( sb );
			DiskCacheOptions.Val.super.buildToString( sb );
			return sb.toString();
		}
	}
}
