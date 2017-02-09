package net.imglib2.cache;

public interface RemovalListener< K, V >
{
	void onRemoval( K key, V value );
}
