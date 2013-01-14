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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.hibernate.nonstop.HibernateNonstopCacheExceptionHandler;
import net.sf.ehcache.hibernate.strategy.EhcacheAccessStrategyFactory;
import net.sf.ehcache.util.Timestamper;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Ehcache specific data region implementation.
 * <p>
 * This class is the ultimate superclass for all Ehcache Hibernate cache regions.
 *
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @author Abhishek Sanoujam
 */
public abstract class EhcacheDataRegion implements Region {

    private static final Logger LOG = LoggerFactory.getLogger(EhcacheDataRegion.class);

    private static final String CACHE_LOCK_TIMEOUT_PROPERTY = "net.sf.ehcache.hibernate.cache_lock_timeout";
    private static final int DEFAULT_CACHE_LOCK_TIMEOUT = 60000;

    /**
     * Ehcache instance backing this Hibernate data region.
     */
    protected final Ehcache cache;

    /**
     * The {@link EhcacheAccessStrategyFactory} used for creating various access strategies
     */
    protected final EhcacheAccessStrategyFactory accessStrategyFactory;

    private final int cacheLockTimeout;


    /**
     * Create a Hibernate data region backed by the given Ehcache instance.
     */
    EhcacheDataRegion(EhcacheAccessStrategyFactory accessStrategyFactory, Ehcache cache, Properties properties) {
        this.accessStrategyFactory = accessStrategyFactory;
        this.cache = cache;
        String timeout = properties.getProperty(CACHE_LOCK_TIMEOUT_PROPERTY, Integer.toString(DEFAULT_CACHE_LOCK_TIMEOUT));
        this.cacheLockTimeout = Timestamper.ONE_MS * Integer.decode(timeout);
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return cache.getName();
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() throws CacheException {
        try {
            cache.getCacheManager().removeCache(cache.getName());
        } catch (IllegalStateException e) {
            //When Spring and Hibernate are both involved this will happen in normal shutdown operation.
            //Do not throw an exception, simply log this one.
            LOG.debug("This can happen if multiple frameworks both try to shutdown ehcache", e);
        } catch (net.sf.ehcache.CacheException e) {
            if (e instanceof NonStopCacheException) {
                HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException((NonStopCacheException) e);
            } else {
                throw new CacheException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getSizeInMemory() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    public long getElementCountInMemory() {
        try {
            return cache.getSize();
        } catch (net.sf.ehcache.CacheException ce) {
            if (ce instanceof NonStopCacheException) {
                HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException((NonStopCacheException) ce);
                return -1;
            } else {
                throw new CacheException(ce);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getElementCountOnDisk() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    public Map toMap() {
        try {
            Map<Object, Object> result = new HashMap<Object, Object>();
            for (Object key : cache.getKeys()) {
                Element e = cache.get(key);
                if (e != null) {
                    result.put(key, e.getObjectValue());
                }
            }
            return result;
        } catch (Exception e) {
            if (e instanceof NonStopCacheException) {
                HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException((NonStopCacheException) e);
                return Collections.emptyMap();
            } else {
                throw new CacheException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long nextTimestamp() {
        return Timestamper.next();
    }

    /**
     * {@inheritDoc}
     */
    public int getTimeout() {
        return cacheLockTimeout;
    }

    /**
     * Return the Ehcache instance backing this Hibernate data region.
     */
    public Ehcache getEhcache() {
        return cache;
    }

    /**
     * Returns <code>true</code> if this region contains data for the given key.
     * <p>
     * This is a Hibernate 3.5 method.
     */
    public boolean contains(Object key) {
        return cache.isKeyInCache(key);
    }
}
