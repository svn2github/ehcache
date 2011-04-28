package net.sf.ehcache.pool.impl;

import net.sf.ehcache.pool.PoolEvictor;
import net.sf.ehcache.pool.PoolableStore;

import java.util.Collection;

/**
 * todo measuring size before & after eviction isn't safe in multi-threaded case
 *
 * @author Ludovic Orban
 */
public class FromLargestCacheOnHeapPoolEvictor implements PoolEvictor<PoolableStore> {

    public boolean freeSpace(Collection<PoolableStore> from, long bytes) {
        if (from == null || from.isEmpty()) {
            return false;
        }

        while (true) {
            long remaining = 0;
            PoolableStore largestPoolableStore = null;

            for (PoolableStore poolableStore : from) {
                if (largestPoolableStore == null || poolableStore.getInMemorySizeInBytes() > largestPoolableStore.getInMemorySizeInBytes()) {
                    largestPoolableStore = poolableStore;
                }
            } // for

            long beforeEvictionSize = largestPoolableStore.getInMemorySizeInBytes();
            if (!largestPoolableStore.evictFromOnHeap(1, bytes)) {
                return false;
            }
            long afterEvictionSize = largestPoolableStore.getInMemorySizeInBytes();

            remaining -= (beforeEvictionSize - afterEvictionSize);
            if (remaining <= 0L) {
                return true;
            }
        } // while

    }
}
