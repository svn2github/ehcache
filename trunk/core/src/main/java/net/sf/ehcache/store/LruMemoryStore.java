/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.concurrent.ConcurrentLinkedHashMap;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An implementation of a LruMemoryStore.
 * <p/>
 * This uses {@link java.util.LinkedHashMap} as its backing map. It uses the {@link java.util.LinkedHashMap} LRU
 * feature. LRU for this implementation means least recently accessed.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class LruMemoryStore extends MemoryStore {

    private static final Logger LOG = Logger.getLogger(LruMemoryStore.class.getName());


    /**
     * Constructor for the LruMemoryStore object
     * The backing {@link java.util.LinkedHashMap} is created with LRU by access order.
     */
    public LruMemoryStore(Ehcache cache, Store diskStore) {
        super(cache, diskStore);

        try {
            map = new ConcurrentLinkedHashMap(ConcurrentLinkedHashMap.EvictionPolicy.SECOND_CHANCE,
                    cache.getCacheConfiguration().getMaxElementsInMemory(), new LruEvictionListener());
        } catch (CacheException e) {
            LOG.log(Level.SEVERE, cache.getName() + "Cache: Cannot start LruMemoryStore. Initial cause was " + e.getMessage(), e);
        }
    }

    /**
     * Allow specialised actions after adding the element to the map.
     *
     * @param element
     */
    protected void doPut(Element element) throws CacheException {
        //noop
    }

    /**
     * A class that is notified when the map evicts an element
     */
    public final class LruEvictionListener implements ConcurrentLinkedHashMap.EvictionListener {

        /**
         * A call-back notification that the entry was evicted.
         *
         * @param key   The evicted key.
         * @param value The evicted value.
         */
        public void onEviction(Object key, Object value) {
            Element element = (Element) value;

            //check for expiry and remove before going to the trouble of spooling it
            if (element.isExpired()) {
                notifyExpiry(element);
            } else {
                evict(element);
            }
        }
    }
}
