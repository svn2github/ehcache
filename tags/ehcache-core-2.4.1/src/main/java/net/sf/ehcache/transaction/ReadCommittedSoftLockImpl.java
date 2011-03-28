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

    private final ReadCommittedSoftLockFactoryImpl factory;
    private final TransactionID transactionID;
    private final Object key;
    private Element newElement;
    private final Element oldElement;
    private final ReentrantLock lock;
    private final ReentrantReadWriteLock freezeLock;
    private volatile boolean expired;

    /**
     * Create a new ReadCommittedSoftLockImpl instance
     * @param factory the creating factory
     * @param transactionID the transaction ID
     * @param key the element's key this soft lock is going to protect
     * @param newElement the new element, can be null
     * @param oldElement the old element, can be null
     */
    ReadCommittedSoftLockImpl(ReadCommittedSoftLockFactoryImpl factory, TransactionID transactionID, Object key,
                              Element newElement, Element oldElement) {
        this.factory = factory;
        this.transactionID = transactionID;
        this.key = key;
        this.newElement = newElement;
        this.oldElement = oldElement;
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
    public Element getElement(TransactionID currentTransactionId) {
        freezeLock.readLock().lock();
        try {
            if (transactionID.equals(currentTransactionId)) {
                return newElement;
            } else {
                return oldElement;
            }
        } finally {
            freezeLock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element updateElement(Element newElement) {
        Element e = this.newElement;
        this.newElement = newElement;
        return e;
    }

    /**
     * {@inheritDoc}
     */
    public TransactionID getTransactionID() {
        return transactionID;
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
    public Element getFrozenElement() {
        if (!isFrozen()) {
            throw new IllegalStateException("cannot get frozen element of a soft lock which hasn't been frozen or hasn't expired");
        }
        if (transactionID.isDecisionCommit()) {
            return newElement;
        } else {
            return oldElement;
        }
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
        factory.clearSoftLock(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object) {
        if (object instanceof ReadCommittedSoftLockImpl) {
            ReadCommittedSoftLockImpl other = (ReadCommittedSoftLockImpl) object;

            if (!transactionID.equals(other.transactionID)) {
                return false;
            }

            if (!key.equals(other.key)) {
                return false;
            }

            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hashCode = PRIME;

        hashCode *= transactionID.hashCode();
        hashCode *= key.hashCode();

        return hashCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Soft Lock [clustered: false, isolation: rc, transactionID: " + transactionID + ", key: " + key +
                ", newElement: " + newElement + ", oldElement: " + oldElement + "]";
    }
}
