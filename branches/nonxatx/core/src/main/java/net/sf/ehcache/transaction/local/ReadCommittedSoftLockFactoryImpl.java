package net.sf.ehcache.transaction.local;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.chm.ConcurrentHashMap;
import net.sf.ehcache.transaction.TransactionID;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Ludovic Orban
 */
public class ReadCommittedSoftLockFactoryImpl implements SoftLockFactory {

    private final static Object MARKER = new Object();

    private final String cacheName;

    // actually all we need would be a ConcurrentSet...
    private final ConcurrentMap<ReadCommittedSoftLockImpl,Object> newKeyLocks = new ConcurrentHashMap<ReadCommittedSoftLockImpl, Object>();

    public ReadCommittedSoftLockFactoryImpl(String cacheName) {
        this.cacheName = cacheName;
    }

    public SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement, Element oldElement) {
        ReadCommittedSoftLockImpl softLock = new ReadCommittedSoftLockImpl(this, transactionID, key, newElement, oldElement);
        if (oldElement == null) {
            newKeyLocks.put(softLock, MARKER);
        }
        return softLock;
    }

    public void clear(SoftLock softLock) {
        newKeyLocks.remove(softLock);
    }

    public Set<Object> getKeysInvisibleInContext(TransactionContext currentTransactionContext) {
        Set<Object> invisibleKeys = new HashSet<Object>();

        // all new keys added into the store are invisible
        invisibleKeys.addAll(getNewKeys());

        List<SoftLock> currentTransactionContextSoftLocks = currentTransactionContext.getSoftLocks(cacheName);
        for (SoftLock softLock : currentTransactionContextSoftLocks) {
            if (softLock.getElement(currentTransactionContext.getTransactionId()) == null) {
                // if the soft lock's element is null in the current transaction then the key is invisible
                invisibleKeys.add(softLock.getKey());
            } else {
                // if the soft lock's element is not null in the current transaction then the key is visible
                invisibleKeys.remove(softLock.getKey());
            }
        }

        return invisibleKeys;
    }

    void clearSoftLock(ReadCommittedSoftLockImpl softLock) {
        newKeyLocks.remove(softLock);
    }

    private Set<Object> getNewKeys() {
        Set<Object> result = new HashSet<Object>();

        for (ReadCommittedSoftLockImpl softLock : newKeyLocks.keySet()) {
            if (!softLock.isExpired()) {
                result.add(softLock.getKey());
            }
        }

        return result;
    }

}
