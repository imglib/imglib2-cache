/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2024 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
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
package net.imglib2.cache.img;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.IoSync;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellGrid.CellDimensionsAndSteps;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;

/**
 * Basic {@link CacheRemover}/{@link CacheLoader} for writing/reading cells
 * to a disk cache. Currently blocks are simply written as flat files to a
 * specified directory. {@link #createTempDirectory(String, boolean)} can be
 * used to create a temporary directory that will be automatically removed when
 * the JVM shuts down.
 * <p>
 * Blocks which are not in the diskcache (yet) are obtained from a backing
 * {@link CacheLoader}.
 * </p>
 * <p><em>
 * A {@link DiskCellCache} should be connected to a in-memory cache through
 * {@link IoSync} if the cache will be used concurrently by multiple threads!
 * </em></p>
 *
 * @param <A>
 *            access type
 *
 * @author Tobias Pietzsch
 */
public class DiskCellCache< A > implements CacheRemover< Long, Cell< A >, A >, CacheLoader< Long, Cell< A > >
{
	private final Path blockcache;

	private final CellGrid grid;

	private final int n;

	private final Fraction entitiesPerPixel;

	private final AccessIo< A > accessIo;

	private final CacheLoader< Long, Cell< A > > backingLoader;

	public DiskCellCache(
			final Path blockcache,
			final CellGrid grid,
			final CacheLoader< Long, Cell< A > > backingLoader,
			final AccessIo< A > accessIo,
			final Fraction entitiesPerPixel )
	{
		this.blockcache = blockcache;
		this.grid = grid;
		this.n = grid.numDimensions();
		this.entitiesPerPixel = entitiesPerPixel;
		this.accessIo = accessIo;
		this.backingLoader = backingLoader;
	}

	private String blockname( final long index )
	{
//		final long[] cellGridPosition = new long[ n ];
//		grid.getCellGridPositionFlat( index, cellGridPosition );


		return String.format( "%s/%d", blockcache, index );
	}

	@Override
	public Cell< A > get( final Long key ) throws Exception
	{
		final long index = key;
		final String filename = blockname( index );

		if ( new File( filename ).exists() )
		{
			final long[] cellMin = new long[ n ];
			final CellDimensionsAndSteps dimsAndSteps = grid.getCellDimensions( index, cellMin );
			final long numEntities = entitiesPerPixel.mulCeil( dimsAndSteps.numPixels() );
			final long bytesize = numEntities * accessIo.getBytesPerElement();
			try ( final RandomAccessFile mmFile = new RandomAccessFile( filename, "r" ) )
			{
				final MappedByteBuffer in = mmFile.getChannel().map( MapMode.READ_ONLY, 0, bytesize );
				final A access = accessIo.load( in, ( int ) numEntities );
				return new Cell<>( dimsAndSteps, cellMin, access );
			}
		}
		else
		{
			return backingLoader.get( key );
		}
	}

	@Override
	public A extract( final Cell< A > value )
	{
		return value.getData();
	}

	@Override
	public Cell< A > reconstruct( final Long key, final A valueData )
	{
		final long index = key;
		final long[] cellMin = new long[ n ];
		final CellDimensionsAndSteps dimsAndSteps = grid.getCellDimensions( index, cellMin );
		return new Cell<>( dimsAndSteps, cellMin, valueData );
	}

