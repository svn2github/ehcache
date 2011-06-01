package net.sf.ehcache.pool.impl;

import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolEvictor;
import net.sf.ehcache.pool.PoolableStore;
import net.sf.ehcache.pool.Role;
import net.sf.ehcache.pool.SizeOfEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ludovic Orban
 */
public class StrictlyBoundedPool implements Pool {

    private volatile long maximumPoolSize;
    private volatile PoolEvictor<PoolableStore> evictor;
    private final List<BoundedPoolAccessor> poolAccessors;
    private final SizeOfEngine defaultSizeOfEngine;

    public StrictlyBoundedPool(long maximumPoolSize, PoolEvictor<PoolableStore> evictor, SizeOfEngine defaultSizeOfEngine) {
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
        return createPoolAccessor(store, defaultSizeOfEngine);
    }

    public PoolAccessor createPoolAccessor(PoolableStore store, SizeOfEngine sizeOfEngine) {
        BoundedPoolAccessor poolAccessor = new BoundedPoolAccessor(store, sizeOfEngine, 0);
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
        private long size;
        private final AtomicBoolean unlinked = new AtomicBoolean();
        private final Lock lock = new ReentrantLock();

        public BoundedPoolAccessor(PoolableStore store, SizeOfEngine sizeOfEngine, long currentSize) {
            this.store = store;
            this.sizeOfEngine = sizeOfEngine;
            this.size = currentSize;
        }

        public long add(Object key, Object value, Object container, boolean force) {
            if (unlinked.get()) {
                throw new IllegalStateException("BoundedPoolAccessor has been unlinked");
            }

            long sizeOf = sizeOfEngine.sizeOf(key, value, container);

            // synchronized makes the size update MT-safe but slow
            lock.lock();
            try {
                while (true) {
                    long newSize = StrictlyBoundedPool.this.getSize() + sizeOf;

                    if (newSize <= maximumPoolSize) {
                        // there is enough room => add & approve
                        size += sizeOf;
                        return sizeOf;
                    } else {
                        // there is not enough room => evict
                        long missingSize = newSize - maximumPoolSize;
                        if (!force && missingSize > maximumPoolSize) {
                            // this is too big to fit in the pool
                            return -1;
                        }

                        // eviction must be done outside the lock to avoid deadlocks as it may evict from other pools
                        lock.unlock();
                        try {
                            boolean successful = evictor.freeSpace(getPoolableStores(), missingSize);
                            if (!force && !successful) {
                                // cannot free enough bytes
                                return -1;
                            }
                        } finally {
                            lock.lock();
                        }

                        // check that the freed space was not 'stolen' by another thread while
                        // eviction was running out of the lock
                        if (StrictlyBoundedPool.this.getSize() + sizeOf > maximumPoolSize) {
                            continue;
                        }

                        size += sizeOf;
                        return sizeOf;
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        public long delete(Object key, Object value, Object container) {
            if (unlinked.get()) {
                throw new IllegalStateException("BoundedPoolAccessor has been unlinked");
            }

            long sizeOf = sizeOfEngine.sizeOf(key, value, container);

            // synchronized makes the size update MT-safe but slow
            lock.lock();
            try {
                size -= sizeOf;
            } finally {
                lock.unlock();
            }

            return sizeOf;
        }

        public long replace(Role role, Object current, Object replacement, boolean force) {
            if (unlinked.get()) {
                throw new IllegalStateException("BoundedPoolAccessor has been unlinked");
            }

            // locking makes the size update MT-safe but slow
            lock.lock();
            try {
                long size;
                long sizeOf = 0;
                switch (role) {
                    case CONTAINER:
                        sizeOf += delete(null, null, current);
                        size = add(null, null, replacement, force);
                        if (size < 0) {
                            add(null, null, current, false);
                            sizeOf = Long.MAX_VALUE;
                        } else {
                            sizeOf -= size;
                        }
                        break;
                    case KEY:
                        sizeOf += delete(current, null, null);
                        size = add(replacement, null, null, force);
                        if (size < 0) {
                            add(current, null, null, false);
                            sizeOf = Long.MAX_VALUE;
                        } else {
                            sizeOf -= size;
                        }
                        break;
                    case VALUE:
                        sizeOf += delete(null, current, null);
                        size = add(null, replacement, null, force);
                        if (size < 0) {
                            add(null, current, null, false);
                            sizeOf = Long.MAX_VALUE;
                        } else {
                            sizeOf -= size;
                        }
                        break;
                }
                return sizeOf;
            } finally {
                lock.unlock();
            }
        }

        public long getSize() {
            // locking makes the size update MT-safe but slow
            lock.lock();
            try {
                return size;
            } finally {
                lock.unlock();
            }
        }

        public void unlink() {
            if (unlinked.compareAndSet(false, true)) {
                removePoolAccessor(this);
            }
        }

        public void clear() {
            // locking makes the size update MT-safe but slow
            lock.lock();
            try {
                size = 0L;
            } finally {
                lock.unlock();
            }
        }
    }

}
