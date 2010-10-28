package net.sf.ehcache.transaction.local;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

import java.util.Set;

/**
 * @author Ludovic Orban
 */
public interface SoftLockFactory {

    SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement, Element oldElement);

    void clearKey(Object key);

    Set<Object> getKeysToRemove(TransactionContext transactionContext, String cacheName);

    Set<Object> getKeysToAdd(TransactionContext transactionContext, String cacheName);

}
