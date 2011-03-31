package net.sf.ehcache.pool.impl;

import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolableStore;
import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolEvictor;
import net.sf.ehcache.pool.Role;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO: this pool implementation is not on-heap specific
 * @author Ludovic Orban
 */
public class BoundedOnHeapPool implements Pool {

    private volatile long maximumPoolSize;
    private volatile PoolEvictor<PoolableStore> evictor;
    private final List<BoundedOnHeapPoolAccessor> poolAccessors;
    private final SizeOfEngine defaultSizeOfEngine;

    public BoundedOnHeapPool(long maximumPoolSize, PoolEvictor<PoolableStore> evictor, SizeOfEngine defaultSizeOfEngine) {
        this.maximumPoolSize = maximumPoolSize;
        this.evictor = evictor;
        this.defaultSizeOfEngine = defaultSizeOfEngine;
        this.poolAccessors = new CopyOnWriteArrayList<BoundedOnHeapPoolAccessor>();
    }

    public long getSize() {
        long total = 0L;
        for (PoolAccessor poolAccessor : poolAccessors) {
            total += poolAccessor.getSize();
        }
        return total;
    }

    public PoolAccessor createPoolAccessor(PoolableStore store) {
        BoundedOnHeapPoolAccessor poolAccessor = new BoundedOnHeapPoolAccessor(store, defaultSizeOfEngine);
        poolAccessors.add(poolAccessor);
        return poolAccessor;
    }

    public Collection<PoolableStore> getPoolableStores() {
        Collection<PoolableStore> poolableStores = new ArrayList<PoolableStore>();
        for (BoundedOnHeapPoolAccessor poolAccessor : poolAccessors) {
            poolableStores.add(poolAccessor.store);
        }
        return poolableStores;
    }


    public class BoundedOnHeapPoolAccessor implements PoolAccessor {
        private final PoolableStore store;
        private final SizeOfEngine sizeOfEngine;
        private volatile long size;

        public BoundedOnHeapPoolAccessor(PoolableStore store, SizeOfEngine sizeOfEngine) {
            this.store = store;
            this.sizeOfEngine = sizeOfEngine;
            this.size = 0L;
        }

        public boolean add(Object key, Object value, Object container) {
            long sizeOf = sizeOfEngine.sizeOf(key, value, container);
            long newSize = BoundedOnHeapPool.this.getSize() + sizeOf;

            if (newSize <= maximumPoolSize) {
                // there is enough room => add & approve
                size += sizeOf;
                return true;
            } else {
                // there is not enough room => evict
                long missingSize = newSize - maximumPoolSize;
                if (missingSize > maximumPoolSize) {
                    // this is too big to fit in the pool
                    return false;
                }
                boolean successful = evictor.freeSpace(getPoolableStores(), missingSize);
                if (successful) {
                    size += sizeOf;
                }
                return successful;
            }
        }

        public void delete(Object key, Object value, Object container) {
            size -= sizeOfEngine.sizeOf(key, value, container);
        }

        public void replace(Role role, Object current, Object replacement) {
            switch (role) {
                case CONTAINER:
                    delete(null, null, replacement);
                    add(null, null, replacement);
                    break;
                case KEY:
                    delete(replacement, null, null);
                    add(replacement, null, null);
                    break;
                case VALUE:
                    delete(null, replacement, null);
                    add(null, replacement, null);
                    break;
            }
        }

        public long getSize() {
            return size;
        }
    }

}
