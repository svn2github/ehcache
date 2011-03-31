package net.sf.ehcache.pool;

import net.sf.ehcache.store.Store;

/**
 * @author Ludovic Orban
 */
public interface PoolableStore extends Store {

    boolean evictFromOnHeap(int count);

    boolean evictFromOffHeap(int count);

    boolean evictFromOnDisk(int count);

}
