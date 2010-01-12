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
package net.sf.ehcache.hibernate;

import java.util.Properties;
import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheProvider;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cfg.Settings;

/**
 * Abstract implementation of an EhCache specific RegionFactory.
 * <p>
 * This abstract class wraps an EhCache CacheProvider as an instance of RegionFactory.
 * 
 * @author Chris Dennis
 */
abstract class AbstractEhCacheRegionFactory implements RegionFactory {
    
    private final EhCacheProvider provider;

    private Settings settings;

    /**
     * Creates an AbstractEhCacheRegionFactory backed by the given CacheProvider.
     * <p>
     * The supplied CacheProvider must return instances of EhCache from buildCache otherwise bad things will happen.
     * 
     * @param provider backing cache provider.
     */
    AbstractEhCacheRegionFactory(CacheProvider provider) {
        this.provider = new EhCacheProvider();
    }

    /**
     * {@inheritDoc}
     */
    public void start(Settings settings, Properties properties) throws CacheException {
        this.settings = settings;
        provider.start(properties);
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        provider.stop();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMinimalPutsEnabledByDefault() {
        return provider.isMinimalPutsEnabledByDefault();
    }

    /**
     * {@inheritDoc}
     */
    public long nextTimestamp() {
        return provider.nextTimestamp();
    }

    /**
     * {@inheritDoc}
     */
    public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata) throws CacheException {
        return new EhCacheEntityRegion(provider.buildCache(regionName, properties), settings, metadata);
    }

    /**
     * {@inheritDoc}
     */
    public CollectionRegion buildCollectionRegion(String regionName, Properties properties, CacheDataDescription metadata)
            throws CacheException {
        return new EhCacheCollectionRegion(provider.buildCache(regionName, properties), settings, metadata);
    }

    /**
     * {@inheritDoc}
     */
    public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException {
        return new EhCacheQueryResultsRegion(provider.buildCache(regionName, properties), settings);
    }

    /**
     * {@inheritDoc}
     */
    public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException {
        return new EhCacheTimestampsRegion(provider.buildCache(regionName, properties), settings);
    }
}
