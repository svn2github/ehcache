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

package net.sf.ehcache.hibernate.nonstop;

import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;

/**
 * Implementation of {@link EntityRegionAccessStrategy} that handles {@link NonStopCacheException} using
 * {@link HibernateNonstopCacheExceptionHandler}
 *
 * @author Abhishek Sanoujam
 *
 */
public class NonstopAwareEntityRegionAccessStrategy implements EntityRegionAccessStrategy {

    private final EntityRegionAccessStrategy actualStrategy;
    private final HibernateNonstopCacheExceptionHandler hibernateNonstopExceptionHandler;

    /**
     * Constructor accepting the actual {@link EntityRegionAccessStrategy} and the {@link HibernateNonstopCacheExceptionHandler}
     *
     * @param actualStrategy
     * @param hibernateNonstopExceptionHandler
     */
    public NonstopAwareEntityRegionAccessStrategy(EntityRegionAccessStrategy actualStrategy,
            HibernateNonstopCacheExceptionHandler hibernateNonstopExceptionHandler) {
        this.actualStrategy = actualStrategy;
        this.hibernateNonstopExceptionHandler = hibernateNonstopExceptionHandler;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#getRegion()
     */
    public EntityRegion getRegion() {
        return actualStrategy.getRegion();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#afterInsert(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
        try {
            return actualStrategy.afterInsert(key, value, version);
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#afterUpdate(java.lang.Object, java.lang.Object, java.lang.Object,
     *      java.lang.Object, org.hibernate.cache.access.SoftLock)
     */
    public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
            throws CacheException {
        try {
            return actualStrategy.afterUpdate(key, value, currentVersion, previousVersion, lock);
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#evict(java.lang.Object)
     */
    public void evict(Object key) throws CacheException {
        try {
            actualStrategy.evict(key);
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#evictAll()
     */
    public void evictAll() throws CacheException {
        try {
            actualStrategy.evictAll();
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#get(java.lang.Object, long)
     */
    public Object get(Object key, long txTimestamp) throws CacheException {
        try {
            return actualStrategy.get(key, txTimestamp);
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#insert(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    public boolean insert(Object key, Object value, Object version) throws CacheException {
        try {
            return actualStrategy.insert(key, value, version);
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#lockItem(java.lang.Object, java.lang.Object)
     */
    public SoftLock lockItem(Object key, Object version) throws CacheException {
        try {
            return actualStrategy.lockItem(key, version);
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#lockRegion()
     */
    public SoftLock lockRegion() throws CacheException {
        try {
            return actualStrategy.lockRegion();
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object,
     *      boolean)
     */
    public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
            throws CacheException {
        try {
            return actualStrategy.putFromLoad(key, value, txTimestamp, version, minimalPutOverride);
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object)
     */
    public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
        try {
            return actualStrategy.putFromLoad(key, value, txTimestamp, version);
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#remove(java.lang.Object)
     */
    public void remove(Object key) throws CacheException {
        try {
            actualStrategy.remove(key);
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#removeAll()
     */
    public void removeAll() throws CacheException {
        try {
            actualStrategy.removeAll();
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#unlockItem(java.lang.Object, org.hibernate.cache.access.SoftLock)
     */
    public void unlockItem(Object key, SoftLock lock) throws CacheException {
        try {
            actualStrategy.unlockItem(key, lock);
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#unlockRegion(org.hibernate.cache.access.SoftLock)
     */
    public void unlockRegion(SoftLock lock) throws CacheException {
        try {
            actualStrategy.unlockRegion(lock);
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.hibernate.cache.access.EntityRegionAccessStrategy#update(java.lang.Object, java.lang.Object, java.lang.Object,
     *      java.lang.Object)
     */
    public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException {
        try {
            return actualStrategy.update(key, value, currentVersion, previousVersion);
        } catch (NonStopCacheException nonStopCacheException) {
            hibernateNonstopExceptionHandler.handleNonstopCacheException(nonStopCacheException);
            return false;
        }
    }

}
