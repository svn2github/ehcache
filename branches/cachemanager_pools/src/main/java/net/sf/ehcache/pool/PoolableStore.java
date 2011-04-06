package net.sf.ehcache.pool;

import net.sf.ehcache.store.Store;

/**
 * @author Ludovic Orban
 */
public interface PoolableStore extends Store {

    boolean evictFromOnHeap(int count, long size);

    boolean evictFromOffHeap(int count, long size);

    boolean evictFromOnDisk(int count, long size);

}
