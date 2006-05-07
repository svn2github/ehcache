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
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;


/**
 * A {@link net.sf.ehcache.Cache} backed cache that creates entries on demand.
 * <p/>
 * Clients of the cache simply call it without needing knowledge of whether
 * the entry exists in the cache.
 * <p/>
 * The cache is designed to be refreshed. Refreshes operate on the backing cache, and do not
 * degrade performance of {@link #get(java.io.Serializable)} calls.
 *
 * Thread safety depends on the factory being used. The UpdatingCacheEntryFactory should be made
 * thread safe. In addition users of returned values should not modify their contents.
 *
 * For values which are {@link Collection}s, extra safety has been built in.
 * Use the {@link SelfPopulatingCollectionCache}
 *
 * @author Greg Luck
 * @version $Id$
 * @see SelfPopulatingCollectionCache
 */
public class SelfPopulatingCache extends BlockingCache {
    private static final Log LOG = LogFactory.getLog(SelfPopulatingCache.class.getName());

    /**
     * A factory for creating entries, given a key
     */
    protected final CacheEntryFactory factory;

    /**
     * Creates a SelfPopulatingCache.
     */
    public SelfPopulatingCache(final String name, final CacheEntryFactory factory)
        throws CacheException {
        super(name);
        this.factory = factory;
    }

    /**
     * Creates a SelfPopulatingCache and use the given cache manager to create cache objects
     */
    public SelfPopulatingCache(final String name, final CacheManager mgr, final CacheEntryFactory factory)
        throws CacheException {
        super(name, mgr);
        this.factory = factory;
    }

    /**
     * Looks up an object, creating it if not found.
     */
    public Serializable get(final Serializable key) throws BlockingCacheException {
        String oldThreadName = Thread.currentThread().getName();
        setThreadName("get", key);

        try {
            //if null will lock here
            Serializable value = super.get(key);

            if (value == null) {
                // Value not cached - fetch it
                value = factory.createEntry(key);
                setThreadName("put", key);
                put(key, value);
                Thread.currentThread().setName(oldThreadName);
            }

            return value;
        } catch (final Throwable throwable) {
            // Could not fetch - Ditch the entry from the cache and rethrow
            setThreadName("put", key);
            put(key, null);

            try {
                throw new BlockingCacheException("Could not fetch object for cache entry \"" + key + "\".", throwable);
            } catch (NoSuchMethodError e) {
                //Running 1.3 or lower
                throw new CacheException("Could not fetch object for cache entry \"" + key + "\".");
            }
        } finally {
            Thread.currentThread().setName(oldThreadName);
        }
    }

    /**
     * Rename the thread for easier thread dump reading.
     *
     * @param method the method about to be called
     * @param key    the key being operated on
     */
    protected void setThreadName(String method, final Serializable key) {
        StringBuffer threadName = new StringBuffer(getName()).append(": ").append(method).append("(").append(key).append(")");
        Thread.currentThread().setName(threadName.toString());
    }

    /**
     * Refresh the elements of this cache.
     * <p/>
     * Refreshes bypass the {@link BlockingCache} and act directly on the backing {@link Ehcache}.
     * This way, {@link BlockingCache} gets can continue to return stale data while the refresh, which
     * might be expensive, takes place.
     * <p/>
     * Quiet methods are used, so that statistics are not affected.
     * <p/>
     * Threads entering this method are temporarily renamed, so that a Thread Dump will show
     * meaningful information.
     * <p/>
     * Configure ehcache.xml to stop elements from being refreshed forever:
     * <ul>
     * <li>use timeToIdle to discard elements unused for a period of time
     * <li>use timeToLive to discard elmeents that have existed beyond their allotted lifespan
     * </ul>
     */
    public void refresh() throws CacheException {
        final String oldThreadName = Thread.currentThread().getName();
        Exception exception = null;

        // Refetch the entries
        final Collection keys = getKeys();

        if (LOG.isTraceEnabled()) {
            LOG.trace(getName() + ": found " + keys.size() + " keys to refresh");
        }

        // perform the refresh
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            final Serializable key = (Serializable) iterator.next();

            try {
                Ehcache backingCache = getCache();
                final Element element = backingCache.getQuiet(key);

                if (element == null) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace(getName() + ": entry with key " + key + " has been removed - skipping it");
                    }

                    continue;
                }

                refreshElement(element, backingCache);
            } catch (final Exception e) {
                // Collect the exception and keep going.
                // Throw the exception once all the entries have been refreshed
                // If the refresh fails, keep the old element. It will simply become staler.
                LOG.warn(getName() + "Could not refresh element " + key, e);
                exception = e;
            } finally {
                Thread.currentThread().setName(oldThreadName);
            }
        }

        if (exception != null) {
            throw new CacheException(exception.getMessage());
        }
    }

    /**
     * Refresh a single element.
     *
     * @param element      the Element to refresh
     * @param backingCache the underlying {@link Ehcache}.
     * @throws Exception
     */
    protected void refreshElement(final Element element, Ehcache backingCache)
        throws Exception {
        Serializable key = element.getKey();

        if (LOG.isTraceEnabled()) {
            setThreadName("refreshElement", key);
            LOG.trace(getName() + ": refreshing element with key " + key);
        }

        final Element replacementElement;

        if (factory instanceof UpdatingCacheEntryFactory) {
            //update the value of the cloned Element in place
            replacementElement = element;
            ((UpdatingCacheEntryFactory) factory).updateEntryValue(key, replacementElement.getValue());

            //put the updated element back into the backingCache, without updating stats
            //It is not usually necessary to do this. We do this in case the element expired
            //or idles out of the backingCache. In that case we hold a reference to it but the
            // backingCache no longer does.
        } else {
            final Serializable value = factory.createEntry(key);
            replacementElement = new Element(key, value);
        }

        backingCache.putQuiet(replacementElement);
    }
}
