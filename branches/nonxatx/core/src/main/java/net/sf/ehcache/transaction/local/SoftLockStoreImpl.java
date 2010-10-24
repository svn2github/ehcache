package net.sf.ehcache.transaction.local;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Ludovic Orban
 */
public class SoftLockStoreImpl implements SoftLockStore {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public SoftLockStoreImpl() {
        //
    }

    public ReadWriteLock getReadWriteLock() {
        return lock;
    }

    public SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement, Element oldElement) {
        return new SoftLockImpl(transactionID, key, newElement, oldElement);
    }

}
