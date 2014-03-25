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

import net.sf.ehcache.Element;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Store;

/**
 * A helper class containing the logic to commit or rollback a soft lock in a store.
 *
 * @author Ludovic Orban
 */
public class SoftLockHelper {

    /**
     * Commit a soft lock.
     *
     * @param softLock        the soft lock to commit.
     * @param underlyingStore the underlying store.
     * @param comparator      the element value comparator.
     */
    public static void commit(SoftLock softLock, Store underlyingStore, ElementValueComparator comparator) {
        Element e = underlyingStore.getQuiet(softLock.getKey());
        if (e == null || !(e.getObjectValue() instanceof SoftLockID)) {
            // the element can be null or not a soft lock if it was manually unpinned, see DEV-8308
            return;
        }
        SoftLockID softLockId = (SoftLockID)e.getObjectValue();

        // must unpin before replacing the softlock ID to avoid racily unpinning another transaction's element
        if (!softLockId.wasPinned()) {
            underlyingStore.setPinned(softLock.getKey(), false);
        }

        Element element = softLockId.getNewElement();
        if (element != null) {
            underlyingStore.replace(e, element, comparator);
        } else {
            underlyingStore.removeElement(e, comparator);
        }
    }

    /**
     * Rollback a soft lock.
     *
     * @param softLock        the soft lock to commit.
     * @param underlyingStore the underlying store.
     * @param comparator      the element value comparator.
     * @see net.sf.ehcache.transaction.xa.commands.AbstractStoreCommand#rollback(net.sf.ehcache.store.Store, SoftLockManager, net.sf.ehcache.store.ElementValueComparator)
     * phase 1 rollback of the 2PC implementation
     */
    public static void rollback(SoftLock softLock, Store underlyingStore, ElementValueComparator comparator) {
        Element e = underlyingStore.getQuiet(softLock.getKey());
        if (e == null || !(e.getObjectValue() instanceof SoftLockID)) {
            // the element can be null or not a soft lock if it was manually unpinned, see DEV-8308
            return;
        }
        SoftLockID softLockId = (SoftLockID)e.getObjectValue();

        // must unpin before replacing the softlock ID to avoid racily unpinning another transaction's element
        if (!softLockId.wasPinned()) {
            underlyingStore.setPinned(softLock.getKey(), false);
        }

        Element element = softLockId.getOldElement();
        if (element != null) {
            underlyingStore.replace(e, element, comparator);
        } else {
            underlyingStore.removeElement(e, comparator);
        }
    }

}
