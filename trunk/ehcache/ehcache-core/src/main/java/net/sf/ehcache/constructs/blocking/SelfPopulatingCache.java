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

package net.sf.ehcache.constructs.blocking;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;


/**
 * A selfpopulating decorator for {@link Ehcache} that creates entries on demand.
 * <p/>
 * Clients of the cache simply call it without needing knowledge of whether
 * the entry exists in the cache.
 * <p/>
 * The cache is designed to be refreshed. Refreshes operate on the backing cache, and do not
 * degrade performance of {@link #get(java.io.Serializable)} calls.
 * <p/>
 * Thread safety depends on the factory being used. The UpdatingCacheEntryFactory should be made
 * thread safe. In addition users of returned values should not modify their contents.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class SelfPopulatingCache extends BlockingCache {

    private static final Logger LOG = LoggerFactory.getLogger(SelfPopulatingCache.class.getName());

    /**
     * A factory for creating entries, given a key
     */
    protected final CacheEntryFactory factory;

    /**
     * Creates a SelfPopulatingCache.
     */
    public SelfPopulatingCache(final Ehcache cache, final CacheEntryFactory factory) throws CacheException {
        super(cache);
        this.factory = factory;
    }

    /**
     * Create a SelfPopulatingCache, with a specific number of stripes passed to the
     * underlying {@link net.sf.ehcache.constructs.blocking.BlockingCache}.
     */
    public SelfPopulatingCache(Ehcache cache, int numberOfStripes, CacheEntryFactory factory) throws CacheException {
       super(cache, numberOfStripes);
       this.factory = factory;
    }

  /**
     * Looks up an entry.  creating it if not found.
     */
    @Override
    public Element get(final Object key) throws LockTimeoutException {

        Element element = super.get(key);

        if (element == null) {
            try {
                // Value not cached - fetch it
                Object value = factory.createEntry(key);
                element = makeAndCheckElement(key, value);
            } catch (final Throwable throwable) {
                // Could not fetch - Ditch the entry from the cache and rethrow
                // release the lock you acquired
                element = new Element(key, null);
                throw new CacheException("Could not fetch object for cache entry with key \"" + key + "\".", throwable);
            } finally {
                put(element);
            }
        }
        return element;
    }

    /**
     * Refresh the elements of this cache.
     * <p/>
     * Refreshes bypass the {@link BlockingCache} and act directly on the backing {@link Ehcache}.
     * This way, {@link BlockingCache} gets can continue to return stale data while the refresh, which
     * might be expensive, takes place.
     * <p/>
     * Quiet methods are used, so that statistics are not affected.
     * Note that the refreshed elements will not be replicated to any cache peers.
     * <p/>
     * Configure ehcache.xml to stop elements from being refreshed forever:
     * <ul>
     * <li>use timeToIdle to discard elements unused for a period of time
     * <li>use timeToLive to discard elmeents that have existed beyond their allotted lifespan
     * </ul>
     *
     * @throws CacheException
     */
    public void refresh() throws CacheException {
        refresh(true);
    }

    /**
     * Refresh the elements of this cache.
     * <p/>
     * Refreshes bypass the {@link BlockingCache} and act directly on the backing {@link Ehcache}.
     * This way, {@link BlockingCache} gets can continue to return stale data while the refresh, which
     * might be expensive, takes place.
     * <p/>
     * Quiet methods are used if argument 0 is true, so that statistics are not affected,
     * but note that replication will then not occur
     * <p/>
     * Configure ehcache.xml to stop elements from being refreshed forever:
     * <ul>
     * <li>use timeToIdle to discard elements unused for a period of time
     * <li>use timeToLive to discard elmeents that have existed beyond their allotted lifespan
     * </ul>
     *
     * @param quiet whether the backing cache is quietly updated or not, if true replication will not occur
     * @throws CacheException
     * @since 1.6.1
     */
    public void refresh(boolean quiet) throws CacheException {
        Exception exception = null;
        Object keyWithException = null;

        // Refetch the entries
        final Collection keys = getKeys();

        LOG.debug(getName() + ": found " + keys.size() + " keys to refresh");

        // perform the refresh
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            final Object key = iterator.next();

            try {
                Ehcache backingCache = getCache();
                final Element element = backingCache.getQuiet(key);

                if (element == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(getName() + ": entry with key " + key + " has been removed - skipping it");
                    }
                    continue;
                }

                refreshElement(element, backingCache, quiet);
            } catch (final Exception e) {
                // Collect the exception and keep going.
                // Throw the exception once all the entries have been refreshed
                // If the refresh fails, keep the old element. It will simply become staler.
                LOG.warn(getName() + "Could not refresh element " + key, e);
                keyWithException = key;
                exception = e;
            }
        }

        if (exception != null) {
            throw new CacheException(exception.getMessage() + " on refresh with key " + keyWithException, exception);
        }
    }

    /**
     * Refresh a single element.
     * <p/>
     * Refreshes bypass the {@link BlockingCache} and act directly on the backing {@link Ehcache}.
     * This way, {@link BlockingCache} gets can continue to return stale data while the refresh, which
     * might be expensive, takes place.
     * <p/>
     * If the element is absent it is created
     * <p/>
     * Quiet methods are used, so that statistics are not affected.
     * Note that the refreshed element will not be replicated to any cache peers.
     *
     * @param key
     * @return the refreshed Element
     * @throws CacheException
     * @since 1.6.1
     */
    public Element refresh(Object key) throws CacheException {
        return refresh(key, true);
    }

    /**
     * Refresh a single element.
     * <p/>
     * Refreshes bypass the {@link BlockingCache} and act directly on the backing {@link Ehcache}.
     * This way, {@link BlockingCache} gets can continue to return stale data while the refresh, which
     * might be expensive, takes place.
     * <p/>
     * If the element is absent it is created
     * <p/>
     * Quiet methods are used if argument 1 is true, so that statistics are not affected,
     * but note that replication will then not occur
     *
     * @param key
     * @param quiet whether the backing cache is quietly updated or not,
     *              if true replication will not occur
     * @return the refreshed Element
     * @throws CacheException
     * @since 1.6.1
     */
    public Element refresh(Object key, boolean quiet) throws CacheException {
        try {
            Ehcache backingCache = getCache();
            Element element = backingCache.getQuiet(key);
            if (element != null) {
                return refreshElement(element, backingCache, quiet);
            } else {
                //need to create
                return get(key);
            }
        } catch (CacheException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CacheException(e.getMessage() + " on refresh with key " + key, e);
        }
    }

    /**
     * Refresh a single element.
     *
     * @param element      the Element to refresh
     * @param backingCache the underlying {@link Ehcache}.
     * @throws Exception
     */
    protected void refreshElement(final Element element, Ehcache backingCache) throws Exception {
        refreshElement(element, backingCache, true);
    }

    /**
     * Refresh a single element.
     *
     * @param element      the Element to refresh
     * @param backingCache the underlying {@link Ehcache}.
     * @param quiet        whether to use putQuiet or not, if true replication will not occur
     * @return the refreshed Element
     * @throws Exception
     * @since 1.6.1
     */
    protected Element refreshElement(final Element element, Ehcache backingCache, boolean quiet) throws Exception {
        Object key = element.getObjectKey();

        if (LOG.isDebugEnabled()) {
            LOG.debug(getName() + ": refreshing element with key " + key);
        }

        final Element replacementElement;

        if (factory instanceof UpdatingCacheEntryFactory) {
            //update the value of the cloned Element in place
            replacementElement = element;
            ((UpdatingCacheEntryFactory) factory).updateEntryValue(key, replacementElement.getObjectValue());

            //put the updated element back into the backingCache, without updating stats
            //It is not usually necessary to do this. We do this in case the element expired
            //or idles out of the backingCache. In that case we hold a reference to it but the
            // backingCache no longer does.
        } else {
            final Object value = factory.createEntry(key);
            replacementElement = makeAndCheckElement(key, value);
        }

        if (quiet) {
            backingCache.putQuiet(replacementElement);
        } else {
            backingCache.put(replacementElement);
        }
        return replacementElement;
    }

    /**
     * Both CacheEntryFactory can return an Element rather than just a regular value
     * this method test this, making a fresh Element otherwise.  It also enforces
     * the rule that the CacheEntryFactory must provide the same key (via equals()
     * not necessarily same object) if it is returning an Element.
     *
     * @param key
     * @param value
     * @return the Element to be put back in the cache
     * @throws CacheException for various illegal states which could be harmful
     */
    protected static Element makeAndCheckElement(Object key, Object value) throws CacheException {
        //simply build a new element using the supplied key
        if (!(value instanceof Element)) {
            return new Element(key, value);
        }

        //It is already an element - perform sanity checks
        Element element = (Element) value;
        if ((element.getObjectKey() == null) && (key == null)) {
            return element;
        } else if (element.getObjectKey() == null) {
            throw new CacheException("CacheEntryFactory returned an Element with a null key");
        } else if (!element.getObjectKey().equals(key)) {
            throw new CacheException("CacheEntryFactory returned an Element with a different key: " +
                    element.getObjectKey() + " compared to the key that was requested: " + key);
        } else {
            return element;
        }
    }

}
