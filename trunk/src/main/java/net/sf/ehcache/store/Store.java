/**
 *  Copyright 2003-2006 Greg Luck
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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;

import java.io.IOException;

/**
 * This is the interface for all stores. A store is a physical counterpart to a cache, which
 * is a logical concept.
 *
 * @author Greg Luck
 * @version $Id$
 */
public interface Store {

    /**
     * Puts an item into the cache.
     */
    void put(Element element) throws IOException, CacheException;

    /**
     * Gets an item from the cache.
     * @revised 1.2
     */
    Element get(Object key) throws IOException;

    /**
     * Removes an item from the cache.
     *
     * @since signature changed in 1.2 from boolean to Element to support notifications
     * @revised 1.2
     */
    Element remove(Object key) throws IOException;

    /**
     * Remove all of the elements from the store.
     * <p/>
     * If there are registered <code>CacheEventListener</code>s they are notified of the expiry or removal
     * of the <code>Element</code> as each is removed.
     *
     * @throws IOException
     */
    void removeAll() throws IOException, CacheException;

    /**
     * Prepares for shutdown.
     */
    void dispose() throws IOException;

    /**
     * Returns the current store size.
     */
    int getSize();

    /**
     * Returns the cache status.
     */
    Status getStatus();


    /**
     * A check to see if a key is in the Store.
     *
     * @param key The Element key
     * @return true if found. No check is made to see if the Element is expired.
     * @revised 1.2
     */
    boolean containsKey(Object key);

}
