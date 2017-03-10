package net.imglib2.cache;

public interface CacheRemover< K, V >
{
	void onRemoval( K key, V value );
}
