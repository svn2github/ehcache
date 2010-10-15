package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.store.NonXaTransactionalStore;
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
    private final Map<String, NonXaTransactionalStore> storeMap = new HashMap<String, NonXaTransactionalStore>();

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

    public void registerSoftLock(String cacheName, NonXaTransactionalStore store, SoftLock softLock) {
        List<SoftLock> softLocks = softLockMap.get(cacheName);
        if (softLocks == null) {
            softLocks = new ArrayList<SoftLock>();
            softLockMap.put(cacheName, softLocks);
        }
        softLocks.add(softLock);
        storeMap.put(cacheName, store);
    }

    public void commit() {
        if (rollbackOnly) {
            rollback();
            throw new TransactionException("transaction was marked as rollback only, rolled back on commit");
        }

        for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
            String cacheName = stringListEntry.getKey();
            NonXaTransactionalStore store = storeMap.get(cacheName);
            List<SoftLock> softLocks = stringListEntry.getValue();
            for (SoftLock softLock : softLocks) {
                LOG.debug("committing {}", softLock);
                store.store(softLock.getNewElement().getKey(), softLock.getNewElement());
                softLock.unlock();
                store.remove(softLock);
            }
        }
        softLockMap.clear();
        storeMap.clear();
    }

    public void rollback() {
        for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
            String cacheName = stringListEntry.getKey();
            NonXaTransactionalStore store = storeMap.get(cacheName);
            List<SoftLock> softLocks = stringListEntry.getValue();
            for (SoftLock softLock : softLocks) {
                LOG.debug("rolling back {}", softLock);
                softLock.unlock();
                store.remove(softLock);
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
