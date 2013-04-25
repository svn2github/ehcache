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
package net.sf.ehcache.hibernate;

import java.net.URL;
import java.util.Properties;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.hibernate.management.impl.ProviderMBeanRegistrationHelper;
import net.sf.ehcache.hibernate.nonstop.NonstopAccessStrategyFactory;
import net.sf.ehcache.hibernate.regions.EhcacheQueryResultsRegion;
import net.sf.ehcache.hibernate.regions.EhcacheTimestampsRegion;
import net.sf.ehcache.hibernate.regions.EhcacheEntityRegion;
import net.sf.ehcache.hibernate.regions.EhcacheCollectionRegion;
import net.sf.ehcache.hibernate.strategy.EhcacheAccessStrategyFactory;
import net.sf.ehcache.hibernate.strategy.EhcacheAccessStrategyFactoryImpl;
import net.sf.ehcache.util.ClassLoaderUtil;
import net.sf.ehcache.util.Timestamper;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cfg.Settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of an Ehcache specific RegionFactory.
 *
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @author Abhishek Sanoujam
 */
abstract class AbstractEhcacheRegionFactory implements RegionFactory {

    /**
     * The Hibernate system property specifying the location of the ehcache configuration file name.
     * <p/>
     * If not set, ehcache.xml will be looked for in the root of the classpath.
     * <p/>
     * If set to say ehcache-1.xml, ehcache-1.xml will be looked for in the root of the classpath.
     */
    public static final String NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME = "net.sf.ehcache.configurationResourceName";

    /**
     * The Hibernate system property specifying the name of the CacheManager being created.
     * <p/>
     * If not set, it will either use the ehcache.xml to name the CacheManager, or fallback to default CacheManager naming.
     */
    public static final String NET_SF_EHCACHE_CACHE_MANAGER_NAME = "net.sf.ehcache.cacheManagerName";

    private static final Logger LOG = LoggerFactory.getLogger(AbstractEhcacheRegionFactory.class);

    /**
     * MBean registration helper class instance for Ehcache Hibernate MBeans.
     */
    protected final ProviderMBeanRegistrationHelper mbeanRegistrationHelper = new ProviderMBeanRegistrationHelper();

    /**
     * Ehcache CacheManager that supplied Ehcache instances for this Hibernate RegionFactory.
     */
    protected volatile CacheManager manager;

    /**
     * Settings object for the Hibernate persistence unit.
     */
    protected Settings settings;

    /**
     * {@link EhcacheAccessStrategyFactory} for creating various access strategies
     */
    protected final EhcacheAccessStrategyFactory accessStrategyFactory = new NonstopAccessStrategyFactory(
            new EhcacheAccessStrategyFactoryImpl());

    /**
     * Whether to optimize for minimals puts or minimal gets.
     * <p>
     * Indicates whether when operating in non-strict read/write or read-only mode
     * Hibernate should optimize the access patterns for minimal puts or minimal gets.
     * In Ehcache we default to minimal puts since this should have minimal to no
     * affect on unclustered users, and has great benefit for clustered users.
     * <p>
     * This setting can be overridden by setting the "hibernate.cache.use_minimal_puts"
     * property in the Hibernate configuration.
     *
     * @return true, optimize for minimal puts
     */
    public boolean isMinimalPutsEnabledByDefault() {
        return true;
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
    public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata) throws CacheException {
        return new EhcacheEntityRegion(accessStrategyFactory, getCache(regionName), settings, metadata, properties);
    }

    /**
     * {@inheritDoc}
     */
    public CollectionRegion buildCollectionRegion(String regionName, Properties properties, CacheDataDescription metadata)
            throws CacheException {
        return new EhcacheCollectionRegion(accessStrategyFactory, getCache(regionName), settings, metadata, properties);
    }

    /**
     * {@inheritDoc}
     */
    public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException {
        return new EhcacheQueryResultsRegion(accessStrategyFactory, getCache(regionName), properties);
    }

    /**
     * {@inheritDoc}
     */
    public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException {
        return new EhcacheTimestampsRegion(accessStrategyFactory, getCache(regionName), properties);
    }

    private Ehcache getCache(String name) throws CacheException {
        try {
            Ehcache cache = manager.getEhcache(name);
            if (cache == null) {
                LOG.warn("Couldn't find a specific ehcache configuration for cache named [" + name + "]; using defaults.");
                manager.addCache(name);
                cache = manager.getEhcache(name);
                LOG.debug("started EHCache region: " + name);
            }
            return cache;
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }

    }

    /**
     * Load a resource from the classpath.
     */
    protected static URL loadResource(String configurationResourceName) {
        ClassLoader standardClassloader = ClassLoaderUtil.getStandardClassLoader();
        URL url = null;
        if (standardClassloader != null) {
            url = standardClassloader.getResource(configurationResourceName);
        }
        if (url == null) {
            url = AbstractEhcacheRegionFactory.class.getResource(configurationResourceName);
        }

            LOG.debug("Creating EhCacheRegionFactory from a specified resource: {}.  Resolved to URL: {}", configurationResourceName, url);
        if (url == null) {

                LOG.warn("A configurationResourceName was set to {} but the resource could not be loaded from the classpath." +
                        "Ehcache will configure itself using defaults.", configurationResourceName);
        }
        return url;
    }

    /**
     * Default access-type used when the configured using JPA 2.0 config.  JPA 2.0 allows <code>@Cacheable(true)</code> to be attached to an
     * entity without any access type or usage qualification.
     * <p>
     * We are conservative here in specifying {@link AccessType#READ_WRITE} so as to follow the mantra of "do no harm".
     * <p>
     * This is a Hibernate 3.5 method.
     */
    public AccessType getDefaultAccessType() {
        return AccessType.READ_WRITE;
    }

}
