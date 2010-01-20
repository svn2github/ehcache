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

import java.net.URL;
import java.util.Properties;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.util.ClassLoaderUtil;

import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheProvider;
import org.hibernate.cache.Timestamper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract super-class for all Ehcache Hibernate CacheProvider implementations.
 * 
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 */
abstract class AbstractEhCacheProvider implements CacheProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractEhCacheProvider.class.getName());

    /**
     * CacheManager instance that creates/builds Cache instances for this provider.
     */
    protected volatile CacheManager manager;

    /**
     * Builds a Cache.
     * <p/>
     * Even though this method provides properties, they are not used.
     * Properties for EHCache are specified in the ehcache.xml file.
     * Configuration will be read from ehcache.xml for a cache declaration
     * where the name attribute matches the name parameter in this builder.
     *
     * @param name       the name of the cache. Must match a cache configured in ehcache.xml
     * @param properties not used
     * @return a newly built cache will be built and initialised
     * @throws org.hibernate.cache.CacheException
     *          inter alia, if a cache of the same name already exists
     */
    public final Cache buildCache(String name, Properties properties) throws CacheException {
        try {
            net.sf.ehcache.Ehcache cache = manager.getEhcache(name);
            if (cache == null) {
                LOG.warn("Could not find a specific ehcache configuration for cache named [" + name + "]; using defaults.");
                manager.addCache(name);
                cache = manager.getEhcache(name);
                LOG.debug("started EHCache region: " + name);
            }
            validateEhcache(cache);
            return new EhCache(cache);
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }
    
    /**
     * Returns the next timestamp.
     */
    public final long nextTimestamp() {
        return Timestamper.next();
    }

    /**
     * Not sure what this is supposed to do.
     *
     * @return false to be safe
     */
    public final boolean isMinimalPutsEnabledByDefault() {
        return false;
    }

    /**
     * Validates that the supplied Ehcache instance is valid for use as a Hibernate cache.
     */
    protected static void validateEhcache(net.sf.ehcache.Ehcache cache) throws CacheException {
        CacheConfiguration cacheConfig = cache.getCacheConfiguration();

        if (cacheConfig.isTerracottaClustered()) {
            TerracottaConfiguration tcConfig = cacheConfig.getTerracottaConfiguration();
            switch (tcConfig.getValueMode()) {
                case IDENTITY:
                    throw new CacheException("The clustered Hibernate cache " + cache.getName() + " is using IDENTITY value mode.\n"
                           + "Identity value mode cannot be used with Hibernate cache regions.");
                case HIBERNATE:
                    // this is the recommended valueMode
                    break;
                default:
                    LOG.warn("The clustered Hibernate cache " + cache.getName() + " is using " + tcConfig.getValueMode() + " value mode .\n"
                           + "Using a value mode strategy other than \"hibernate\" will deliver sub-optimal performance.");
                    break;
            }
        }
    }

    /**
     * Load the supplied resource from the classpath.
     */
    protected URL loadResource(String configurationResourceName) {
        ClassLoader standardClassloader = ClassLoaderUtil.getStandardClassLoader();
        URL url = null;
        if (standardClassloader != null) {
            url = standardClassloader.getResource(configurationResourceName);
        }
        if (url == null) {
            url = this.getClass().getResource(configurationResourceName);
        }

        LOG.debug("Creating EhCacheProvider from a specified resource: {}. Resolved to URL: ", configurationResourceName, url);
        if (url == null) {
                LOG.warn("A configurationResourceName was set to {} but the resource could not be loaded from the classpath." +
                        "Ehcache will configure itself using defaults.", configurationResourceName);
        }
        return url;
    }
}
