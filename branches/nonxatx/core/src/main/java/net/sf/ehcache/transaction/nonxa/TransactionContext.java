package net.sf.ehcache.transaction.nonxa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author lorban
 */
public class TransactionContext {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionContext.class.getName());

    private boolean rollbackOnly;
    private final int transactionTimeout;
    private final TransactionID transactionId;
    private final ConcurrentMap<String, List<SoftLock>> softLockMap = new ConcurrentHashMap<String, List<SoftLock>>();

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

    public void add(String cacheName, SoftLock softLock) {
        List<SoftLock> softLocks = softLockMap.get(cacheName);
        if (softLocks == null) {
            softLocks = new ArrayList<SoftLock>();
            softLockMap.put(cacheName, softLocks);
        }
        softLocks.add(softLock);
    }

    public void commit() {
        if (rollbackOnly) {
            rollback();
            throw new TransactionException("transaction was marked as rollback only, rolled back on commit");
        }

        for (Map.Entry<String, List<SoftLock>> stringListEntry : softLockMap.entrySet()) {
            List<SoftLock> softLocks = stringListEntry.getValue();
            for (SoftLock softLock : softLocks) {
                LOG.debug("committing {}", softLock);
                softLock.commitNewElement();
            }
        }
        softLockMap.clear();
    }

    public void rollback() {
        softLockMap.clear();
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
