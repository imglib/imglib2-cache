package net.imglib2.cache.img;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.img.cell.Cell;

/**
 * Common interface for cell caches with loader and remover
 */
public interface CellCache<A>
        extends CacheRemover<Long, Cell<A>>, CacheLoader<Long, Cell<A>> {
    // Marker interface
}