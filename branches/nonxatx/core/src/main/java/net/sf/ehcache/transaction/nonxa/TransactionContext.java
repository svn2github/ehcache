package net.sf.ehcache.transaction.nonxa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
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
    private final ConcurrentMap<String, ConcurrentMap<Object, SoftLock>> softLocksMap = new ConcurrentHashMap<String, ConcurrentMap<Object, SoftLock>>();

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

    public ConcurrentMap<Object, SoftLock> getOrCreateSoftLocksMap(String cacheName) {
        ConcurrentMap<Object, SoftLock> map = softLocksMap.get(cacheName);
        if (map == null) {
            map = new ConcurrentHashMap<Object, SoftLock>();
            softLocksMap.put(cacheName, map);
        }
        return map;
    }



    public void commit() {
        if (rollbackOnly) {
            rollback();
            throw new TransactionException("transaction was marked as rollback only, rolled back on commit");
        }

        for (Map.Entry<String, ConcurrentMap<Object, SoftLock>> stringMapEntry : softLocksMap.entrySet()) {
            Set<Map.Entry<Object, SoftLock>> entries = stringMapEntry.getValue().entrySet();
            for (Map.Entry<Object, SoftLock> entry : entries) {
                LOG.debug("committing {}", entry.getValue());
                entry.getValue().commitNewElement();
            }
        }
        softLocksMap.clear();
    }

    public void rollback() {
        softLocksMap.clear();
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
