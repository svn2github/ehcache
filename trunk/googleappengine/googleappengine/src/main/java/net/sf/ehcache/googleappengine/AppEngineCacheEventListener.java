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



package net.sf.ehcache.googleappengine;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

/**
 * Listens to {@link net.sf.ehcache.CacheManager} and {@link net.sf.ehcache.Cache} events and propagates those to
 * Google AppEngine's Memcache
 *
 * @author C&eacute;drik LIME
 * @see "http://ehcache.org/documentation/googleappengine.html"
 * @see "http://ehcache.org/documentation/cache_event_listeners.html"
 */
public class AppEngineCacheEventListener implements CacheEventListener, Cloneable {

    private static final Logger LOG = LoggerFactory.getLogger(AppEngineCacheEventListener.class);

    private Properties properties;

    /**
     * @param properties
     */
    public AppEngineCacheEventListener(Properties properties) {
        super();
        this.properties = properties;
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        properties = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
        if (checkElementKeySerializable(element) && checkElementSerializable(element)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("put in MemCache element with key: " + element.getKey());
            }
            MemcacheService memCache = MemcacheServiceFactory.getMemcacheService(cache.getName());
            memCache.put(element.getKey(), element.getValue(), Expiration.byDeltaSeconds(element.getTimeToLive()));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
        if (checkElementKeySerializable(element) && checkElementSerializable(element)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("update in MemCache element with key: " + element.getKey());
            }
            MemcacheService memCache = MemcacheServiceFactory.getMemcacheService(cache.getName());
            memCache.put(element.getKey(), element.getValue(), Expiration.byDeltaSeconds(element.getTimeToLive()));
        }
    }

    /**
     * {@inheritDoc}
     * Called when a put exceeds the MemoryStore's capacity.
     * This replicator does not propagate this event, as the element
     * is already in MemCache via a previous put().
     */
    public void notifyElementEvicted(Ehcache cache, Element element) {
        //noop
    }

    /**
     * {@inheritDoc}
     * This implementation does not propagate expiries.
     * The memcache and Ehcache expiries should be synced, so that if an element
     * expires in the L1 then it should also be expired in the L2.
     */
    public void notifyElementExpired(Ehcache cache, Element element) {
        //noop
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
        if (checkElementKeySerializable(element)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("delete in MemCache element with key: " + element.getKey());
            }
            MemcacheService memCache = MemcacheServiceFactory.getMemcacheService(cache.getName());
            memCache.delete(element.getKey());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyRemoveAll(Ehcache cache) {
        // it is too late to do anything, as the cache is already empty
        if (LOG.isDebugEnabled()) {
            LOG.debug("Can not propagate removeAll() for cache: " + cache.getName());
        }
    }

    /**
     * Checks the key is Serializable
     *
     * @param element the Element the key is in
     * @return true if Serializable
     */
    protected boolean checkElementKeySerializable(Element element) {
        if (!element.isKeySerializable()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Key " + element.getObjectKey() + " is not Serializable and cannot be replicated in MemCache.");
            }
            return false;
        }
        return true;
    }

    /**
     * Checks the element is Serializable
     *
     * @param element the Element
     * @return true if Serializable
     */
    protected boolean checkElementSerializable(Element element) {
        if (!element.isSerializable()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Object with key " + element.getObjectKey() + " is not Serializable and cannot be replicated in MemCache.");
            }
            return false;
        }
        return true;
    }
}
