package net.imglib2.cache;

public interface AbstractCache< K, V >
{
	V getIfPresent( K key );

	/**
	 * TODO: Decide what invalidateAll() should actually do. See
	 * <a href="https://github.com/imglib/imglib2-cache/issues/2">this issue</a>
	 */
	void invalidateAll();
//	void cleanUp();
//	void invalidate( Object key );
//	long size();
}
