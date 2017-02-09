package net.imglib2.cache;

public interface CacheLoader< K, V >
{
	V get( K key ) throws Exception;
}
