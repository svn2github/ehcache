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

package net.sf.ehcache.constructs.blocking;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;


import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;


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

    private static final Logger LOG = Logger.getLogger(SelfPopulatingCache.class.getName());

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
     * Looks up an entry.  creating it if not found.
     */
    public Element get(final Object key) throws LockTimeoutException {

        try {
            //if null will lock here
            Element element = super.get(key);

            if (element == null) {
                // Value not cached - fetch it
                Object value = factory.createEntry(key);
                element = makeAndCheckElement(key, value);
                put(element);
            }
            return element;

        } catch (LockTimeoutException e) {
            //do not release the lock, because you never acquired it
            String message = "Timeout after " + timeoutMillis + " waiting on another thread " +
                    "to fetch object for cache entry \"" + key + "\".";
            throw new LockTimeoutException(message, e);

        } catch (final Throwable throwable) {
            // Could not fetch - Ditch the entry from the cache and rethrow

            //release the lock you acquired
            put(new Element(key, null));
            throw new CacheException("Could not fetch object for cache entry with key \"" + key + "\".", throwable);
        }
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
     * Configure ehcache.xml to stop elements from being refreshed forever:
     * <ul>
     * <li>use timeToIdle to discard elements unused for a period of time
     * <li>use timeToLive to discard elmeents that have existed beyond their allotted lifespan
     * </ul>
     */
    public void refresh() throws CacheException {
        Exception exception = null;
        Object keyWithException = null;

        // Refetch the entries
        final Collection keys = getKeys();

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(getName() + ": found " + keys.size() + " keys to refresh");
        }

        // perform the refresh
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            final Object key = iterator.next();

            try {
                Ehcache backingCache = getCache();
                final Element element = backingCache.getQuiet(key);

                if (element == null) {
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.finest(getName() + ": entry with key " + key + " has been removed - skipping it");
                    }

                    continue;
                }

                refreshElement(element, backingCache);
            } catch (final Exception e) {
                // Collect the exception and keep going.
                // Throw the exception once all the entries have been refreshed
                // If the refresh fails, keep the old element. It will simply become staler.
                LOG.log(Level.WARNING, getName() + "Could not refresh element " + key, e);
                exception = e;
            }
        }

        if (exception != null) {
            throw new CacheException(exception.getMessage() + " on refresh with key " + keyWithException);
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
        Object key = element.getObjectKey();

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(getName() + ": refreshing element with key " + key);
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
            final Object value = factory.createEntry(key);
            replacementElement = makeAndCheckElement(key, value);
        }

        backingCache.putQuiet(replacementElement);
    }
    
    /** Both CacheEntryFactory can return an Element rather than just a regular value
     * this method test this, making a fresh Element otherwise.  It also enforces
     * the rule that the CacheEntryFactory must provide the same key (via equals()
     * not necessarily same object) if it is returning an Element.  
     * @param key
     * @param value
     * @return the Element to be put back in the cache
     * @throws CacheException
     */
    private static Element makeAndCheckElement(Object key, Object value) throws CacheException{
        if (value instanceof Element) {
            Element element = (Element) value;
            if((element.getObjectKey()==null) && (key==null))
            	return element;
            if(element.getObjectKey()==null)
            	throw new CacheException("CacheEntryFactory returned an Element with a null key");
            if(element.getObjectKey().equals(key))
            	return element;
            throw new CacheException("CacheEntryFactory returned an Element with a different key: "
            		+ element.getObjectKey() + " compared to the key that was requested: "
            		+ key);
        } else {
            return new Element(key, value);
        }
    }
}
