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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolParticipant;
import net.sf.ehcache.pool.SizeOfEngine;

/**
 * The PoolAccessor class of the StrictlyBoundedPool
 *
 * @author Ludovic Orban
 * @author Alex Snaps
 */
final class LockedPoolAccessor extends AbstractPoolAccessor {

    private long size;
    private final Lock lock = new ReentrantLock();

    /**
     * Creates a locked pool accessor with the specified properties.
     *
     * @param pool pool to be accessed
     * @param poolParticipant accessing poolParticipant
     * @param sizeOfEngine engine used to size objects
     * @param currentSize initial size of the poolParticipant
     */
    LockedPoolAccessor(Pool pool, PoolParticipant poolParticipant, SizeOfEngine sizeOfEngine, long currentSize) {
        super(pool, poolParticipant, sizeOfEngine);
        this.size = currentSize;
    }

    /**
     * {@inheritDoc}
     */
    protected long add(long sizeOf, boolean force) throws IllegalArgumentException {
        if (sizeOf < 0L) {
            throw new IllegalArgumentException("cannot add negative size");
        }

        lock.lock();
        try {
            while (true) {
                long newSize = getPool().getSize() + sizeOf;

                if (newSize <= getPool().getMaxSize()) {
                    // there is enough room => add & approve
                    size += sizeOf;
                    return sizeOf;
                } else {
                    // check that the element isn't too big
                    if (!force && sizeOf > getPool().getMaxSize()) {
                        // this is too big to fit in the pool
                        return -1;
                    }

                    // if there is not enough room => evict
                    long missingSize = newSize - getPool().getMaxSize();

                    // eviction must be done outside the lock to avoid deadlocks as it may evict from other pools
                    lock.unlock();
                    try {
                        boolean successful = getPool().getEvictor().freeSpace(getPool().getPoolAccessors(), missingSize);
                        if (!force && !successful) {
                            // cannot free enough bytes
                            return -1;
                        }
                    } finally {
                        lock.lock();
                    }

                    // check that the freed space was not 'stolen' by another thread while
                    // eviction was running out of the lock
                    if (!force && getPool().getSize() + sizeOf > getPool().getMaxSize()) {
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
    protected boolean canAddWithoutEvicting(long sizeOf) {
        lock.lock();
        try {
            long newSize = getPool().getSize() + sizeOf;
            return newSize <= getPool().getMaxSize();
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long delete(long sizeOf) throws IllegalArgumentException {
        checkLinked();
//        if (sizeOf < 0L) {
//            throw new IllegalArgumentException("cannot delete negative size");
//        }

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
    protected void doClear() {
        // locking makes the size update MT-safe but slow
        lock.lock();
        try {
            size = 0L;
        } finally {
            lock.unlock();
        }
    }
}
