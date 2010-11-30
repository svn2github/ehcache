/**
 *  Copyright 2003-2010 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.ehcache.transaction.local;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.chm.ConcurrentHashMap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * A SoftLockFactory implementation which creates soft locks with Read-Committed isolation level
 *
 * @author Ludovic Orban
 */
public class ReadCommittedSoftLockFactoryImpl implements SoftLockFactory {

    private final static Object MARKER = new Object();

    private final String cacheName;

    // actually all we need would be a ConcurrentSet...
    private final ConcurrentMap<ReadCommittedSoftLockImpl, Object> newKeyLocks = new ConcurrentHashMap<ReadCommittedSoftLockImpl, Object>();

    /**
     * Create a new ReadCommittedSoftLockFactoryImpl instance for a cache
     * @param cacheName the name of the cache
     */
    public ReadCommittedSoftLockFactoryImpl(String cacheName) {
        this.cacheName = cacheName;
    }

    /**
     * {@inheritDoc}
     */
    public SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement, Element oldElement) {
        ReadCommittedSoftLockImpl softLock = new ReadCommittedSoftLockImpl(this, transactionID, key, newElement, oldElement);
        if (oldElement == null) {
            newKeyLocks.put(softLock, MARKER);
        }
        return softLock;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Object> getKeysInvisibleInContext(TransactionContext currentTransactionContext) {
        Set<Object> invisibleKeys = new HashSet<Object>();

        // all new keys added into the store are invisible
        invisibleKeys.addAll(getNewKeys());

        List<SoftLock> currentTransactionContextSoftLocks = currentTransactionContext.getSoftLocksForCache(cacheName);
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

    /**
     * Callback when a soft lock died
     * @param softLock the soft lock which died
     */
    void clearSoftLock(ReadCommittedSoftLockImpl softLock) {
        newKeyLocks.remove(softLock);
    }

    private Set<Object> getNewKeys() {
        Set<Object> result = new HashSet<Object>();

        for (ReadCommittedSoftLockImpl softLock : newKeyLocks.keySet()) {
            result.add(softLock.getKey());
        }

        return result;
    }

}
