/**
 *  Copyright Terracotta, Inc.
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
import net.sf.ehcache.pool.PoolParticipant;
import net.sf.ehcache.pool.Size;
import net.sf.ehcache.pool.SizeOfEngine;

/**
 * Abstract PoolAccessor implementation providing pool to store binding functionality.
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
public abstract class AbstractPoolAccessor implements PoolAccessor<PoolParticipant> {

    /**
     * {@link SizeOfEngine} used by the accessor.
     */
    protected final SizeOfEngine sizeOfEngine;

    private final AtomicBoolean unlinked = new AtomicBoolean();
    private final Pool pool;
    private final PoolParticipant poolParticipant;

    private volatile boolean abortedSizeOf = false;

    /**
     * Creates an accessor for the specified participant to access the specified pool.
     *
     * @param pool pool to be accessed
     * @param participant accessing participant
     */
    public AbstractPoolAccessor(Pool pool, PoolParticipant participant, SizeOfEngine sizeOfEngine) {
        this.pool = pool;
        this.poolParticipant = participant;
        this.sizeOfEngine = sizeOfEngine;
    }

    /**
     * {@inheritDoc}
     */
    public final long add(Object key, Object value, Object container, boolean force) {
        checkLinked();
        Size sizeOf = sizeOfEngine.sizeOf(key, value, container);
        if (!sizeOf.isExact()) {
            abortedSizeOf = true;
        }
        return add(sizeOf.getCalculated(), force);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean canAddWithoutEvicting(Object key, Object value, Object container) {
        Size sizeOf = sizeOfEngine.sizeOf(key, value, container);
        return canAddWithoutEvicting(sizeOf.getCalculated());
    }

    /**
     * Add a specific number of bytes to the pool.
     *
     * @param sizeOf number of bytes to add
     * @param force true if the pool should accept adding the element, even if it's out of resources
     * @return how many bytes have been added to the pool or -1 if add failed.
     * @throws IllegalArgumentException when sizeOf is negative
     */
    protected abstract long add(long sizeOf, boolean force) throws IllegalArgumentException;

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
        Size sizeOf = sizeOfEngine.sizeOf(key, value, container);

        long delta = sizeOf.getCalculated() - currentSize;
        if (delta == 0L) {
            return 0L;
        } else if (delta < 0L) {
            return -delete(-delta);
        } else {
            long added = add(delta, force);
            return added == -1L ? Long.MIN_VALUE : added;
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void clear() {
        doClear();
        abortedSizeOf = false;
    }

    /**
     * Free resources used by this accessor.
     * This method is called by {@link #clear()}.
     */
    protected abstract void doClear();

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
    public final PoolParticipant getParticipant() {
        return poolParticipant;
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxSize(final long newValue) {
        this.pool.setMaxSize(newValue);
    }

    /**
     * {@inheritDoc}
     */
    public long getPoolOccupancy() {
        return pool.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getPoolSize() {
        return pool.getMaxSize();
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
    protected final Pool getPool() {
        return pool;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAbortedSizeOf() {
        return abortedSizeOf;
    }
}
