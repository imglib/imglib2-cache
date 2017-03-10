package net.imglib2.cache;

public interface UncheckedLoaderCache< K, V > extends AbstractCache< K, V >
{
	V get( K key, CacheLoader< ? super K, ? extends V > loader );
}
