package net.sf.ehcache.pool.impl;

import net.sf.ehcache.pool.PoolableStore;
import net.sf.ehcache.pool.PoolEvictor;

import java.util.Collection;

/**
 * @author Ludovic Orban
 */
public class RoundRobinOnHeapPoolEvictor implements PoolEvictor<PoolableStore> {
    public boolean freeSpace(Collection<PoolableStore> from, long bytes) {
        long remaining = bytes;

        while (true) {
            for (PoolableStore poolableStore : from) {
                long beforeEvictionSize = poolableStore.getInMemorySizeInBytes();
                if (!poolableStore.evictFromOnHeap(1, bytes)) {
                    return false;
                }
                long afterEvictionSize = poolableStore.getInMemorySizeInBytes();

                remaining -= (beforeEvictionSize - afterEvictionSize);
                if (remaining <= 0L) {
                    return true;
                }
            }
        }
    }
}
