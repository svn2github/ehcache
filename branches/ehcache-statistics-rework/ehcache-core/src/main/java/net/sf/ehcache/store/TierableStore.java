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
package net.sf.ehcache.store;

import net.sf.ehcache.Element;

/**
 * This is the interface for all tierable stores.
 *
 * @author Ludovic Orban
 */
public interface TierableStore extends Store {

    /**
     * Add this element to the cache if the key is already present or the add
     * can succeed without resorting to eviction.
     *
     * @param e element to be added
     */
    void fill(Element e);

    /**
     * This method will only remove if the element or the store is not pinned and the key is present in the store
     * @param key the key to the element
     * @return true if an element was removed
     * @see #remove(Object)
     */
    boolean removeIfNotPinned(Object key);

    /**
     * Removes an item from the cache.
     */
    void removeNoReturn(Object key);

    /**
     * Is this TierableStore pinned ?
     * @return true if pinned
     */
    boolean isTierPinned();

    /**
     * Is this store persistent (data survives a JVM restart)
     * @return true if persistent
     */
    boolean isPersistent();
}
