package net.sf.ehcache.transaction.local;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.chm.ConcurrentHashMap;
import net.sf.ehcache.transaction.TransactionID;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Ludovic Orban
 */
public class ReadCommittedSoftLockFactoryImpl implements SoftLockFactory {

    private final static Object MARKER = new Object();

    // actually all we need would be a ConcurrentSet...
    private final ConcurrentMap<Object,Object> newKeys = new ConcurrentHashMap<Object, Object>();

    public ReadCommittedSoftLockFactoryImpl() {
        //
    }

    public SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement, Element oldElement) {
        ReadCommittedSoftLockImpl softLock = new ReadCommittedSoftLockImpl(transactionID, key, newElement, oldElement);
        if (oldElement == null) {
            newKeys.put(key, MARKER);
        }
        return softLock;
    }

    private Set<Object> getNewKeys() {
        return newKeys.keySet();
    }

    public void clearKey(Object key) {
        newKeys.remove(key);
    }

    public Set<Object> getKeysToRemove(TransactionContext transactionContext, String cacheName) {
        Set<Object> keysToRemove = new HashSet<Object>();
        keysToRemove.addAll(getNewKeys());
        keysToRemove.removeAll(transactionContext.getNewKeys(cacheName));
        keysToRemove.addAll(transactionContext.getRemovedKeys(cacheName));
        return keysToRemove;
    }

    public Set<Object> getKeysToAdd(TransactionContext transactionContext, String cacheName) {
        return Collections.emptySet();
    }

}
