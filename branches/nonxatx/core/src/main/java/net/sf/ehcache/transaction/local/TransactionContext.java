package net.sf.ehcache.transaction.local;

import net.sf.ehcache.transaction.TransactionID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lorban
 */
public class TransactionContext {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionContext.class.getName());

    private boolean rollbackOnly;
    private final long expirationTimestamp;
    private final TransactionID transactionId;
    private final Map<String, List<SoftLock>> softLockMap = new HashMap<String, List<SoftLock>>();
    private final Map<String, LocalTransactionStore> storeMap = new HashMap<String, LocalTransactionStore>();
    private final List<TransactionListener> listeners = new ArrayList<TransactionListener>();

    public TransactionContext(int transactionTimeout, TransactionID transactionId) {
        this.expirationTimestamp = System.currentTimeMillis() + transactionTimeout * 1000L;
        this.transactionId = transactionId;
    }

    public long getExpirationTimestamp() {
        return expirationTimestamp;
    }

    public boolean timedOut() {
        return expirationTimestamp <= System.currentTimeMillis();
    }

    public void setRollbackOnly(boolean rollbackOnly) {
        this.rollbackOnly = rollbackOnly;
    }

    public void registerSoftLock(String cacheName, LocalTransactionStore store, SoftLock softLock) {
        List<SoftLock> softLocks = softLockMap.get(cacheName);
        if (softLocks == null) {
            softLocks = new ArrayList<SoftLock>();
            softLockMap.put(cacheName, softLocks);
            storeMap.put(cacheName, store);
        }
        softLocks.add(softLock);
    }

    public void updateSoftLock(String cacheName, SoftLock softLock) {
        List<SoftLock> softLocks = softLockMap.get(cacheName);
        softLocks.remove(softLock);
        softLocks.add(softLock);
    }

    public List<Object> getNewKeys(String cacheName) {
        List<Object> result = new ArrayList<Object>();

        List<SoftLock> softLocks = softLockMap.get(cacheName);
        if (softLocks == null) {
            return result;
        }

        for (SoftLock softLock : softLocks) {
            if (softLock.getNewElement() != null && softLock.getOldElement() == null) {
                result.add(softLock.getKey());
            }
        }

        return result;
    }

    public List<Object> getUpdatedKeys(String cacheName) {
        List<Object> result = new ArrayList<Object>();

        List<SoftLock> softLocks = softLockMap.get(cacheName);
        if (softLocks == null) {
            return result;
        }

        for (SoftLock softLock : softLocks) {
            if (softLock.getNewElement() != null && softLock.getOldElement() != null) {
                result.add(softLock.getKey());
            }
        }

        return result;
    }

    public List<Object> getRemovedKeys(String cacheName) {
        List<Object> result = new ArrayList<Object>();

        List<SoftLock> softLocks = softLockMap.get(cacheName);
        if (softLocks == null) {
            return result;
        }

        for (SoftLock softLock : softLocks) {
            if (softLock.getNewElement() == null) {
                result.add(softLock.getKey());
            }
        }

        return result;
    }

    public void commit(boolean ignoreTimeout) {
        if (rollbackOnly) {
            rollback();
            throw new TransactionException("transaction was marked as rollback only, rolled back on commit");
        }
        if (!ignoreTimeout && timedOut()) {
            rollback();
            throw new TransactionTimeoutException("transaction timed out, rolled back on commit");
        }

        try {
            fireBeforeCommitEvent();

            LOG.debug("{} cache(s) participated in transaction", softLockMap.keySet().size());
            for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
                List<SoftLock> softLocks = stringListEntry.getValue();

                for (SoftLock softLock : softLocks) {
                    softLock.freeze();
                }
            }

            for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
                String cacheName = stringListEntry.getKey();
                LocalTransactionStore store = storeMap.get(cacheName);
                List<SoftLock> softLocks = stringListEntry.getValue();

                store.commit(softLocks);
            }

            for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
                List<SoftLock> softLocks = stringListEntry.getValue();

                for (SoftLock softLock : softLocks) {
                    softLock.unlock();
                    softLock.unfreeze();
                }
            }
        } finally {
            softLockMap.clear();
            storeMap.clear();
            fireAfterCommitEvent();
        }
    }

    public void rollback() {
        try {
            LOG.debug("{} cache(s) participated in transaction", softLockMap.keySet().size());
            for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
                List<SoftLock> softLocks = stringListEntry.getValue();

                for (SoftLock softLock : softLocks) {
                    softLock.freeze();
                }
            }

            for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
                String cacheName = stringListEntry.getKey();
                LocalTransactionStore store = storeMap.get(cacheName);
                List<SoftLock> softLocks = stringListEntry.getValue();

                store.rollback(softLocks);
            }

            for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
                List<SoftLock> softLocks = stringListEntry.getValue();

                for (SoftLock softLock : softLocks) {
                    softLock.unlock();
                    softLock.unfreeze();
                }
            }
        } finally {
            softLockMap.clear();
            storeMap.clear();
            fireAfterRollbackEvent();
        }
    }

    public TransactionID getTransactionId() {
        return transactionId;
    }

    public void addListener(TransactionListener listener) {
        this.listeners.add(listener);
    }

    private void fireBeforeCommitEvent() {
        for (TransactionListener listener : listeners) {
            listener.beforeCommit();
        }
    }

    private void fireAfterCommitEvent() {
        for (TransactionListener listener : listeners) {
            listener.afterCommit();
        }
    }

    private void fireAfterRollbackEvent() {
        for (TransactionListener listener : listeners) {
            listener.afterRollback();
        }
    }    

    @Override
    public int hashCode() {
        return transactionId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TransactionContext) {
            TransactionContext otherCtx = (TransactionContext) obj;
            return transactionId.equals(otherCtx.transactionId);
        }
        return false;
    }

}
