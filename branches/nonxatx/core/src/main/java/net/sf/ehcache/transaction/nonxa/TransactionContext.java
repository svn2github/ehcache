package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.store.NonXaTransactionalStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lorban
 */
public class TransactionContext {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionContext.class.getName());

    private boolean rollbackOnly;
    private final CacheManager cacheManager;
    private final int transactionTimeout;
    private final TransactionID transactionId;
    private final ConcurrentMap<String, ConcurrentMap<Object, SoftLock>> softLocksMap = new ConcurrentHashMap<String, ConcurrentMap<Object, SoftLock>>();
    private final ConcurrentMap<SoftLock, Lock> locksMap = new ConcurrentHashMap<SoftLock, Lock>();

    public TransactionContext(CacheManager cacheManager, int transactionTimeout) {
        this.cacheManager = cacheManager;
        this.transactionTimeout = transactionTimeout;
        this.transactionId = new TransactionID();
    }

    public int getTransactionTimeout() {
        return transactionTimeout;
    }

    public void setRollbackOnly(boolean rollbackOnly) {
        this.rollbackOnly = rollbackOnly;
    }

    public void put(String cacheName, Object key, SoftLock softLock) {
        ConcurrentMap<Object, SoftLock> map = softLocksMap.get(cacheName);
        if (map == null) {
            map = new ConcurrentHashMap<Object, SoftLock>();
            softLocksMap.put(cacheName, map);
        }
        map.put(key, softLock);
        locksMap.put(softLock, new ReentrantLock());
    }

    protected void lock(SoftLock softLock) {
        locksMap.get(softLock).lock();
    }

    public boolean tryLock(SoftLock softLock, int timeOutInSeconds) throws InterruptedException {
        return locksMap.get(softLock).tryLock(timeOutInSeconds, TimeUnit.SECONDS);
    }

    public void unlock(SoftLock softLock) {
        locksMap.get(softLock).unlock();
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
                entry.getValue().commit();
            }
        }
        softLocksMap.clear();
        locksMap.clear();
    }

    public void rollback() {
        for (Map.Entry<String, ConcurrentMap<Object, SoftLock>> stringMapEntry : softLocksMap.entrySet()) {
            Set<Map.Entry<Object, SoftLock>> entries = stringMapEntry.getValue().entrySet();
            for (Map.Entry<Object, SoftLock> entry : entries) {
                entry.getValue().rollback();
            }
        }
        softLocksMap.clear();
        locksMap.clear();
    }

    public NonXaTransactionalStore getTransactionalStore(String cacheName) {
        return (NonXaTransactionalStore) cacheManager.getCache(cacheName).getStore();
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
