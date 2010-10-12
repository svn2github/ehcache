package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.Element;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.store.NonXaTransactionalStore;

import java.io.Serializable;

/**
 * @author lorban
 */
public class SoftLock implements Serializable {
    private final TransactionID txId;
    private final String cacheName;
    private final Element oldElement;
    private final Element newElement;

    public SoftLock(TransactionContext transactionContext, String cacheName, Element oldElement, Element newElement) {
        this.txId = transactionContext.getTransactionId();
        this.cacheName = cacheName;
        this.oldElement = oldElement;
        this.newElement = newElement;

        transactionContext.put(cacheName, newElement.getKey(), this);
        transactionContext.lock(this);
    }

    public Element getOldElement() {
        return oldElement;
    }

    public Element getNewElement() {
        return newElement;
    }

    public boolean inContext(TransactionContext ctx) {
        return txId.equals(ctx.getTransactionId());
    }

    private TransactionContext getTransactionContext() {
        return TransactionController.getInstance().getTransactionContext(txId);
    }

    public boolean tryLock(int timeoutInSeconds) throws InterruptedException {
        return getTransactionContext().tryLock(this, timeoutInSeconds);
    }

    public void unlock() {
        getTransactionContext().unlock(this);
    }

    public void commit() {
        NonXaTransactionalStore transactionalStore = getTransactionContext().getTransactionalStore(cacheName);
        transactionalStore.store(newElement.getKey(), newElement);
        getTransactionContext().unlock(this);
    }

    public void rollback() {
        NonXaTransactionalStore transactionalStore = getTransactionContext().getTransactionalStore(cacheName);
        transactionalStore.store(newElement.getKey(), oldElement);
        getTransactionContext().unlock(this);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof SoftLock) {
            SoftLock other = (SoftLock) object;

            if (!cacheName.equals(other.cacheName)) {
                return false;
            }

            if (!txId.equals(other.txId)) {
                return false;
            }

            if (oldElement != null) {
                if (!oldElement.equals(other.oldElement)) {
                    return false;
                }
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

        hashCode *= cacheName.hashCode();
        hashCode *= txId.hashCode();

        if (oldElement != null) {
            hashCode *= oldElement.hashCode();
        }

        if (newElement != null) {
            hashCode *= newElement.hashCode();
        }

        return hashCode;
    }

    @Override
    public String toString() {
        return "SoftLock [cacheName: " + cacheName + ", txId: " + txId + ", oldElement: " + oldElement + ", newElement: " + newElement + "]";
    }
}
