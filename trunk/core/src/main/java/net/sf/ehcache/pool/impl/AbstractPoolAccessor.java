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

import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.Role;
import net.sf.ehcache.pool.SizeOfEngine;

/**
 * Abstract PoolAccessor implementation providing pool to store binding functionality.
 *
 * @author Chris Dennis
 *
 * @param <T> accessing store type
 */
public abstract class AbstractPoolAccessor<T> implements PoolAccessor<T> {

    /**
     * {@link SizeOfEngine} used by the accessor.
     */
    protected final SizeOfEngine sizeOfEngine;

    private final AtomicBoolean unlinked = new AtomicBoolean();
    private final Pool<T> pool;
    private final T store;

    /**
     * Creates an accessor for the specified store to access the specified pool.
     *
     * @param pool pool to be accessed
     * @param store accessing store
     */
    public AbstractPoolAccessor(Pool<T> pool, T store, SizeOfEngine sizeOfEngine) {
        this.pool = pool;
        this.store = store;
        this.sizeOfEngine = sizeOfEngine;
    }

    /**
     * {@inheritDoc}
     */
    public final long add(Object key, Object value, Object container, boolean force) {
        checkLinked();
        return add(sizeOfEngine.sizeOf(key, value, container), force);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean canAddWithoutEvicting(Object key, Object value, Object container) {
        return canAddWithoutEvicting(sizeOfEngine.sizeOf(key, value, container));
    }

    /**
     * {@inheritDoc}
     */
    public long delete(Object key, Object value, Object container) {
        checkLinked();
        return delete(sizeOfEngine.sizeOf(key, value, container));
    }

    /**
     * Add a specific number of bytes to the pool.
     *
     * @param sizeOf number of bytes to add
     * @param force true if the pool should accept adding the element, even if it's out of resources
     * @return how many bytes have been added to the pool or -1 if add failed.
     */
    protected abstract long add(long sizeOf, boolean force);

    /**
     * Check if there is enough room in the pool to add a specific number of bytes without provoking any eviction
     *
     * @param sizeOf number of bytes to test against
     * @return true if there is enough room left
     */
    protected abstract boolean canAddWithoutEvicting(long sizeOf);

    /**
     * {@inheritDoc}
     */
    public final long replace(long currentSize, Object key, Object value, Object container, boolean force) {
        long sizeOf = sizeOfEngine.sizeOf(key, value, container);

        long delta = sizeOf - currentSize;
        if (delta < 0) {
            return -delete(-delta);
        } else {
            return add(delta, force);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long replace(Role role, Object current, Object replacement, boolean force) {
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
    public final void unlink() {
        if (unlinked.compareAndSet(false, true)) {
            getPool().removePoolAccessor(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final T getStore() {
        return store;
    }

    /**
     * Throws {@code IllegalStateException} if this accessor is not linked to it's pool.
     *
     * @throws IllegalStateException if not linked
     */
    protected final void checkLinked() throws IllegalStateException {
        if (unlinked.get()) {
            throw new IllegalStateException("BoundedPoolAccessor has been unlinked");
        }
    }

    /**
     * Return the pool this accessor is associated with.
     *
     * @return associated pool
     */
    protected final Pool<T> getPool() {
        return pool;
    }
}
