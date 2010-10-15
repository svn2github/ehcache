package net.sf.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.store.chm.ConcurrentHashMap;
import net.sf.ehcache.transaction.nonxa.SoftLock;
import net.sf.ehcache.transaction.nonxa.TransactionContext;
import net.sf.ehcache.transaction.nonxa.TransactionException;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Ludovic Orban
 */
public abstract class AbstractNonXaTransactionalStore extends AbstractStore {

    protected final TransactionController transactionController;
    protected final String cacheName;
    protected final Store underlyingStore;
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    protected final ConcurrentMap<Object, SoftLock> softLockMap = new ConcurrentHashMap<Object, SoftLock>();

    protected AbstractNonXaTransactionalStore(TransactionController transactionController, String cacheName, Store underlyingStore) {
        this.transactionController = transactionController;
        this.cacheName = cacheName;
        this.underlyingStore = underlyingStore;
    }

    protected TransactionContext getCurrentTransactionContext() {
        TransactionContext currentTransactionContext = transactionController.getCurrentTransactionContext();
        if (currentTransactionContext == null) {
            throw new TransactionException("no transaction started");
        }
        return currentTransactionContext;
    }

    public boolean underlyingPut(Element element) throws CacheException {
        return underlyingStore.put(element);
    }

    public Element underlyingRemove(Object key) throws CacheException {
        return underlyingStore.remove(key);
    }

    public void release(SoftLock softLock) {
        lock.writeLock().lock();
        try {
            softLockMap.remove(softLock.getKey());
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected void tryLockSoftLock(SoftLock softLock) throws TransactionException {
        lock.writeLock().unlock();
        while (true) {
            try {
                boolean locked = softLock.tryLock(getCurrentTransactionContext().getTransactionTimeout());
                lock.writeLock().lock();
                if (!locked) {
                    throw new TransactionException("deadlock detected in cache [" + cacheName +
                            "] during transaction [" + getCurrentTransactionContext().getTransactionId() +
                            "], conflict: " + softLock);
                }
                break;
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
