package net.imglib2.cache.img;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.IoSync;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
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
public class DiskCellCache<A> implements ReadWriteCellCache<A> {
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
			final int[] cellDims = new int[ n ];
			grid.getCellDimensions( index, cellMin, cellDims );
			final long numEntities = entitiesPerPixel.mulCeil( Intervals.numElements( cellDims ) );
			final long bytesize = numEntities * accessIo.getBytesPerElement();
			try (
					final RandomAccessFile mmFile = new RandomAccessFile( filename, "r" ); )
			{
				final MappedByteBuffer in = mmFile.getChannel().map( MapMode.READ_ONLY, 0, bytesize );
				final A access = accessIo.load( in, ( int ) numEntities );
				return new Cell<>( cellDims, cellMin, access );
			}
		}
		else
		{
			return backingLoader.get( key );
		}
	}

	@Override
	public void onRemoval( final Long key, final Cell< A > value )
	{
		final long index = key;
		final String filename = blockname( index );

		final int[] cellDims = new int[ n ];
		value.dimensions( cellDims );
		final long blocksize = entitiesPerPixel.mulCeil( Intervals.numElements( cellDims ) );
		final long bytesize = blocksize * accessIo.getBytesPerElement();
		try (
				final RandomAccessFile mmFile = new RandomAccessFile( filename, "rw" ); )
		{
			final MappedByteBuffer out = mmFile.getChannel().map( MapMode.READ_WRITE, 0, bytesize );
			accessIo.save( value.getData(), out, ( int ) blocksize );
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
	public static void addDeleteHook( final Path path )
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
