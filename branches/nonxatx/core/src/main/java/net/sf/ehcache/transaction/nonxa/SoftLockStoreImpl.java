package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Ludovic Orban
 */
public class SoftLockStoreImpl implements SoftLockStore {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ConcurrentMap<Object,SoftLock> softLockMap = new ConcurrentHashMap<Object, SoftLock>();

    public SoftLockStoreImpl() {
        //
    }

    public ReadWriteLock getReadWriteLock() {
        return lock;
    }

    public ConcurrentMap<Object, SoftLock> getSoftLockMap() {
        return softLockMap;
    }

    public SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement, Element oldElement) {
        return new SoftLockImpl(transactionID, key, newElement, oldElement);
    }

}
