package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lorban
 */
public class SoftLockImpl implements SoftLock {
    private final TransactionID transactionID;
    private final Object key;
    private Element newElement;
    private final Lock lock = new ReentrantLock();

    SoftLockImpl(TransactionID transactionID, Object key, Element newElement) {
        this.transactionID = transactionID;
        this.key = key;
        this.newElement = newElement;
    }

    public Object getKey() {
        return key;
    }

    public Element getNewElement() {
        return newElement;
    }

    public void setNewElement(Element newElement) {
        this.newElement = newElement;
    }

    public TransactionID getTransactionID() {
        return transactionID;
    }

    public void lock() {
        lock.lock();
    }

    public boolean tryLock(int transactionTimeout) throws InterruptedException {
        return lock.tryLock(transactionTimeout, TimeUnit.SECONDS);
    }

    public void unlock() {
        lock.unlock();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof SoftLockImpl) {
            SoftLockImpl other = (SoftLockImpl) object;

            if (!transactionID.equals(other.transactionID)) {
                return false;
            }

            if (!key.equals(other.key)) {
                return false;
            }

            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 31;

        hashCode *= transactionID.hashCode();
        hashCode *= key.hashCode();

        return hashCode;
    }

    @Override
    public String toString() {
        return "[transactionID: " + transactionID + ", key: " + key + ", newElement: " + newElement + "]";
    }
}
