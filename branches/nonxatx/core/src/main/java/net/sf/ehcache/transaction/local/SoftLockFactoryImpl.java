package net.sf.ehcache.transaction.local;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.chm.ConcurrentHashMap;
import net.sf.ehcache.transaction.TransactionID;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Ludovic Orban
 */
public class SoftLockFactoryImpl implements SoftLockFactory {

    private final static Object MARKER = new Object();

    // actually all we need would be a ConcurrentSet...
    private final ConcurrentMap<Object,Object> newKeys = new ConcurrentHashMap<Object, Object>();

    public SoftLockFactoryImpl() {
        //
    }

    public SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement, Element oldElement) {
        ReadCommittedSoftLockImpl softLock = new ReadCommittedSoftLockImpl(transactionID, key, newElement, oldElement);
        if (oldElement == null) {
            newKeys.put(key, MARKER);
        }
        return softLock;
    }

    public Set<Object> getNewKeys() {
        return newKeys.keySet();
    }

    public void clearNewKey(Object key) {
        newKeys.remove(key);
    }

}
