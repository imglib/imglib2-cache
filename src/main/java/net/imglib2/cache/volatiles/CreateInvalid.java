package net.imglib2.cache.volatiles;

public interface CreateInvalid< K, V >
{
	public V createInvalid( K key ) throws Exception;
}
