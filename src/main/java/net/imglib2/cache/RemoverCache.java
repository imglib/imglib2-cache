package net.imglib2.cache;

import java.util.concurrent.ExecutionException;

public interface RemoverCache< K, V > extends AbstractCache< K, V >
{
	V get( K key, CacheRemover< ? super K, ? super V > remover ) throws ExecutionException;
}
