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
package net.sf.ehcache.hibernate.regions;

import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.StripedReadWriteLockSync;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.hibernate.nonstop.HibernateNonstopCacheExceptionHandler;
import net.sf.ehcache.hibernate.strategy.EhcacheAccessStrategyFactory;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.TransactionalDataRegion;
import org.hibernate.cfg.Settings;

/**
 * An Ehcache specific TransactionalDataRegion.
 * <p>
 * This is the common superclass entity and collection regions.
 *
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @author Abhishek Sanoujam
 */
public class EhcacheTransactionalDataRegion extends EhcacheDataRegion implements TransactionalDataRegion {

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
    EhcacheTransactionalDataRegion(EhcacheAccessStrategyFactory accessStrategyFactory, Ehcache cache, Settings settings,
            CacheDataDescription metadata, Properties properties) {
        super(accessStrategyFactory, cache, properties);
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
     * Return the hibernate settings
     *
     * @return settings
     */
    public Settings getSettings() {
        return settings;
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
        try {
            Element element = cache.get(key);
            if (element == null) {
                return null;
            } else {
                return element.getObjectValue();
            }
        } catch (net.sf.ehcache.CacheException e) {
            if (e instanceof NonStopCacheException) {
                HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException((NonStopCacheException) e);
                return null;
            } else {
                throw new CacheException(e);
            }
        }
    }

    /**
     * Map the given value to the given key, replacing any existing mapping for this key
     * this unpins the key in the cache should it be currently pinned
     */
    public final void put(Object key, Object value) throws CacheException {
        put(key, value, false);
    }

    /**
     * Map the given value to the given key, replacing any existing mapping for this key,
     * pinning the key in the cache
     */
    public final void putEternal(Object key, Object value) throws CacheException {
        put(key, value, true);
    }

    private void put(Object key, Object value, boolean eternal) throws CacheException {
        try {
            Element element = new Element(key, value);
            element.setEternal(eternal);
            cache.put(element);
        } catch (IllegalArgumentException e) {
            throw new CacheException(e);
        } catch (IllegalStateException e) {
            throw new CacheException(e);
        } catch (net.sf.ehcache.CacheException e) {
            if (e instanceof NonStopCacheException) {
                HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException((NonStopCacheException) e);
            } else {
                throw new CacheException(e);
            }
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
        } catch (net.sf.ehcache.CacheException e) {
            if (e instanceof NonStopCacheException) {
                HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException((NonStopCacheException) e);
            } else {
                throw new CacheException(e);
            }
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
        } catch (net.sf.ehcache.CacheException e) {
            if (e instanceof NonStopCacheException) {
                HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException((NonStopCacheException) e);
            } else {
                throw new CacheException(e);
            }
        }
    }

    /**
     * Attempts to write lock the mapping for the given key.
     */
    public final void writeLock(Object key) {
        try {
            lockProvider.getSyncForKey(key).lock(LockType.WRITE);
        } catch (net.sf.ehcache.CacheException e) {
            if (e instanceof NonStopCacheException) {
                HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException((NonStopCacheException) e);
            } else {
                throw new CacheException(e);
            }
        }
    }

    /**
     * Attempts to write unlock the mapping for the given key.
     */
    public final void writeUnlock(Object key) {
        try {
            lockProvider.getSyncForKey(key).unlock(LockType.WRITE);
        } catch (net.sf.ehcache.CacheException e) {
            if (e instanceof NonStopCacheException) {
                HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException((NonStopCacheException) e);
            } else {
                throw new CacheException(e);
            }
        }
    }

    /**
     * Attempts to read lock the mapping for the given key.
     */
    public final void readLock(Object key) {
        try {
            lockProvider.getSyncForKey(key).lock(LockType.READ);
        } catch (net.sf.ehcache.CacheException e) {
            if (e instanceof NonStopCacheException) {
                HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException((NonStopCacheException) e);
            } else {
                throw new CacheException(e);
            }
        }
    }

    /**
     * Attempts to read unlock the mapping for the given key.
     */
    public final void readUnlock(Object key) {
        try {
            lockProvider.getSyncForKey(key).unlock(LockType.READ);
        } catch (net.sf.ehcache.CacheException e) {
            if (e instanceof NonStopCacheException) {
                HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException((NonStopCacheException) e);
            } else {
                throw new CacheException(e);
            }
        }
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
