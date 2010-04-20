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
package net.sf.ehcache.hibernate;

import java.net.URL;
import java.util.Properties;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.hibernate.management.impl.ProviderMBeanRegistrationHelper;

import org.hibernate.cache.CacheException;

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
@Deprecated
public final class SingletonEhCacheProvider extends AbstractEhcacheProvider {

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

    private final ProviderMBeanRegistrationHelper mbeanRegistrationHelper = new ProviderMBeanRegistrationHelper();

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
        mbeanRegistrationHelper.registerMBean(manager, properties);
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
}
