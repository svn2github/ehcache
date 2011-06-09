/**
 *  Copyright 2003-2010 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
 * A pool which strictly obeys to its bound: it will never allow the accessors to consume more bytes than what
 * has been configured.
 *
 * @author Ludovic Orban
 */
public class StrictlyBoundedPool implements Pool {

    private volatile long maximumPoolSize;
    private volatile PoolEvictor<PoolableStore> evictor;
    private final List<StrictlyBoundedPoolAccessor> poolAccessors;
    private final SizeOfEngine defaultSizeOfEngine;

    /**
     * Create a StrictlyBoundedPool instance
     *
     * @param maximumPoolSize the maximum size of the pool, in bytes.
     * @param evictor the pool evictor, for cross-store eviction.
     * @param defaultSizeOfEngine the default SizeOf engine used by the accessors.
     */
    public StrictlyBoundedPool(long maximumPoolSize, PoolEvictor<PoolableStore> evictor, SizeOfEngine defaultSizeOfEngine) {
        this.maximumPoolSize = maximumPoolSize;
        this.evictor = evictor;
        this.defaultSizeOfEngine = defaultSizeOfEngine;
        this.poolAccessors = Collections.synchronizedList(new ArrayList<StrictlyBoundedPoolAccessor>());
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
        long total = 0L;
        for (PoolAccessor poolAccessor : poolAccessors) {
            total += poolAccessor.getSize();
        }
        return total;
    }

    /**
     * {@inheritDoc}
     */
    public PoolAccessor createPoolAccessor(PoolableStore store) {
        return createPoolAccessor(store, defaultSizeOfEngine);
    }

    /**
     * {@inheritDoc}
     */
    public PoolAccessor createPoolAccessor(PoolableStore store, SizeOfEngine sizeOfEngine) {
        StrictlyBoundedPoolAccessor poolAccessor = new StrictlyBoundedPoolAccessor(store, sizeOfEngine, 0);
        poolAccessors.add(poolAccessor);
        return poolAccessor;
    }

    private void removePoolAccessor(PoolAccessor poolAccessor) {
        Iterator<StrictlyBoundedPoolAccessor> iterator = poolAccessors.iterator();
        while (iterator.hasNext()) {
            StrictlyBoundedPoolAccessor next = iterator.next();
            if (next == poolAccessor) {
                iterator.remove();
                return;
            }
        }
    }

    private Collection<PoolableStore> getPoolableStores() {
        Collection<PoolableStore> poolableStores = new ArrayList<PoolableStore>();
        for (StrictlyBoundedPoolAccessor poolAccessor : poolAccessors) {
            poolableStores.add(poolAccessor.store);
        }
        return poolableStores;
    }


    /**
     * The PoolAccessor class of the StrictlyBoundedPool
     */
    private final class StrictlyBoundedPoolAccessor implements PoolAccessor {
        private final PoolableStore store;
        private final SizeOfEngine sizeOfEngine;
        private long size;
        private final AtomicBoolean unlinked = new AtomicBoolean();
        private final Lock lock = new ReentrantLock();

        private StrictlyBoundedPoolAccessor(PoolableStore store, SizeOfEngine sizeOfEngine, long currentSize) {
            this.store = store;
            this.sizeOfEngine = sizeOfEngine;
            this.size = currentSize;
        }

        /**
         * {@inheritDoc}
         */
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
                        // check that the element isn't too big
                        if (!force && sizeOf > maximumPoolSize) {
                            // this is too big to fit in the pool
                            return -1;
                        }

                        // there is not enough room => evict
                        long missingSize = newSize - maximumPoolSize;

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
                        if (!force && StrictlyBoundedPool.this.getSize() + sizeOf > maximumPoolSize) {
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

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        public long getSize() {
            // locking makes the size update MT-safe but slow
            lock.lock();
            try {
                return size;
            } finally {
                lock.unlock();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void unlink() {
            if (unlinked.compareAndSet(false, true)) {
                removePoolAccessor(this);
            }
        }

        /**
         * {@inheritDoc}
         */
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
