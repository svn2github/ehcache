package net.sf.ehcache.transaction.nonxa;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Ludovic Orban
 */
public class SoftLockStoreImpl implements SoftLockStore {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ConcurrentMap<Object, SoftLock> softLockMap = new ConcurrentHashMap<Object, SoftLock>();


    public SoftLockStoreImpl() {
        //
    }

    public ReadWriteLock getReadWriteLock() {
        return lock;
    }

    public ConcurrentMap<Object, SoftLock> getSoftLockMap() {
        return softLockMap;
    }
}
