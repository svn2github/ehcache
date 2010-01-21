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

import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.StripedReadWriteLockSync;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.TransactionalDataRegion;
import org.hibernate.cfg.Settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Ehcache specific TransactionalDataRegion.
 * <p>
 * This is the common superclass entity and collection regions.
 * 
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 */
public class EhcacheTransactionalDataRegion extends EhcacheDataRegion implements TransactionalDataRegion {

    private static final Logger LOG = LoggerFactory.getLogger(EhcacheTransactionalDataRegion.class);

    private static final int LOCAL_LOCK_PROVIDER_CONCURRENCY = 128;
    
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
     * Construct an transactional Hibernate cache region around the given Ehcache instance.
     */
    EhcacheTransactionalDataRegion(Ehcache cache, Settings settings, CacheDataDescription metadata, Properties properties) {
        super(cache, properties);
        this.settings = settings;
        this.metadata = metadata;

        Object context = cache.getInternalContext();
        if (context instanceof CacheLockProvider) {
            this.lockProvider = (CacheLockProvider) context;
        } else {
            this.lockProvider = new StripedReadWriteLockSync(LOCAL_LOCK_PROVIDER_CONCURRENCY);
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
     * Attempts to write lock the mapping for the given key.
     */
    public final void writeLock(Object key) {
        lockProvider.getSyncForKey(key).lock(LockType.WRITE);
    }

    /**
     * Attempts to write unlock the mapping for the given key.
     */
    public final void writeUnlock(Object key) {
        lockProvider.getSyncForKey(key).unlock(LockType.WRITE);
    }

    /**
     * Attempts to read lock the mapping for the given key.
     */
    public final void readLock(Object key) {
        lockProvider.getSyncForKey(key).lock(LockType.WRITE);
    }

    /**
     * Attempts to read unlock the mapping for the given key.
     */
    public final void readUnlock(Object key) {
        lockProvider.getSyncForKey(key).unlock(LockType.WRITE);
    }

    /**
     * Returns <code>true</code> if the locks used by the locking methods of this region are the independent of the cache.
     * <p>
     * Independent locks are not locked by the cache when the cache is accessed directly.  This means that for an independent lock
     * lock holds taken through a region method will not block direct access to the cache via other means.
     */
    public final boolean locksAreIndependentOfCache() {
        return lockProvider instanceof StripedReadWriteLockSync;
    }
}