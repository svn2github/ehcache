package net.sf.ehcache.transaction.local;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lorban
 */
public class ReadCommittedSoftLockImpl implements SoftLock {
    private final TransactionID transactionID;
    private final Object key;
    private Element newElement;
    private final Element oldElement;
    private final Lock lock;
    private final Lock freezeLock;

    ReadCommittedSoftLockImpl(TransactionID transactionID, Object key, Element newElement, Element oldElement) {
        this.transactionID = transactionID;
        this.key = key;
        this.newElement = newElement;
        this.oldElement = oldElement;
        this.lock = new ReentrantLock();
        this.freezeLock = new ReentrantLock();
    }

    private ReadCommittedSoftLockImpl(TransactionID transactionID, Object key, Element newElement, Element oldElement, Lock lock, Lock freezeLock) {
        this.transactionID = transactionID;
        this.key = key;
        this.newElement = newElement;
        this.oldElement = oldElement;
        this.lock = lock;
        this.freezeLock = freezeLock;
    }

    public Object getKey() {
        return key;
    }

    public Element getElement() {
        freezeLock.lock();
        try {
            return newElement;
        } finally {
            freezeLock.unlock();
        }
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

    public void lock() {
        lock.lock();
    }

    public boolean tryLock(long ms) throws InterruptedException {
        return lock.tryLock(ms, TimeUnit.MILLISECONDS);
    }

    public void unlock() {
        lock.unlock();
    }

    public boolean isLocked() {
        boolean locked = lock.tryLock();
        if (locked) {
            lock.unlock();
        }
        return !locked;
    }

    public SoftLock copy(Element oldElement, Element newElement) {
        return new ReadCommittedSoftLockImpl(transactionID, key, newElement, oldElement, lock, freezeLock);
    }

    public void freeze() {
        freezeLock.lock();
    }

    public void unfreeze() {
        freezeLock.unlock();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ReadCommittedSoftLockImpl) {
            ReadCommittedSoftLockImpl other = (ReadCommittedSoftLockImpl) object;

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
        return "[isolation: rc, transactionID: " + transactionID + ", key: " + key + ", newElement: " + newElement + ", oldElement: " + oldElement + "]";
    }
}
