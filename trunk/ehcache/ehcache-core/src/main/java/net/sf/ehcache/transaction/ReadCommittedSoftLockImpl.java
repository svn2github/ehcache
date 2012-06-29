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
package net.sf.ehcache.transaction;

import net.sf.ehcache.Element;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A SoftLock implementation with Read-Committed isolation level
 *
 * @author Ludovic Orban
 */
public class ReadCommittedSoftLockImpl implements SoftLock {
    private static final int PRIME = 31;

    private final SoftLockManager manager;
    private final ReentrantLock lock;
    private final ReentrantReadWriteLock freezeLock;

    private final Object key;
    private volatile boolean expired;

    /**
     * Create a new ReadCommittedSoftLockImpl instance
     * @param manager the creating manager
     * @param key the element's key this soft lock is going to protect
     */
    ReadCommittedSoftLockImpl(SoftLockManager manager, Object key) {
        this.manager = manager;
        this.key = key;
        this.lock = new ReentrantLock();
        this.freezeLock = new ReentrantReadWriteLock();
    }

    /**
     * {@inheritDoc}
     */
    public Object getKey() {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    public Element getElement(TransactionID currentTransactionId, SoftLockID softLockId) {
        freezeLock.readLock().lock();
        try {
            if (softLockId.getTransactionID().equals(currentTransactionId)) {
                return softLockId.getNewElement();
            } else {
                return softLockId.getOldElement();
            }
        } finally {
            freezeLock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void lock() {
        lock.lock();
    }

    /**
     * {@inheritDoc}
     */
    public boolean tryLock(long ms) throws InterruptedException {
        return lock.tryLock(ms, TimeUnit.MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     */
    public void clearTryLock() {
        lock.unlock();
    }

    /**
     * {@inheritDoc}
     */
    public void unlock() {
        lock.unlock();
        clear();
    }

    private boolean isLocked() {
        return lock.isLocked();
    }

    /**
     * {@inheritDoc}
     */
    public void freeze() {
        if (!isLocked()) {
            throw new IllegalStateException("cannot freeze an unlocked soft lock");
        }
        freezeLock.writeLock().lock();
    }

    /**
     * {@inheritDoc}
     */
    public void unfreeze() {
        freezeLock.writeLock().unlock();
    }

    private boolean isFrozen() {
        return freezeLock.isWriteLocked();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExpired() {
        if (!expired) {
            expired = !isFrozen() && !isLocked();
        }
        return expired;
    }

    private void clear() {
        manager.clearSoftLock(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Soft Lock [clustered: false, isolation: rc, key: " + key + "]";
    }

}
