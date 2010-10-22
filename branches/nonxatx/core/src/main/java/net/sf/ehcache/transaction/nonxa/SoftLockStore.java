package net.sf.ehcache.transaction.nonxa;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author Ludovic Orban
 */
public interface SoftLockStore {

    ReadWriteLock getReadWriteLock(String cacheName);

    ConcurrentMap<Object,SoftLock> getSoftLockMap(String cacheName);

    SoftLockFactory getSoftLockFactory();

}
