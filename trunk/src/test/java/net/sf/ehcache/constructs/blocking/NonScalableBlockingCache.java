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

package net.sf.ehcache.constructs.blocking;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

/**
 * A cache backed by {@link net.sf.ehcache.Cache}
 * <p>
 * It allows concurrent read access to an object but requires exclusive access for write
 * operations. Reads will be suspended until the write is complete.
 *
 * Implements Single Threaded Execution [Grand98] pattern on all methods. This does not
 * scale well. Consequently, this cache is included in the test package for comparative purposes.
 *
 * @version $Id$
 * @author Greg Luck
 */
public class NonScalableBlockingCache extends BlockingCache {
    private static final Log LOG = LogFactory.getLog(BlockingCacheTest.class.getName());
    private final  net.sf.ehcache.Cache cache;

    /** A list of cache entries which are presently locked */
    private final Collection locks = new HashSet();

    /**
     *  Creates a BlockingCache with given name.
     */
    public NonScalableBlockingCache(final String name) throws Exception {
        super(name);
        CacheManager manager = CacheManager.create();
        LOG.debug("Creating BlockingCache for: " + name);
        cache = manager.getCache(name);
        if (cache == null || !cache.getName().equals(name)) {
            throw new Exception("Cache " + name + " cannot be retrieved. Please check ehcache.xml");
        }
    }

    /**
     *  Retrieve the EHCache backing cache
     */
    protected net.sf.ehcache.Cache getCache() {
        return cache;
    }

    /**
     * Returns this cache's name
     */
    public String getName() {
        return cache.getName();
    }

    /**
     * Looks up an entry.  Blocks if the entry is locked.
     * Returns null if the entry is not cached and locks the entry.
     * Note:  If this method returns null, {@link #put} must be called
     * at some point after this method, to mark the entry as fetched.
     *
     * Warning: If not a deadlock will occur.
     *
     */
    public synchronized Serializable get(final Serializable key) throws CacheException {
        LOG.debug("get called on Cache " + cache.getName() + " for key " + key);
        while (locks.contains(key)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cache " + cache.getName() + " blocking on " + key);
            }
            try {
                wait();
            } catch (InterruptedException e) {
                throw new CacheException("Exception");
            }
        }

        Element element = null;
        try {
            element = cache.get(key);
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException("Exception");
        }
        if (element != null) {
            return element.getValue();
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cache " + cache.getName() + " does not contain " + key);
                LOG.debug("Cache " + cache.getName() + " locks " + key);
            }
            locks.add(key);
            return null;
        }
    }

    /**
     * Adds an entry and unlocks it.
     */
    public synchronized void put(final Serializable key, final Serializable value) {
        if (value != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cache " + cache.getName() + " adding " + key);
            }
            final Element element = new Element(key, value);
            cache.put(element);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cache " + cache.getName() + " removing " + key);
            }
            cache.remove(key);
        }
        locks.remove(key);
        notify();
    }

    /**
     * Returns the keys of this cache.
     * @return The keys of this cache.  This is not a live set, so it will not track changes to the key set.
     */
    public synchronized Collection getKeys() throws CacheException {
        return cache.getKeys();
    }

    /**
     * Drops the contents of this cache.
     */
    public synchronized void clear() throws CacheException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Cache " + cache.getName() + " removing all entries");
        }
        try {
            cache.removeAll();
        } catch (IOException e) {
            throw new CacheException("Exception");
        }
    }

    /**
     * Synchronized version of getName to test liveness of the object lock.
     */
    public synchronized String liveness() {
        return getName();
    }
}
