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
 * A pool which loosely obeys to its bound: it can allow the accessors to consume more bytes than what
 * has been configured if that helps concurrency.

 * @author Ludovic Orban
 * @author Chris Dennis
 */
public class BoundedPool implements Pool {

    private volatile long maximumPoolSize;
    private final PoolEvictor<PoolableStore> evictor;
    private final List<BoundedPoolAccessor> poolAccessors;
    private final SizeOfEngine defaultSizeOfEngine;

    /**
     * Create a BoundedPool instance
     *
     * @param maximumPoolSize the maximum size of the pool, in bytes.
     * @param evictor the pool evictor, for cross-store eviction.
     * @param defaultSizeOfEngine the default SizeOf engine used by the accessors.
     */
    public BoundedPool(long maximumPoolSize, PoolEvictor<PoolableStore> evictor, SizeOfEngine defaultSizeOfEngine) {
        this.maximumPoolSize = maximumPoolSize;
        this.evictor = evictor;
        this.defaultSizeOfEngine = defaultSizeOfEngine;
        this.poolAccessors = Collections.synchronizedList(new ArrayList<BoundedPoolAccessor>());
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
    public long getMaxSize() {
        return maximumPoolSize;
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxSize(long newSize) {
        long oldSize = this.maximumPoolSize;
        this.maximumPoolSize = newSize;
        long sizeToEvict = oldSize - newSize;

        if (sizeToEvict > 0) {
            evictor.freeSpace(getPoolableStores(), sizeToEvict);
        }
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
        BoundedPoolAccessor poolAccessor = new BoundedPoolAccessor(store, sizeOfEngine, 0);
        poolAccessors.add(poolAccessor);
        return poolAccessor;
    }

    private void removePoolAccessor(PoolAccessor poolAccessor) {
        Iterator<BoundedPoolAccessor> iterator = poolAccessors.iterator();
        while (iterator.hasNext()) {
            BoundedPoolAccessor next = iterator.next();
            if (next == poolAccessor) {
                iterator.remove();
                return;
            }
        }
    }

    private Collection<PoolableStore> getPoolableStores() {
        Collection<PoolableStore> poolableStores = new ArrayList<PoolableStore>();
        for (BoundedPoolAccessor poolAccessor : poolAccessors) {
            poolableStores.add(poolAccessor.store);
        }
        return poolableStores;
    }


    /**
     * The PoolAccessor class of the BoundedPool
     */
    private final class BoundedPoolAccessor implements PoolAccessor {
        private final PoolableStore store;
        private final SizeOfEngine sizeOfEngine;
        private final AtomicLong size;
        private final AtomicBoolean unlinked = new AtomicBoolean();

        private BoundedPoolAccessor(PoolableStore store, SizeOfEngine sizeOfEngine, long currentSize) {
            this.store = store;
            this.sizeOfEngine = sizeOfEngine;
            this.size = new AtomicLong(currentSize);
        }

        /**
         * {@inheritDoc}
         */
        public long add(Object key, Object value, Object container, boolean force) {
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
                // check that the element isn't too big
                if (!force && sizeOf > maximumPoolSize) {
                    // this is too big to fit in the pool
                    return -1;
                }

                // if there is not enough room => evict
                long missingSize = newSize - maximumPoolSize;

                if (force | evictor.freeSpace(getPoolableStores(), missingSize)) {
                    size.addAndGet(sizeOf);
                    return sizeOf;
                } else {
                    // cannot free enough bytes
                    return -1;
                }
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

            size.addAndGet(-sizeOf);

            return sizeOf;
        }

        /**
         * {@inheritDoc}
         */
        public long replace(Role role, Object current, Object replacement, boolean force) {
            if (unlinked.get()) {
                throw new IllegalStateException("BoundedPoolAccessor has been unlinked");
            }

            long addedSize;
            long sizeOf = 0;
            switch (role) {
                case CONTAINER:
                    sizeOf += delete(null, null, current);
                    addedSize = add(null, null, replacement, force);
                    if (addedSize < 0) {
                        add(null, null, current, false);
                        sizeOf = Long.MAX_VALUE;
                    } else {
                        sizeOf -= addedSize;
                    }
                    break;
                case KEY:
                    sizeOf += delete(current, null, null);
                    addedSize = add(replacement, null, null, force);
                    if (addedSize < 0) {
                        add(current, null, null, false);
                        sizeOf = Long.MAX_VALUE;
                    } else {
                        sizeOf -= addedSize;
                    }
                    break;
                case VALUE:
                    sizeOf += delete(null, current, null);
                    addedSize = add(null, replacement, null, force);
                    if (addedSize < 0) {
                        add(null, current, null, false);
                        sizeOf = Long.MAX_VALUE;
                    } else {
                        sizeOf -= addedSize;
                    }
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            return sizeOf;
        }

        /**
         * {@inheritDoc}
         */
        public long getSize() {
            return size.get();
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
            size.set(0);
        }
    }

}
