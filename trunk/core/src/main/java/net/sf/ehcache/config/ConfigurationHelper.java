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

package net.sf.ehcache.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration.CacheDecoratorFactoryConfiguration;
import net.sf.ehcache.constructs.CacheDecoratorFactory;
import net.sf.ehcache.distribution.CacheManagerPeerListener;
import net.sf.ehcache.distribution.CacheManagerPeerListenerFactory;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CacheManagerPeerProviderFactory;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.event.CacheManagerEventListenerFactory;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandler;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandlerFactory;
import net.sf.ehcache.exceptionhandler.ExceptionHandlingDynamicCacheProxy;
import net.sf.ehcache.util.ClassLoaderUtil;
import net.sf.ehcache.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The configuration for ehcache.
 * <p/>
 * This class can be populated through:
 * <ul>
 * <li>introspection by {@link ConfigurationFactory} or
 * <li>programmatically
 * </ul>
 *
 * @author Greg Luck
 * @version $Id$
 */
public final class ConfigurationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationHelper.class.getName());

    private Configuration configuration;
    private CacheManager cacheManager;

    /**
     * Only Constructor
     *
     * @param cacheManager
     * @param configuration
     */
    public ConfigurationHelper(CacheManager cacheManager, Configuration configuration) {
        if (cacheManager == null || configuration == null) {
            throw new IllegalArgumentException("Cannot have null parameters");
        }
        this.cacheManager = cacheManager;
        this.configuration = configuration;
    }

    /**
     * Tries to create a CacheLoader from the configuration using the factory
     * specified.
     *
     * @return The CacheExceptionHandler, or null if it could not be found.
     */
    public static CacheExceptionHandler createCacheExceptionHandler(
            CacheConfiguration.CacheExceptionHandlerFactoryConfiguration factoryConfiguration) throws CacheException {
        String className = null;
        CacheExceptionHandler cacheExceptionHandler = null;
        if (factoryConfiguration != null) {
            className = factoryConfiguration.getFullyQualifiedClassPath();
        }
        if (className == null || className.length() == 0) {
            LOG.debug("No CacheExceptionHandlerFactory class specified. Skipping...");
        } else {
            CacheExceptionHandlerFactory factory = (CacheExceptionHandlerFactory)
                    ClassLoaderUtil.createNewInstance(className);
            Properties properties = PropertyUtil.parseProperties(factoryConfiguration.getProperties(),
                    factoryConfiguration.getPropertySeparator());
            return factory.createExceptionHandler(properties);
        }
        return cacheExceptionHandler;
    }


    /**
     * Tries to load the class specified otherwise defaults to null
     *
     * @return a map of CacheManagerPeerProviders
     */
    public Map<String, CacheManagerPeerProvider> createCachePeerProviders() {
        String className = null;
        Map<String, CacheManagerPeerProvider> cacheManagerPeerProviders = new HashMap<String, CacheManagerPeerProvider>();
        List<FactoryConfiguration> cachePeerProviderFactoryConfiguration =
                configuration.getCacheManagerPeerProviderFactoryConfiguration();
        for (FactoryConfiguration factoryConfiguration : cachePeerProviderFactoryConfiguration) {

            if (factoryConfiguration != null) {
                className = factoryConfiguration.getFullyQualifiedClassPath();
            }
            if (className == null) {
                LOG.debug("No CachePeerProviderFactoryConfiguration specified. Not configuring a CacheManagerPeerProvider.");
                return null;
            } else {
                CacheManagerPeerProviderFactory cacheManagerPeerProviderFactory =
                        (CacheManagerPeerProviderFactory)
                                ClassLoaderUtil.createNewInstance(className);
                Properties properties = PropertyUtil.parseProperties(factoryConfiguration.getProperties(),
                        factoryConfiguration.getPropertySeparator());
                CacheManagerPeerProvider cacheManagerPeerProvider =
                        cacheManagerPeerProviderFactory.createCachePeerProvider(cacheManager, properties);
                cacheManagerPeerProviders.put(cacheManagerPeerProvider.getScheme(), cacheManagerPeerProvider);

            }
        }
        return cacheManagerPeerProviders;
    }

    /**
     * Tries to load the class specified otherwise defaults to null
     */
    public Map<String, CacheManagerPeerListener> createCachePeerListeners() {
        String className = null;
        Map<String, CacheManagerPeerListener> cacheManagerPeerListeners = new HashMap<String, CacheManagerPeerListener>();
        List<FactoryConfiguration> cacheManagerPeerListenerFactoryConfigurations =
                configuration.getCacheManagerPeerListenerFactoryConfigurations();
        boolean first = true;
        for (FactoryConfiguration factoryConfiguration : cacheManagerPeerListenerFactoryConfigurations) {

            if (factoryConfiguration != null) {
                className = factoryConfiguration.getFullyQualifiedClassPath();
            }
            if (className == null) {
                LOG.debug("No CachePeerListenerFactoryConfiguration specified. Not configuring a CacheManagerPeerListener.");
                return null;
            } else {
                CacheManagerPeerListenerFactory cacheManagerPeerListenerFactory = (CacheManagerPeerListenerFactory)
                        ClassLoaderUtil.createNewInstance(className);
                Properties properties = PropertyUtil.parseProperties(factoryConfiguration.getProperties(),
                        factoryConfiguration.getPropertySeparator());
                CacheManagerPeerListener cacheManagerPeerListener =
                        cacheManagerPeerListenerFactory.createCachePeerListener(cacheManager, properties);
                cacheManagerPeerListeners.put(cacheManagerPeerListener.getScheme(), cacheManagerPeerListener);
            }
        }
        return cacheManagerPeerListeners;
    }

    /**
     * Tries to load the class specified.
     *
     * @return If there is none returns null.
     */
    public final CacheManagerEventListener createCacheManagerEventListener() throws CacheException {
        String className = null;
        FactoryConfiguration cacheManagerEventListenerFactoryConfiguration =
                configuration.getCacheManagerEventListenerFactoryConfiguration();
        if (cacheManagerEventListenerFactoryConfiguration != null) {
            className = cacheManagerEventListenerFactoryConfiguration.getFullyQualifiedClassPath();
        }
        if (className == null || className.length() == 0) {
            LOG.debug("No CacheManagerEventListenerFactory class specified. Skipping...");
            return null;
        } else {
            CacheManagerEventListenerFactory factory = (CacheManagerEventListenerFactory)
                    ClassLoaderUtil.createNewInstance(className);
            Properties properties = PropertyUtil.parseProperties(cacheManagerEventListenerFactoryConfiguration.properties,
                    cacheManagerEventListenerFactoryConfiguration.getPropertySeparator());
            return factory.createCacheManagerEventListener(properties);
        }
    }


    /**
     * @return the disk store path, or null if not set.
     */
    public final String getDiskStorePath() {
        DiskStoreConfiguration diskStoreConfiguration = configuration.getDiskStoreConfiguration();
        if (diskStoreConfiguration == null) {
            return null;
        } else {
            return diskStoreConfiguration.getPath();
        }
    }

    /**
     * @return the Default Cache
     * @throws net.sf.ehcache.CacheException if there is no default cache
     */
    public final Ehcache createDefaultCache() throws CacheException {
        CacheConfiguration cacheConfiguration = configuration.getDefaultCacheConfiguration();
        if (cacheConfiguration == null) {
            throw new CacheException("Illegal configuration. No default cache is configured.");
        } else {
            cacheConfiguration.name = Cache.DEFAULT_CACHE_NAME;
            return createCache(cacheConfiguration);
        }
    }

    /**
     * Creates unitialised caches for each cache configuration found
     *
     * @return an empty set if there are none,
     */
    public final Set createCaches() {
        Set caches = new HashSet();
        Set cacheConfigurations = configuration.getCacheConfigurations().entrySet();
        for (Iterator iterator = cacheConfigurations.iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            CacheConfiguration cacheConfiguration = (CacheConfiguration) entry.getValue();
            cacheConfiguration.setDefaultTransactionManager(configuration.getDefaultTransactionManager());
            Ehcache cache = createCache(cacheConfiguration);
            caches.add(cache);
        }
        return caches;
    }


    /**
     * Calculates the number of caches in the configuration that overflow to disk
     */
    public final Integer numberOfCachesThatOverflowToDisk() {
        int count = 0;
        Set cacheConfigurations = configuration.getCacheConfigurations().entrySet();
        for (Iterator iterator = cacheConfigurations.iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            CacheConfiguration cacheConfiguration = (CacheConfiguration) entry.getValue();
            if (cacheConfiguration.overflowToDisk) {
                count++;
            }
        }
        return Integer.valueOf(count);
    }


    /**
     * Calculates the number of caches in the configuration that are diskPersistent
     */
    public final Integer numberOfCachesThatAreDiskPersistent() {
        int count = 0;
        Set cacheConfigurations = configuration.getCacheConfigurations().entrySet();
        for (Iterator iterator = cacheConfigurations.iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            CacheConfiguration cacheConfiguration = (CacheConfiguration) entry.getValue();
            if (cacheConfiguration.isDiskPersistent()) {
                count++;
            }
        }
        return Integer.valueOf(count);
    }

    /**
     * Creates a cache from configuration where the configuration cache name matches the given name
     *
     * @return the cache, or null if there is no match
     */
    final Ehcache createCacheFromName(String name) {
        CacheConfiguration cacheConfiguration = null;
        Set cacheConfigurations = configuration.getCacheConfigurations().entrySet();
        for (Iterator iterator = cacheConfigurations.iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            CacheConfiguration cacheConfigurationCandidate = (CacheConfiguration) entry.getValue();
            if (cacheConfigurationCandidate.name.equals(name)) {
                cacheConfiguration = cacheConfigurationCandidate;
                break;
            }
        }
        if (cacheConfiguration == null) {
            return null;
        } else {
            return createCache(cacheConfiguration);
        }
    }

    /**
     * Create a cache given a cache configuration
     *
     * @param cacheConfiguration
     */
    final Ehcache createCache(CacheConfiguration cacheConfiguration) {
        Ehcache cache = new Cache(cacheConfiguration.clone(), null, null);
        cache = applyCacheExceptionHandler(cacheConfiguration, cache);
        return cache;
    }

    private Ehcache applyCacheExceptionHandler(CacheConfiguration cacheConfiguration, Ehcache cache) {
        CacheExceptionHandler cacheExceptionHandler =
                createCacheExceptionHandler(cacheConfiguration.getCacheExceptionHandlerFactoryConfiguration());
        cache.setCacheExceptionHandler(cacheExceptionHandler);

        if (cache.getCacheExceptionHandler() != null) {
            return ExceptionHandlingDynamicCacheProxy.createProxy(cache);
        }
        return cache;
    }
    
    /**
     * Creates decorated ehcaches for the cache, if any configured in ehcache.xml
     * 
     * @param cache the cache
     * @return List of the decorated ehcaches, if any configured in ehcache.xml otherwise returns empty list
     */
    public List<Ehcache> createCacheDecorators(Ehcache cache) {
        CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
        if (cacheConfiguration == null) {
            return createDefaultCacheDecorators(cache, configuration.getDefaultCacheConfiguration());
        }
        List<CacheDecoratorFactoryConfiguration> cacheDecoratorConfigurations = cacheConfiguration.getCacheDecoratorConfigurations();
        if (cacheDecoratorConfigurations == null || cacheDecoratorConfigurations.size() == 0) {
            LOG.debug("CacheDecoratorFactory not configured. Skipping for '" + cache.getName() + "'.");
            return createDefaultCacheDecorators(cache, configuration.getDefaultCacheConfiguration());
        }
        List<Ehcache> result = new ArrayList<Ehcache>();
        for (CacheDecoratorFactoryConfiguration factoryConfiguration : cacheDecoratorConfigurations) {
            Ehcache decoratedCache = createDecoratedCache(cache, factoryConfiguration, false);
            if (decoratedCache != null) {
                result.add(decoratedCache);
            }
        }
        for (Ehcache defaultDecoratedCache : createDefaultCacheDecorators(cache, configuration.getDefaultCacheConfiguration())) {
            result.add(defaultDecoratedCache);
        }
        return result;
    }

    /**
     * Creates default cache decorators specified in the default cache configuration if any
     * @param cache the underlying cache that will be decorated
     * @param defaultCacheConfiguration default cache configuration
     * @return list of decorated caches
     */
    public static List<Ehcache> createDefaultCacheDecorators(Ehcache cache, CacheConfiguration defaultCacheConfiguration) {
        if (cache == null) {
            throw new CacheException("Underlying cache cannot be null when creating decorated caches.");
        }
        List<CacheDecoratorFactoryConfiguration> defaultCacheDecoratorConfigurations = defaultCacheConfiguration
                .getCacheDecoratorConfigurations();
        if (defaultCacheDecoratorConfigurations == null || defaultCacheDecoratorConfigurations.size() == 0) {
            LOG.debug("CacheDecoratorFactory not configured for defaultCache. Skipping for '" + cache.getName() + "'.");
            return Collections.emptyList();
        }
        List<Ehcache> result = new ArrayList<Ehcache>();
        Set<String> newCacheNames = new HashSet<String>();
        for (CacheDecoratorFactoryConfiguration factoryConfiguration : defaultCacheDecoratorConfigurations) {
            Ehcache decoratedCache = createDecoratedCache(cache, factoryConfiguration, true);
            if (decoratedCache != null) {
                if (newCacheNames.contains(decoratedCache.getName())) {
                    throw new InvalidConfigurationException(
                            "Looks like the defaultCache is configured with multiple CacheDecoratorFactory's "
                                    + "that does not set unique names for newly created caches. Please fix the "
                                    + "CacheDecoratorFactory and/or the config to set unique names for newly created caches.");
                }
                newCacheNames.add(decoratedCache.getName());
                result.add(decoratedCache);
            }
        }
        return result;
    }

    /**
     * Creates the decorated cache from the decorator config specified. Returns null if the name of the factory class is not specified
     */
    private static Ehcache createDecoratedCache(Ehcache cache,
            CacheConfiguration.CacheDecoratorFactoryConfiguration factoryConfiguration, boolean forDefaultCache) {
        if (factoryConfiguration == null) {
            return null;
        }
        String className = factoryConfiguration.getFullyQualifiedClassPath();
        if (className == null) {
            LOG.debug("CacheDecoratorFactory was specified without the name of the factory. Skipping...");
            return null;
        } else {
            CacheDecoratorFactory factory = (CacheDecoratorFactory) ClassLoaderUtil.createNewInstance(className);
            Properties properties = PropertyUtil.parseProperties(factoryConfiguration.getProperties(),
                    factoryConfiguration.getPropertySeparator());
            if (forDefaultCache) {
                return factory.createDefaultDecoratedEhcache(cache, properties);
            } else {
                return factory.createDecoratedEhcache(cache, properties);
            }
        }
    }

    /**
     * @return the Configuration used
     */
    public final Configuration getConfigurationBean() {
        return configuration;
    }
}
