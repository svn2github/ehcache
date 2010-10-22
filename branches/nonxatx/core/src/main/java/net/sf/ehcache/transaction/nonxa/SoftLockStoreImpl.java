package net.sf.ehcache.transaction.nonxa;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Ludovic Orban
 */
public class SoftLockStoreImpl implements SoftLockStore {

    private final ConcurrentMap<String,ReadWriteLock> locks = new ConcurrentHashMap<String, ReadWriteLock>();
    private final ConcurrentMap<String,ConcurrentMap<Object,SoftLock>> softLockMaps = new ConcurrentHashMap<String, ConcurrentMap<Object, SoftLock>>();
    private final SoftLockFactory softLockFactory = new SoftLockFactoryImpl();

    public SoftLockStoreImpl() {
        //
    }

    public ReadWriteLock getReadWriteLock(String cacheName) {
        ReadWriteLock lock = locks.get(cacheName);
        if (lock == null) {
            lock = new ReentrantReadWriteLock();
            locks.put(cacheName, lock);
        }
        return lock;
    }

    public ConcurrentMap<Object, SoftLock> getSoftLockMap(String cacheName) {
        ConcurrentMap<Object,SoftLock> softLockMap = softLockMaps.get(cacheName);
        if (softLockMap == null) {
            softLockMap = new ConcurrentHashMap<Object, SoftLock>();
            softLockMaps.put(cacheName, softLockMap);
        }
        return softLockMap;
    }

    public SoftLockFactory getSoftLockFactory() {
        return softLockFactory;
    }
}
