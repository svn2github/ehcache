/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;


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
    private static final Log LOG = LogFactory.getLog(LruMemoryStore.class.getName());

    /**
     * Constructor for the LruMemoryStore object
     * The backing {@link java.util.LinkedHashMap} is created with LRU by access order.
     */
    public LruMemoryStore(Ehcache cache, Store diskStore) {
        super(cache, diskStore);

        try {
            map = loadMapInstance();
        } catch (CacheException e) {
            LOG.error(cache.getName() + "Cache: Cannot start LruMemoryStore. Initial cause was " + e.getMessage(), e);
        }
    }

    /**
     * Tries to load a {@link java.util.LinkedHashMap} (JDK1.4) and then
     * tries to load an {@link org.apache.commons.collections.LRUMap}.
     * <p/>
     * This way applications running JDK1.4 do not have a dependency
     * on Apache commons-collections.
     *
     * @return a Map, being either {@link java.util.LinkedHashMap} or
     */
    private Map loadMapInstance() throws CacheException {
        //First try to load java.util.LinkedHashMap, which is preferred, but only if not overriden
        if (System.getProperty("net.sf.ehcache.useLRUMap") == null) {

            try {
                Class.forName("java.util.LinkedHashMap");
                Map candidateMap = new SpoolingLinkedHashMap();
                if (LOG.isDebugEnabled()) {
                    LOG.debug(cache.getName() + " Cache: Using SpoolingLinkedHashMap implementation");
                }
                return candidateMap;
            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(cache.getName() + " Cache: Cannot find java.util.LinkedHashMap");
                }
            }
        }

        //Secondly, try and load org.apache.commons.collections.LRUMap
        try {
            Class.forName("org.apache.commons.collections.LRUMap");
            Map candidateMap = new SpoolingLRUMap();
            if (LOG.isDebugEnabled()) {
                LOG.debug(cache.getName() + " Cache: Using SpoolingLRUMap implementation");
            }
            return candidateMap;
        } catch (Exception e) {
            //Give up
            throw new CacheException(cache.getName()
                    + "Cache: Cannot find org.apache.commons.collections.LRUMap.");
        }
    }


    /**
     * An LRU Map implementation based on Apache Commons LRUMap.
     * <p/>
     * This is used if {@link java.util.LinkedHashMap} is not found in the classpath.
     * LinkedHashMap is part of JDK
     */
    public final class SpoolingLRUMap extends org.apache.commons.collections.LRUMap {

        /**
         * Constructor.
         * The maximum size is set to {@link Ehcache#getMaxElementsInMemory}. If the
         * LRUMap gets bigger than this, {@link #processRemovedLRU} is called.
         */
        public SpoolingLRUMap() {
            setMaximumSize(cache.getMaxElementsInMemory());
        }

        /**
         * Called after the element has been removed.
         * <p/>
         * Our choices are to do nothing or spool the element to disk.
         * <p/>
         * Note that value will be null when the memory size is set to 0. Thus a null guard is used.
         *
         * @param key
         * @param value
         */
        protected final void processRemovedLRU(Object key, Object value) {
            //Already removed from the map at this point
            Element element = (Element) value;

            //When max size is 0
            if (element == null) {
                return;
            }

            //check for expiry before going to the trouble of spooling
            if (element.isExpired()) {
                notifyExpiry(element);
            } else {
                evict(element);
            }
        }
    }

    /**
     * An extension of LinkedHashMap which overrides {@link #removeEldestEntry}
     * to persist cache entries to the auxiliary cache before they are removed.
     * <p/>
     * This implementation also provides LRU by access order.
     */
    public final class SpoolingLinkedHashMap extends java.util.LinkedHashMap {
        private static final int INITIAL_CAPACITY = 100;
        private static final float GROWTH_FACTOR = .75F;

        /**
         * Default constructor.
         * Will create an initial capacity of 100, a loading of .75 and
         * LRU by access order.
         */
        public SpoolingLinkedHashMap() {
            super(INITIAL_CAPACITY, GROWTH_FACTOR, true);
        }

        /**
         * Returns <tt>true</tt> if this map should remove its eldest entry.
         * This method is invoked by <tt>put</tt> and <tt>putAll</tt> after
         * inserting a new entry into the map.  It provides the implementer
         * with the opportunity to remove the eldest entry each time a new one
         * is added.  This is useful if the map represents a cache: it allows
         * the map to reduce memory consumption by deleting stale entries.
         * <p/>
         * Will return true if:
         * <ol>
         * <li> the element has expired
         * <li> the cache size is greater than the in-memory actual.
         * In this case we spool to disk before returning.
         * </ol>
         *
         * @param eldest The least recently inserted entry in the map, or if
         *               this is an access-ordered map, the least recently accessed
         *               entry.  This is the entry that will be removed it this
         *               method returns <tt>true</tt>.  If the map was empty prior
         *               to the <tt>put</tt> or <tt>putAll</tt> invocation resulting
         *               in this invocation, this will be the entry that was just
         *               inserted; in other words, if the map contains a single
         *               entry, the eldest entry is also the newest.
         * @return true if the eldest entry should be removed
         *         from the map; <tt>false</t> if it should be retained.
         */
        protected final boolean removeEldestEntry(Map.Entry eldest) {
            Element element = (Element) eldest.getValue();
            return removeLeastRecentlyUsedElement(element);
        }

        /**
         * Relies on being called from a synchronized method
         *
         * @param element
         * @return true if the LRU element should be removed
         */
        private boolean removeLeastRecentlyUsedElement(Element element) throws CacheException {
            //check for expiry and remove before going to the trouble of spooling it
            if (element.isExpired()) {
                notifyExpiry(element);
                return true;
            }

            if (isFull()) {
                evict(element);
                return true;
            } else {
                return false;
            }

        }
    }
}
