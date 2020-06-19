package net.imglib2.cache.img;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import net.imglib2.Dirty;
import net.imglib2.cache.CacheLoader;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.util.Fraction;

/**
 * TODO
 *
 * @param <A>
 *            access type
 *
 * @author Tobias Pietzsch
 */
public class DirtyDiskCellCache< A extends Dirty > extends DiskCellCache< A >
{
	public < T extends NativeType< T > > DirtyDiskCellCache(
			final Path blockcache,
			final CellGrid grid,
			final CacheLoader< Long, Cell< A > > backingLoader,
			final AccessIo< A > accessIo,
			final Fraction entitiesPerPixel )
	{
		super( blockcache, grid, backingLoader, accessIo, entitiesPerPixel );
	}

	@Override
	public void onRemoval( final Long key, final A valueData )
	{
		if ( valueData.isDirty() )
			super.onRemoval( key, valueData );
	}

	@Override
	public CompletableFuture< Void > persist( final Long key, final A valueData )
	{
		if ( valueData.isDirty() )
		{
			final CompletableFuture< Void > result = super.persist( key, valueData );
			valueData.setDirty( false );
			return result;
		}
		else
			return CompletableFuture.completedFuture( null );
	}
}
