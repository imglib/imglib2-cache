package net.imglib2.cache.img;

import net.imglib2.Dirty;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;

/**
 * Flags that specify variants of {@link ArrayDataAccess} underlying primitive
 * types. {@link #DIRTY} means that an access implements {@link Dirty}.
 * {@link #VOLATILE} means that an access implements {@link VolatileAccess}.
 *
 * @author Tobias Pietzsch
 */
public enum AccessFlags
{
	DIRTY,
	VOLATILE
}