	@Override
	public void onRemoval( final Long key, final A valueData )
	{
		final long index = key;
		final String filename = blockname( index );

		final long[] cellMin = new long[ n ];
		final int[] cellDims = new int[ n ];
		grid.getCellDimensions( index, cellMin, cellDims );
		final long blocksize = entitiesPerPixel.mulCeil( Intervals.numElements( cellDims ) );
		final long bytesize = blocksize * accessIo.getBytesPerElement();
		try (
				final RandomAccessFile mmFile = new RandomAccessFile( filename, "rw" ); )
		{
			final MappedByteBuffer out = mmFile.getChannel().map( MapMode.READ_WRITE, 0, bytesize );
			accessIo.save( valueData, out, ( int ) blocksize );
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public CompletableFuture< Void > persist( final Long key, final A valueData )
	{
		onRemoval( key, valueData );
		return CompletableFuture.completedFuture( null );
	}

	/**
	 * Removes the file for the given {@code key} (if it exists).
	 */
	@Override
	public void invalidate( final Long key )
	{
		try
		{
			Files.deleteIfExists( Paths.get( blockname( key ) ) );
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	/**
	 * Removes all files for keys matching the given {@code condition}.
	 *
	 * @param parallelismThreshold
	 *            ignored
	 * @param condition
	 *            condition on keys of entries to remove
	 */
	@Override
	public void invalidateIf( final long parallelismThreshold, final Predicate< Long > condition )
	{
		try
		{
			Files.walkFileTree( blockcache, EnumSet.noneOf( FileVisitOption.class ), 1, new SimpleFileVisitor< Path >()
			{
				@Override
				public FileVisitResult visitFile( final Path file, final BasicFileAttributes attrs ) throws IOException
				{
					try
					{
						final long key = Long.parseLong( file.getFileName().toString() );
						if ( condition.test( key ) )
							Files.delete( file );
					}
					catch ( final NumberFormatException e )
					{}
					return FileVisitResult.CONTINUE;
				}
			} );
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	/**
	 * Removes all files.
	 *
	 * @param parallelismThreshold
	 *            ignored
	 */
	@Override
	public void invalidateAll( final long parallelismThreshold )
	{
		try
		{
			Files.walkFileTree( blockcache, EnumSet.noneOf( FileVisitOption.class ), 1, new SimpleFileVisitor< Path >()
			{
				@Override
				public FileVisitResult visitFile( final Path file, final BasicFileAttributes attrs ) throws IOException
				{
					Files.delete( file );
					return FileVisitResult.CONTINUE;
				}

			} );
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	// Adapted from http://stackoverflow.com/a/20280989
	static class DeleteTempFilesHook extends Thread
	{
		private final ArrayList< Path > tempPaths = new ArrayList<>();

		public void add( final Path path )
		{
			tempPaths.add( path );
		}

		@Override
		public void run()
		{
			for ( final Path path : tempPaths )
			{
				try
				{
					Files.walkFileTree( path, new SimpleFileVisitor< Path >()
					{
						@Override
						public FileVisitResult visitFile( final Path file, final BasicFileAttributes attrs ) throws IOException
						{
							Files.delete( file );
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory( final Path dir, final IOException e ) throws IOException
						{
							if ( e == null )
							{
								Files.delete( dir );
								return FileVisitResult.CONTINUE;
							}
							// directory iteration failed
							throw e;
						}
					} );
				}
				catch ( final IOException e )
				{
					throw new RuntimeException( e );
				}
			}
		}
	}

	static DeleteTempFilesHook deleteTempFilesHook = null;

	/**
	 * Register a path for deletion when the virtual machine shuts down. If the
	 * specified {@code path} is a directory, it is deleted recursively.
	 *
	 * @param path
	 *            path to delete on virtual machine shutdown.
	 */
	public static synchronized void addDeleteHook( final Path path )
	{
		if ( deleteTempFilesHook == null )
		{
			deleteTempFilesHook = new DeleteTempFilesHook();
			Runtime.getRuntime().addShutdownHook( deleteTempFilesHook );
		}
		deleteTempFilesHook.add( path );
	}

	/**
	 * Creates a new directory in the specified directory, using the given
	 * prefix to generate its name.
	 *
	 * @param dir
	 *            the path to directory in which to create the directory
	 * @param prefix
	 *            the prefix string to be used in generating the directory's
	 *            name; may be {@code null}
	 * @param deleteOnExit
	 *            whether the created directory should be automatically deleted
	 *            when the virtual machine shuts down.
	 */
	public static Path createTempDirectory( final Path dir, final String prefix, final boolean deleteOnExit ) throws IOException
	{
		final Path tmp = Files.createTempDirectory( dir, prefix );

		if ( deleteOnExit )
			addDeleteHook( tmp );

		return tmp;
	}

	/**
	 * Creates a new directory in the default temporary-file directory, using
	 * the given prefix to generate its name.
	 *
	 * @param prefix
	 *            the prefix string to be used in generating the directory's
	 *            name; may be {@code null}
	 * @param deleteOnExit
	 *            whether the created directory should be automatically deleted
	 *            when the virtual machine shuts down.
	 */
	public static Path createTempDirectory( final String prefix, final boolean deleteOnExit ) throws IOException
	{
		final Path tmp = Files.createTempDirectory( prefix );

		if ( deleteOnExit )
			addDeleteHook( tmp );

		return tmp;
	}
}
