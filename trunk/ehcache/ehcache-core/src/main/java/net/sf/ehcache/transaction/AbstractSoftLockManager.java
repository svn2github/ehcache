/**
 *  Copyright Terracotta, Inc.
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

package net.sf.ehcache.transaction;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.local.LocalTransactionContext;

/**
 * An abstract map backed soft lock manager.
 *
 * @author Chris Dennis
 */
public abstract class AbstractSoftLockManager implements SoftLockManager {

    private final String cacheName;
    private final SoftLockFactory lockFactory;

    /**
     * Create an abstract soft lock manager for the given cache name and soft lock factory.
     *
     * @param cacheName name of the cache
     * @param lockFactory factory of managed locks
     */
    public AbstractSoftLockManager(String cacheName, SoftLockFactory lockFactory) {
        this.cacheName = cacheName;
        this.lockFactory = lockFactory;
    }

    /**
     * Return the map of all soft locks.
     *
     * @return the map of all locks
     */
    protected abstract ConcurrentMap<SoftLockID, SoftLock> getAllLocks();

    /**
     * Return the map of all locks that are for new keys.
     *
     * @return the map of all new key locks
     */
    protected abstract ConcurrentMap<SoftLockID, Boolean> getNewKeyLocks();

    /**
     * {@inheritDoc}
     */
    public SoftLockID createSoftLockID(TransactionID transactionID, Object key, Element newElement, Element oldElement) {
        if (newElement != null && newElement.getObjectValue() instanceof SoftLockID) {
            throw new AssertionError("newElement must not contain a soft lock ID");
        }
        if (oldElement != null && oldElement.getObjectValue() instanceof SoftLockID) {
            throw new AssertionError("oldElement must not contain a soft lock ID");
        }

        SoftLockID lockId = new SoftLockID(transactionID, key, newElement, oldElement);

        if (getAllLocks().containsKey(lockId)) {
            return lockId;
        } else {
            SoftLock lock = lockFactory.newSoftLock(this, key);

            if (getAllLocks().putIfAbsent(lockId, lock) != null) {
                throw new AssertionError();
            } else {
                if (oldElement == null) {
                    getNewKeyLocks().put(lockId, Boolean.TRUE);
                }
                return lockId;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public SoftLock findSoftLockById(SoftLockID softLockId) {
        return getAllLocks().get(softLockId);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Object> getKeysInvisibleInContext(LocalTransactionContext currentTransactionContext, Store underlyingStore) {
        Set<Object> invisibleKeys = new HashSet<Object>();

        // all new keys added into the store are invisible
        invisibleKeys.addAll(getNewKeys());

        List<SoftLock> currentTransactionContextSoftLocks = currentTransactionContext.getSoftLocksForCache(cacheName);
        for (SoftLock softLock : currentTransactionContextSoftLocks) {
            Element e = underlyingStore.getQuiet(softLock.getKey());
            if (e.getObjectValue() instanceof SoftLockID) {
                SoftLockID softLockId = (SoftLockID) e.getObjectValue();
                if (softLock.getElement(currentTransactionContext.getTransactionId(), softLockId) == null) {
                    // if the soft lock's element is null in the current transaction then the key is invisible
                    invisibleKeys.add(softLock.getKey());
                } else {
                    // if the soft lock's element is not null in the current transaction then the key is visible
                    invisibleKeys.remove(softLock.getKey());
                }
            }
        }

        return invisibleKeys;
    }

    /**
     * {@inheritDoc}
     */
    public Set<SoftLock> collectAllSoftLocksForTransactionID(TransactionID transactionID) {
        Set<SoftLock> result = new HashSet<SoftLock>();

        for (Entry<SoftLockID, SoftLock> entry : getAllLocks().entrySet()) {
            if (entry.getKey().getTransactionID().equals(transactionID)) {
                result.add(entry.getValue());
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void clearSoftLock(SoftLock softLock) {

        for (Map.Entry<SoftLockID, SoftLock> entry : getAllLocks().entrySet()) {
            if (entry.getValue() == softLock) {
                getAllLocks().remove(entry.getKey());
                getNewKeyLocks().remove(entry.getKey());
                break;
            }
        }
    }

    private Set<Object> getNewKeys() {
        Set<Object> result = new HashSet<Object>();

        for (SoftLockID softLock : getNewKeyLocks().keySet()) {
            result.add(softLock.getKey());
        }

        return result;
    }
}
