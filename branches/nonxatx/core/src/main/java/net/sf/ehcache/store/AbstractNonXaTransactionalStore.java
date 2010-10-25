package net.sf.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.transaction.local.SoftLock;
import net.sf.ehcache.transaction.local.SoftLockStore;
import net.sf.ehcache.transaction.local.TransactionContext;
import net.sf.ehcache.transaction.local.TransactionException;
import net.sf.ehcache.transaction.local.DeadLockException;
import net.sf.ehcache.transaction.local.TransactionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author Ludovic Orban
 */
public abstract class AbstractNonXaTransactionalStore extends AbstractStore {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNonXaTransactionalStore.class.getName());

    protected final TransactionController transactionController;
    protected final String cacheName;
    protected final Store underlyingStore;

    protected final ReadWriteLock lock;
    protected final SoftLockStore softLockStore;

    protected AbstractNonXaTransactionalStore(TransactionController transactionController, SoftLockStore softLockStore, String cacheName, Store underlyingStore) {
        this.transactionController = transactionController;
        this.softLockStore = softLockStore;
        this.lock = softLockStore.getReadWriteLock();
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

    protected boolean isTimedOut() {
        return System.currentTimeMillis() >= getCurrentTransactionContext().getExpirationTimestamp();
    }

    protected void assertNotTimedOut() {
        if (isTimedOut()) {
            throw new TransactionTimeoutException("transaction [" + getCurrentTransactionContext().getTransactionId() + "] timed out");
        }
    }

    public void commit(Collection<SoftLock> softLocks) {
        lock.writeLock().lock();
        try {
            LOG.debug("cache [{}] has {} soft lock(s) to commit", cacheName, softLocks.size());
            for (SoftLock softLock : softLocks) {
                LOG.debug("committing {}", softLock);
                Element newElement = softLock.getNewElement();
                if (newElement != null) {
                    underlyingStore.put(newElement);
                } else {
                    underlyingStore.remove(softLock.getKey());
                }
                softLock.unlock();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void rollback(Collection<SoftLock> softLocks) {
        lock.writeLock().lock();
        try {
            LOG.debug("cache [{}] has {} soft lock(s) to rollback", cacheName, softLocks.size());
            for (SoftLock softLock : softLocks) {
                LOG.debug("rolling back {}", softLock);
                Element oldElement = softLock.getOldElement();
                if (oldElement != null) {
                    underlyingStore.put(oldElement);
                } else {
                    underlyingStore.remove(softLock.getKey());
                }
                softLock.unlock();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * The softlock is 'dead' after this method returns, ie: it doesn't protect anything anymore,
     * unless a DeadLockException is thrown.
     */
    protected void tryLockSoftLock(SoftLock softLock) throws DeadLockException {
        lock.writeLock().unlock();
        while (true) {
            try {
                boolean locked = softLock.tryLock();
                lock.writeLock().lock();
                if (locked) {
                    softLock.unlock();
                } else {
                    throw new DeadLockException("deadlock detected in cache [" + cacheName +
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
