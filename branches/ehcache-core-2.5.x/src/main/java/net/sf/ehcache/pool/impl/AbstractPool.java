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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolEvictor;
import net.sf.ehcache.pool.SizeOfEngine;

/**
 * An abstract pool implementation.
 * <p>
 * This contains all the logic of a pool except for the actual creation of accessor instances.
 *
 * @author Chris Dennis
 *
 * @param <T> the pool store type
 */
public abstract class AbstractPool<T> implements Pool<T> {

    private volatile long maximumPoolSize;
    private final PoolEvictor<T> evictor;
    private final List<PoolAccessor<? extends T>> poolAccessors;
    private final SizeOfEngine defaultSizeOfEngine;

    /**
     * Create an AbstractPool instance
     *
     * @param maximumPoolSize the maximum size of the pool, in bytes.
     * @param evictor the pool evictor, for cross-store eviction.
     * @param defaultSizeOfEngine the default SizeOf engine used by the accessors.
     */
    public AbstractPool(long maximumPoolSize, PoolEvictor<T> evictor, SizeOfEngine defaultSizeOfEngine) {
        this.maximumPoolSize = maximumPoolSize;
        this.evictor = evictor;
        this.defaultSizeOfEngine = defaultSizeOfEngine;
        this.poolAccessors = new CopyOnWriteArrayList<PoolAccessor<? extends T>>();
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
            getEvictor().freeSpace(getPoolableStores(), sizeToEvict);
        }
    }

    /**
     * {@inheritDoc}
     */
    public PoolAccessor<T> createPoolAccessor(T store, int maxDepth, boolean abortWhenMaxDepthExceeded) {
        return createPoolAccessor(store, defaultSizeOfEngine.copyWith(maxDepth, abortWhenMaxDepthExceeded));
    }

    /**
     * {@inheritDoc}
     */
    public void registerPoolAccessor(PoolAccessor<? extends T> accessor) {
        poolAccessors.add(accessor);
    }

    /**
     * {@inheritDoc}
     */
    public void removePoolAccessor(PoolAccessor<?> accessor) {
        Iterator<PoolAccessor<? extends T>> iterator = poolAccessors.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == accessor) {
                iterator.remove();
                return;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<T> getPoolableStores() {
        Collection<T> poolableStores = new ArrayList<T>(poolAccessors.size());
        for (PoolAccessor<? extends T> poolAccessor : poolAccessors) {
            poolableStores.add(poolAccessor.getStore());
        }
        return poolableStores;
    }

    /**
     * {@inheritDoc}
     */
    public PoolEvictor<T> getEvictor() {
        return evictor;
    }

}
