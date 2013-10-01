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
package net.sf.ehcache.transaction.local;

import net.sf.ehcache.transaction.SoftLock;
import net.sf.ehcache.transaction.TransactionException;
import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.transaction.TransactionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A local transaction's thread context
 *
 * @author Ludovic Orban
 */
public class LocalTransactionContext {

    private static final Logger LOG = LoggerFactory.getLogger(LocalTransactionContext.class.getName());

    private boolean rollbackOnly;
    private final long expirationTimestamp;
    private final TransactionIDFactory transactionIdFactory;
    private final TransactionID transactionId;
    private final Map<String, List<SoftLock>> softLockMap = new HashMap<String, List<SoftLock>>();
    private final Map<String, LocalTransactionStore> storeMap = new HashMap<String, LocalTransactionStore>();
    private final List<TransactionListener> listeners = new ArrayList<TransactionListener>();

    /**
     * Create a new LocalTransactionContext
     * @param transactionTimeout the timeout before the context expires
     * @param transactionIdFactory the transaction ID factory to retrieve a new transaction id from
     */
    public LocalTransactionContext(int transactionTimeout, TransactionIDFactory transactionIdFactory) {
        this.expirationTimestamp = MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS) +
                MILLISECONDS.convert(transactionTimeout, TimeUnit.SECONDS);
        this.transactionIdFactory = transactionIdFactory;
        this.transactionId = transactionIdFactory.createTransactionID();
    }

    /**
     * Check if the context timed out
     * @return true if the context timed out, false otherwise
     */
    public boolean timedOut() {
        return timeBeforeTimeout() <= 0;
    }

    /**
     * Get the time until this context will expire
     * @return the time in milliseconds after which this context will expire
     */
    public long timeBeforeTimeout() {
        return Math.max(0, expirationTimestamp - MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS));
    }
    
    /**
     * Mark the context for rollback
     */
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    /**
     * Register a soft lock in the context
     * @param cacheName the name of the cache this soft lock is in
     * @param store the LocalTransactionStore this soft lock is in
     * @param softLock the soft lock
     */
    public void registerSoftLock(String cacheName, LocalTransactionStore store, SoftLock softLock) {
        List<SoftLock> softLocks = softLockMap.get(cacheName);
        if (softLocks == null) {
            softLocks = new ArrayList<SoftLock>();
            softLockMap.put(cacheName, softLocks);
            storeMap.put(cacheName, store);
        }
        softLocks.add(softLock);
    }

    //todo this method isn't needed if there is no copy on read/write in the underlying store
    /**
     * Update a soft lock already registered in the context
     * @param cacheName the name of the cache this soft lock is in
     * @param softLock the soft lock
     */
    public void updateSoftLock(String cacheName, SoftLock softLock) {
        List<SoftLock> softLocks = softLockMap.get(cacheName);
        softLocks.remove(softLock);
        softLocks.add(softLock);
    }

    /**
     * Get all soft locks registered in this context for a specific cache
     * @param cacheName the name of the cache
     * @return a List of registered soft locks for this cache
     */
    public List<SoftLock> getSoftLocksForCache(String cacheName) {
        List<SoftLock> softLocks = softLockMap.get(cacheName);
        if (softLocks == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(softLocks);
    }

    /**
     * Check if anything was locked in this transaction's context
     * @return true if at least one soft lock got registered, false otherwise
     */
    public boolean hasLockedAnything() {
        return !softLockMap.isEmpty();
    }

    /**
     * Commit all work done in the context and release all registered soft locks
     * @param ignoreTimeout true if commit should proceed no matter the timeout
     */
    public void commit(boolean ignoreTimeout) {
        if (!ignoreTimeout && timedOut()) {
            rollback();
            throw new TransactionTimeoutException("transaction timed out, rolled back on commit");
        }
        if (rollbackOnly) {
            rollback();
            throw new TransactionException("transaction was marked as rollback only, rolled back on commit");
        }

        try {
            fireBeforeCommitEvent();
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} participating cache(s), committing transaction {}", softLockMap.keySet().size(), transactionId);
            }
            freeze();
            transactionIdFactory.markForCommit(transactionId);

            for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
                String cacheName = stringListEntry.getKey();
                LocalTransactionStore store = storeMap.get(cacheName);
                List<SoftLock> softLocks = stringListEntry.getValue();

                LOG.debug("committing soft locked values of cache {}", cacheName);
                store.commit(softLocks);
            }
            LOG.debug("committed transaction {}", transactionId);
        } finally {
            try {
                unfreezeAndUnlock();
            } finally {
                softLockMap.clear();
                storeMap.clear();
                fireAfterCommitEvent();
            }
        }
    }

    /**
     * Rollback all work done in the context and release all registered soft locks
     */
    public void rollback() {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} participating cache(s), rolling back transaction {}", softLockMap.keySet().size(), transactionId);
            }
            freeze();

            for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
                String cacheName = stringListEntry.getKey();
                LocalTransactionStore store = storeMap.get(cacheName);
                List<SoftLock> softLocks = stringListEntry.getValue();

                LOG.debug("rolling back soft locked values of cache {}", cacheName);
                store.rollback(softLocks);
            }
            LOG.debug("rolled back transaction {}", transactionId);
        } finally {
            try {
                unfreezeAndUnlock();
            } finally {
                softLockMap.clear();
                storeMap.clear();
                fireAfterRollbackEvent();
            }
        }
    }

    /**
     * Get the transaction ID of the context
     * @return the transaction ID
     */
    public TransactionID getTransactionId() {
        return transactionId;
    }

    /**
     * Add a TransactionListener to this context
     * @param listener the listener
     */
    public void addListener(TransactionListener listener) {
        this.listeners.add(listener);
    }

    private void fireBeforeCommitEvent() {
        for (TransactionListener listener : listeners) {
            try {
                listener.beforeCommit();
            } catch (Exception e) {
                LOG.error("beforeCommit error", e);
            }
        }
    }

    private void fireAfterCommitEvent() {
        for (TransactionListener listener : listeners) {
            try {
                listener.afterCommit();
            } catch (Exception e) {
                LOG.error("afterCommit error", e);
            }
        }
    }

    private void fireAfterRollbackEvent() {
        for (TransactionListener listener : listeners) {
            try {
                listener.afterRollback();
            } catch (Exception e) {
                LOG.error("afterRollback error", e);
            }
        }
    }

    private void unfreezeAndUnlock() {
        LOG.debug("unfreezing and unlocking {} soft lock(s)", softLockMap.size());
        boolean success = true;
        for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
            List<SoftLock> softLocks = stringListEntry.getValue();

            for (SoftLock softLock : softLocks) {
                try {
                    softLock.unfreeze();
                    LOG.debug("unfroze {}", softLock);
                } catch (Exception e) {
                    success = false;
                    LOG.error("error unfreezing " + softLock, e);
                }
                try {
                    softLock.unlock();
                    LOG.debug("unlocked {}", softLock);
                } catch (Exception e) {
                  success = false;
                  LOG.error("error unlocking " + softLock, e);
                }
            }
        }
        if (!success) {
            throw new TransactionException("Error unfreezing/unlocking transaction with ID " + transactionId);
        }
    }

    private void freeze() {
        for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
            List<SoftLock> softLocks = stringListEntry.getValue();

            for (SoftLock softLock : softLocks) {
                softLock.freeze();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return transactionId.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LocalTransactionContext) {
            LocalTransactionContext otherCtx = (LocalTransactionContext) obj;
            return transactionId.equals(otherCtx.transactionId);
        }
        return false;
    }

}
