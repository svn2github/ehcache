/**
 *  Copyright 2003-2009 Terracotta, Inc.
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
package net.sf.ehcache.hibernate.regions;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.TransactionalDataRegion;
import org.hibernate.cfg.Settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An EhCache specific TransactionalDataRegion.
 * <p>
 * This is the common superclass entity and collection regions.
 * 
 * @author Chris Dennis
 */
public class EhCacheTransactionalDataRegion extends EhCacheDataRegion implements TransactionalDataRegion {

    private static final Logger LOG = LoggerFactory.getLogger(EhCacheTransactionalDataRegion.class);
    
    /**
     * Hibernate settings associated with the persistence unit.
     */
    protected final Settings settings;

    /**
     * Metadata associated with the objects stored in the region.
     */
    protected final CacheDataDescription metadata;

    private final CacheLockProvider lockProvider;
    
    /**
     * Construct an transactional Hibernate cache region around the given EhCache instance.
     */
    EhCacheTransactionalDataRegion(Ehcache cache, Settings settings, CacheDataDescription metadata) {
        super(cache);
        this.settings = settings;
        this.metadata = metadata;

        Object context = cache.getInternalContext();
        if (context instanceof CacheLockProvider) {
            this.lockProvider = (CacheLockProvider) context;
        } else {
            this.lockProvider = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTransactionAware() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public CacheDataDescription getCacheDataDescription() {
        return metadata;
    }

    /**
     * Get the value mapped to this key, or null if no value is mapped to this key.
     */
    public final Object get(Object key) {
        LOG.debug("key: {}", key);
        if (key == null) {
            return null;
        } else {
            try {
                Element element = cache.get(key);
                if (element == null) {
                    LOG.debug("Element for key {} is null", key);
                    return null;
                } else {
                    return element.getObjectValue();
                }
            } catch (net.sf.ehcache.CacheException e) {
                throw new CacheException(e);
            }
        }
    }

    /**
     * Map the given value to the given key, replacing any existing mapping for this key
     */
    public final void put(Object key, Object value) throws CacheException {
        LOG.debug("key: {} value: {}", key, value);
        try {
            Element element = new Element(key, value);
            cache.put(element);
        } catch (IllegalArgumentException e) {
            throw new CacheException(e);
        } catch (IllegalStateException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Remove the mapping for this key (if any exists).
     */
    public final void remove(Object key) throws CacheException {
        try {
            cache.remove(key);
        } catch (ClassCastException e) {
            throw new CacheException(e);
        } catch (IllegalStateException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Remove all mapping from this cache region.
     */
    public final void clear() throws CacheException {
        try {
            cache.removeAll();
        } catch (IllegalStateException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Returns <code>true</code> if the underlying Ehcache instance supports finegrained locking of individual entries.
     */
    public final boolean supportsFinegrainedLocking() {
        return lockProvider != null;
    }

    /**
     * Attempts to lock the mapping for the given key.
     * <p>
     * This is a no-op if the underlying cache does not support finegrained locking.
     */
    public final void lock(Object key) {
        if (lockProvider != null) {
            lockProvider.getSyncForKey(key).lock(LockType.WRITE);
        }
    }

    /**
     * Attempts to unlock the mapping for the given key.
     * <p>
     * This is a no-op if the underlying cache does not support finegrained locking.
     */
    public final void unlock(Object key) {
        if (lockProvider != null) {
            lockProvider.getSyncForKey(key).unlock(LockType.WRITE);
        }
    }
}