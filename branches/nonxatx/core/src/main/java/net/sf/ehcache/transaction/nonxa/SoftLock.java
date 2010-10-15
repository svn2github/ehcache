package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.NonXaTransactionalStore;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lorban
 */
public class SoftLock {
    private final NonXaTransactionalStore store;
    private final TransactionContext transactionContext;
    private Element newElement;
    private final Lock lock = new ReentrantLock();

    public SoftLock(NonXaTransactionalStore store, TransactionContext transactionContext, Element newElement) {
        this.store = store;
        this.transactionContext = transactionContext;
        this.newElement = newElement;
        lock.lock();
    }

    public Element getNewElement() {
        return newElement;
    }

    public void setNewElement(Element newElement) {
        this.newElement = newElement;
    }

    public TransactionID getTransactionID() {
        return transactionContext.getTransactionId();
    }

    public boolean tryLock(int transactionTimeout) throws InterruptedException {
        return lock.tryLock(transactionTimeout, TimeUnit.SECONDS);
    }

    public void unlock() {
        lock.unlock();
    }

    public void commitNewElement() {
        store.store(newElement.getKey(), newElement);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof SoftLock) {
            SoftLock other = (SoftLock) object;

            if (!transactionContext.equals(other.transactionContext)) {
                return false;
            }

            if (newElement != null) {
                if (!newElement.equals(other.newElement)) {
                    return false;
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 31;

        hashCode *= transactionContext.hashCode();

        if (newElement != null) {
            hashCode *= newElement.hashCode();
        }

        return hashCode;
    }

    @Override
    public String toString() {
        return "SoftLock [transactionID: " + transactionContext.getTransactionId() + ", newElement: " + newElement + "]";
    }
}
