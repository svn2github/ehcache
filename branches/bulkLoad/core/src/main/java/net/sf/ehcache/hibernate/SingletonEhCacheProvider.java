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
import net.sf.ehcache.hibernate.management.ProviderMBeanRegistrationHelper;
import net.sf.ehcache.util.ClassLoaderUtil;

import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheProvider;
import org.hibernate.cache.Timestamper;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton cache Provider plugin for Hibernate 3.2 and ehcache-1.2. New in this provider is support for
 * non Serializable keys and values. This provider works as a Singleton. No matter how many Hibernate Configurations
 * you have, only one ehcache CacheManager is used. See EhCacheProvider for a non-singleton implementation.
 * <p/>
 * Ehcache-1.2 also has many other features such as cluster support and listeners, which can be used seamlessly simply
 * by configurion in ehcache.xml.
 * <p/>
 * Use <code>hibernate.cache.provider_class=net.sf.ehcache.hibernate.SingletonEhCacheProvider</code> in the Hibernate configuration
 * to enable this provider for Hibernate's second level cache.
 * <p/>
 * Updated for ehcache-1.2. Note this provider requires ehcache-1.2.jar. Make sure ehcache-1.1.jar or earlier
 * is not in the classpath or it will not work.
 * <p/>
 * See http://ehcache.org for documentation on ehcache
 * <p/>
 *
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @version $Id$
 */
public final class SingletonEhCacheProvider implements CacheProvider {

    /**
     * The Hibernate system property specifying the location of the ehcache configuration file name.
     * <p/
     * If not set, ehcache.xml will be looked for in the root of the classpath.
     * <p/>
     * If set to say ehcache-1.xml, ehcache-1.xml will be looked for in the root of the classpath.
     */
    public static final String NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME = "net.sf.ehcache.configurationResourceName";

    private static final Logger LOG = LoggerFactory.getLogger(SingletonEhCacheProvider.class.getName());

    /**
     * To be backwardly compatible with a lot of Hibernate code out there, allow multiple starts and stops on the
     * one singleton CacheManager. Keep a count of references to only stop on when only one reference is held.
     */
    private static int referenceCount;

    private CacheManager manager;
    private final ProviderMBeanRegistrationHelper mbeanRegistrationHelper = new ProviderMBeanRegistrationHelper();


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
                SingletonEhCacheProvider.LOG.warn("Could not find a specific ehcache configuration for cache named ["
                        + name + "]; using defaults.");
                manager.addCache(name);
                cache = manager.getEhcache(name);
                SingletonEhCacheProvider.LOG.debug("started EHCache region: " + name);
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
     * Callback to perform any necessary initialization of the underlying cache implementation
     * during SessionFactory construction.
     * <p/>
     *
     * @param properties current configuration settings.
     */
    public final void start(Properties properties) throws CacheException {
        String configurationResourceName = null;
        if (properties != null) {
            configurationResourceName = (String) properties.get(NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME);
        }
        if (configurationResourceName == null || configurationResourceName.length() == 0) {
            manager = CacheManager.create();
            referenceCount++;
        } else {
            if (!configurationResourceName.startsWith("/")) {
                configurationResourceName = "/" + configurationResourceName;
                    LOG.debug("prepending / to {}. It should be placed in the rootof the classpath rather than in a package.", 
                            configurationResourceName);
            }
            URL url = loadResource(configurationResourceName);
            manager = CacheManager.create(url);
            referenceCount++;
        }
        mbeanRegistrationHelper.registerMBean(manager, properties == null ? "" : properties.getProperty(Environment.SESSION_FACTORY_NAME));
    }
    
    private URL loadResource(String configurationResourceName) {
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

    /**
     * Callback to perform any necessary cleanup of the underlying cache implementation
     * during SessionFactory.close().
     */
    public void stop() {
        if (manager != null) {
            referenceCount--;
            if (referenceCount == 0) {
                manager.shutdown();
            }
            manager = null;
        }
    }

    /**
     * Not sure what this is supposed to do.
     *
     * @return false to be safe
     */
    public final boolean isMinimalPutsEnabledByDefault() {
        return false;
    }

}
