package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.store.AbstractNonXaTransactionalStore;
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
    private final int transactionTimeout;
    private final TransactionID transactionId;
    private final Map<String, List<SoftLock>> softLockMap = new HashMap<String, List<SoftLock>>();
    private final Map<String, AbstractNonXaTransactionalStore> storeMap = new HashMap<String, AbstractNonXaTransactionalStore>();

    public TransactionContext(int transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
        this.transactionId = new TransactionID();
    }

    public int getTransactionTimeout() {
        return transactionTimeout;
    }

    public void setRollbackOnly(boolean rollbackOnly) {
        this.rollbackOnly = rollbackOnly;
    }

    public void registerSoftLock(String cacheName, AbstractNonXaTransactionalStore store, SoftLock softLock) {
        List<SoftLock> softLocks = softLockMap.get(cacheName);
        if (softLocks == null) {
            softLocks = new ArrayList<SoftLock>();
            softLockMap.put(cacheName, softLocks);
        }
        softLocks.add(softLock);
        storeMap.put(cacheName, store);
    }

    public List<Object> getPutKeys(String cacheName) {
        List<Object> result = new ArrayList<Object>();

        List<SoftLock> softLocks = softLockMap.get(cacheName);
        if (softLocks == null) {
            return result;
        }

        for (SoftLock softLock : softLocks) {
            if (softLock.getNewElement() != null) {
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

    public void commit() {
        if (rollbackOnly) {
            rollback();
            throw new TransactionException("transaction was marked as rollback only, rolled back on commit");
        }

        for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
            String cacheName = stringListEntry.getKey();
            AbstractNonXaTransactionalStore store = storeMap.get(cacheName);
            List<SoftLock> softLocks = stringListEntry.getValue();
            for (SoftLock softLock : softLocks) {
                LOG.debug("committing {}", softLock);
                if (softLock.getNewElement() != null) {
                    store.underlyingPut(softLock.getNewElement());
                } else {
                    store.underlyingRemove(softLock.getKey());
                }
                store.release(softLock);
                softLock.unlock();
            }
        }
        softLockMap.clear();
        storeMap.clear();
    }

    public void rollback() {
        for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
            String cacheName = stringListEntry.getKey();
            AbstractNonXaTransactionalStore store = storeMap.get(cacheName);
            List<SoftLock> softLocks = stringListEntry.getValue();
            for (SoftLock softLock : softLocks) {
                LOG.debug("rolling back {}", softLock);
                store.release(softLock);
                softLock.unlock();
            }
        }
        softLockMap.clear();
        storeMap.clear();
    }

    public TransactionID getTransactionId() {
        return transactionId;
    }

    @Override
    public int hashCode() {
        return transactionId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TransactionContext) {
            TransactionContext otherCtx = (TransactionContext) obj;
            return transactionId == otherCtx.transactionId;
        }
        return false;
    }

}
