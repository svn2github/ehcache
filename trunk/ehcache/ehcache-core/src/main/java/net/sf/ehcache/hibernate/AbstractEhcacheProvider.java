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
import net.sf.ehcache.util.ClassLoaderUtil;
import net.sf.ehcache.util.Timestamper;

import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract super-class for all Ehcache Hibernate CacheProvider implementations.
 * 
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 */
abstract class AbstractEhcacheProvider implements CacheProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractEhcacheProvider.class.getName());

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
    public final boolean isMinimalPutsEnabledByDefault() {
        return true;
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
