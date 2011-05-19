package net.sf.ehcache.pool.impl;

import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolableStore;
import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolEvictor;
import net.sf.ehcache.pool.Role;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Ludovic Orban
 */
public class BoundedPool implements Pool {

    private final long maximumPoolSize;
    private final PoolEvictor<PoolableStore> evictor;
    private final List<BoundedPoolAccessor> poolAccessors;
    private final SizeOfEngine defaultSizeOfEngine;

    public BoundedPool(long maximumPoolSize, PoolEvictor<PoolableStore> evictor, SizeOfEngine defaultSizeOfEngine) {
        this.maximumPoolSize = maximumPoolSize;
        this.evictor = evictor;
        this.defaultSizeOfEngine = defaultSizeOfEngine;
        this.poolAccessors = Collections.synchronizedList(new ArrayList<BoundedPoolAccessor>());
    }

    public long getSize() {
        long total = 0L;
        for (PoolAccessor poolAccessor : poolAccessors) {
            total += poolAccessor.getSize();
        }
        return total;
    }

    public PoolAccessor createPoolAccessor(PoolableStore store) {
        BoundedPoolAccessor poolAccessor = new BoundedPoolAccessor(store, defaultSizeOfEngine, 0);
        poolAccessors.add(poolAccessor);
        return poolAccessor;
    }

    public void removePoolAccessor(PoolAccessor poolAccessor) {
        Iterator<BoundedPoolAccessor> iterator = poolAccessors.iterator();
        while (iterator.hasNext()) {
            BoundedPoolAccessor next = iterator.next();
            if (next == poolAccessor) {
                iterator.remove();
                return;
            }
        }
    }

    public Collection<PoolableStore> getPoolableStores() {
        Collection<PoolableStore> poolableStores = new ArrayList<PoolableStore>();
        for (BoundedPoolAccessor poolAccessor : poolAccessors) {
            poolableStores.add(poolAccessor.store);
        }
        return poolableStores;
    }


    public class BoundedPoolAccessor implements PoolAccessor {
        private final PoolableStore store;
        private final SizeOfEngine sizeOfEngine;
        private final AtomicLong size;
        private final AtomicBoolean unlinked = new AtomicBoolean();

        public BoundedPoolAccessor(PoolableStore store, SizeOfEngine sizeOfEngine, long currentSize) {
            this.store = store;
            this.sizeOfEngine = sizeOfEngine;
            this.size = new AtomicLong(currentSize);
        }

        public long add(Object key, Object value, Object container) {
            if (unlinked.get()) {
                throw new IllegalStateException("BoundedPoolAccessor has been unlinked");
            }

            long sizeOf = sizeOfEngine.sizeOf(key, value, container);

            long newSize = BoundedPool.this.getSize() + sizeOf;

            if (newSize <= maximumPoolSize) {
                // there is enough room => add & approve
                size.addAndGet(sizeOf);
                return sizeOf;
            } else {
                // there is not enough room => evict
                long missingSize = newSize - maximumPoolSize;
                if (missingSize > maximumPoolSize) {
                    // this is too big to fit in the pool
                    return -1;
                }

                if (evictor.freeSpace(getPoolableStores(), missingSize)) {
                    size.addAndGet(sizeOf);
                    return sizeOf;
                } else {
                    // cannot free enough bytes
                    return -1;
                }
            }
        }

        public long delete(Object key, Object value, Object container) {
            if (unlinked.get()) {
                throw new IllegalStateException("BoundedPoolAccessor has been unlinked");
            }

            long sizeOf = sizeOfEngine.sizeOf(key, value, container);

            size.addAndGet(-sizeOf);

            return sizeOf;
        }

        public long replace(Role role, Object current, Object replacement) {
            if (unlinked.get()) {
                throw new IllegalStateException("BoundedPoolAccessor has been unlinked");
            }

            long sizeOf = 0;
            switch (role) {
                case CONTAINER:
                    sizeOf += delete(null, null, current);
                    sizeOf -= add(null, null, replacement);
                    break;
                case KEY:
                    sizeOf += delete(current, null, null);
                    sizeOf -= add(replacement, null, null);
                    break;
                case VALUE:
                    sizeOf += delete(null, current, null);
                    sizeOf -= add(null, replacement, null);
                    break;
            }
            return sizeOf;
        }

        public long getSize() {
            return size.get();
        }

        public void unlink() {
            if (unlinked.compareAndSet(false, true)) {
                removePoolAccessor(this);
            }
        }

        public void clear() {
            size.set(0);
        }
    }

}
