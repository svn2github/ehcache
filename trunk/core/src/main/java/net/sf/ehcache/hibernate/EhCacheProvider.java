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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.hibernate.management.impl.ProviderMBeanRegistrationHelper;

import org.hibernate.cache.CacheException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache Provider plugin for Hibernate 3.2 and ehcache-1.2. New in this provider are ehcache support for multiple
 * Hibernate session factories, each with its own ehcache configuration, and non Serializable keys and values.
 * Ehcache-1.2 also has many other features such as cluster support and listeners, which can be used seamlessly simply
 * by configurion in ehcache.xml.
 * <p/>
 * Use <code>hibernate.cache.provider_class=net.sf.ehcache.hibernate.EhCacheProvider</code> in the Hibernate configuration
 * to enable this provider for Hibernate's second level cache.
 * <p/>
 * When configuring multiple ehcache CacheManagers, as you would where you have multiple Hibernate Configurations and
 * multiple SessionFactories, specify in each Hibernate configuration the ehcache configuration using
 * the property <code>net.sf.ehcache.configurationResourceName</code> An example to set an ehcach configuration
 * called ehcache-2.xml would be <code>net.sf.ehcache.configurationResourceName=/ehcache-2.xml</code>. If the leading
 * slash is not there one will be added. The configuration file will be looked for in the root of the classpath.
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
public final class EhCacheProvider extends AbstractEhcacheProvider {

    /**
     * The Hibernate system property specifying the location of the ehcache configuration file name.
     * <p/>
     * If not set, ehcache.xml will be looked for in the root of the classpath.
     * <p/>
     * If set to say ehcache-1.xml, ehcache-1.xml will be looked for in the root of the classpath.
     */
    public static final String NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME = "net.sf.ehcache.configurationResourceName";

    private static final Logger LOG = LoggerFactory.getLogger(EhCacheProvider.class.getName());

    private final ProviderMBeanRegistrationHelper mbeanRegistrationHelper = new ProviderMBeanRegistrationHelper();

    /**
     * Callback to perform any necessary initialization of the underlying cache implementation
     * during SessionFactory construction.
     * <p/>
     *
     * @param properties current configuration settings.
     */
    public final void start(Properties properties) throws CacheException {
        if (manager != null) {
            LOG.warn("Attempt to restart an already started EhCacheProvider. Use sessionFactory.close() " +
                    " between repeated calls to buildSessionFactory. Using previously created EhCacheProvider." +
                    " If this behaviour is required, consider using SingletonEhCacheProvider.");
            return;
        }
        try {
            String configurationResourceName = null;
            if (properties != null) {
                configurationResourceName = (String) properties.get(NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME);
            }
            if (configurationResourceName == null || configurationResourceName.length() == 0) {
                manager = CacheManager.create();
            } else {
                URL url;
                try {
                    url = new URL(configurationResourceName);
                } catch (MalformedURLException e) {
                    url = loadResource(configurationResourceName);
                }
                manager = CacheManager.create(HibernateUtil.loadAndCorrectConfiguration(url));
            }
            mbeanRegistrationHelper.registerMBean(manager, properties);
        } catch (net.sf.ehcache.CacheException e) {
            if (e.getMessage().startsWith("Cannot parseConfiguration CacheManager. Attempt to create a new instance of " +
                    "CacheManager using the diskStorePath")) {
                throw new CacheException("Attempt to restart an already started EhCacheProvider. Use sessionFactory.close() " +
                        " between repeated calls to buildSessionFactory. Consider using SingletonEhCacheProvider. Error from " +
                        " ehcache was: " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    /**
     * Callback to perform any necessary cleanup of the underlying cache implementation
     * during SessionFactory.close().
     */
    public final void stop() {
        if (manager != null) {
            manager.shutdown();
            manager = null;
        }
    }
}

