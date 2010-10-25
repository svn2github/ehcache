package net.sf.ehcache.transaction.local;

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
    private final long expirationTimestamp;
    private final Object key;
    private Element newElement;
    private final Element oldElement;
    private final Lock lock;

    SoftLockImpl(TransactionID transactionID, long expirationTimestamp, Object key, Element newElement, Element oldElement) {
        this.transactionID = transactionID;
        this.expirationTimestamp = expirationTimestamp;
        this.key = key;
        this.newElement = newElement;
        this.oldElement = oldElement;
        this.lock = new ReentrantLock();
    }

    private SoftLockImpl(TransactionID transactionID, long expirationTimestamp, Object key, Element newElement, Element oldElement, Lock lock) {
        this.expirationTimestamp = expirationTimestamp;
        this.transactionID = transactionID;
        this.key = key;
        this.newElement = newElement;
        this.oldElement = oldElement;
        this.lock = lock;
    }

    public Object getKey() {
        return key;
    }

    public Element getOldElement() {
        return oldElement;
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

    public long getExpirationTimestamp() {
        return expirationTimestamp;
    }

    public void lock() {
        lock.lock();
    }

    public boolean waitForRelease() throws InterruptedException {
        long msBeforeTimeout = expirationTimestamp - System.currentTimeMillis();
        if (msBeforeTimeout <= 0) {
            return true;
        }
        boolean locked = lock.tryLock(expirationTimestamp, TimeUnit.MILLISECONDS);
        if (locked) {
            lock.unlock();
        }
        return locked;
    }

    public void unlock() {
        lock.unlock();
    }

    public SoftLock copy() {
        return new SoftLockImpl(transactionID, expirationTimestamp, key, newElement, oldElement, lock);
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
        return "[transactionID: " + transactionID + ", key: " + key + ", newElement: " + newElement + ", oldElement: " + oldElement + "]";
    }
}
