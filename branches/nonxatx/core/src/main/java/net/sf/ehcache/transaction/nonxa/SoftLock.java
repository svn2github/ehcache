package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.Element;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.store.NonXaTransactionalStore;

import java.io.Serializable;

/**
 * @author lorban
 */
public class SoftLock {
    private final NonXaTransactionalStore store;
    private final TransactionContext transactionContext;
    private Element newElement;

    public SoftLock(NonXaTransactionalStore store, TransactionContext transactionContext, Element newElement) {
        this.store = store;
        this.transactionContext = transactionContext;
        this.newElement = newElement;
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

/*
    public boolean tryLock(int timeoutInSeconds) throws InterruptedException {
        return getTransactionContext().tryLock(this, timeoutInSeconds);
    }

    public void unlock() {
        getTransactionContext().unlock(this);
    }
*/

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
        return "SoftLock [transactionContext: " + transactionContext + ", newElement: " + newElement + "]";
    }
}
