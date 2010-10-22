package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author Ludovic Orban
 */
public interface SoftLockStore {

    ReadWriteLock getReadWriteLock(String cacheName);

    ConcurrentMap<Object,SoftLock> getSoftLockMap(String cacheName);

    SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement);

}
