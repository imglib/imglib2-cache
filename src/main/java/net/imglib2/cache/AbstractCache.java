package net.imglib2.cache;

public interface AbstractCache< K, V >
{
	V getIfPresent( K key );

	void invalidateAll();
//	void cleanUp();
//	void invalidate( Object key );
//	long size();
}
