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
package net.sf.ehcache.store;

import net.sf.ehcache.Element;

import java.util.Set;

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
     * Removes the element if the tier. This method will remove if the element is pinned and present in the store
     * @param key the key to the element
     * @return true if an element was removed
     * @see #remove(Object)
     */
    boolean removeIfTierNotPinned(Object key);

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
     * Returns all the keys that are pinned, for which there is a mapping present
     * @return the set of keys with values that are currently pinned
     */
    Set getPresentPinnedKeys();

    /**
     * Is this store persistent (data survives a JVM restart)
     * @return true if persistent
     */
    boolean isPersistent();
}
