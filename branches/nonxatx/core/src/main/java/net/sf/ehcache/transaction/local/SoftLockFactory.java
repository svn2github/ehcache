package net.sf.ehcache.transaction.local;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author Ludovic Orban
 */
public interface SoftLockFactory {

    SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement, Element oldElement);

}
