package net.sf.ehcache.transaction.nonxa;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author Ludovic Orban
 */
public interface SoftLockStore {

    ReadWriteLock getReadWriteLock();

    ConcurrentMap<Object,SoftLock> getSoftLockMap();

}
