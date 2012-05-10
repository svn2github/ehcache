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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A non-singleton EhCacheRegionFactory implementation.
 *
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @author Abhishek Sanoujam
 */
public class EhCacheRegionFactory extends AbstractEhcacheRegionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(EhCacheRegionFactory.class);

    /**
     * Creates a non-singleton EhCacheRegionFactory
     */
    public EhCacheRegionFactory(Properties prop) {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public void start(Settings settings, Properties properties) throws CacheException {
        this.settings = settings;
        if (manager != null) {
            LOG.warn("Attempt to restart an already started EhCacheRegionFactory. Use sessionFactory.close() " +
                    " between repeated calls to buildSessionFactory. Using previously created EhCacheRegionFactory." +
                    " If this behaviour is required, consider using SingletonEhCacheRegionFactory.");
            return;
        }

        try {
            String configurationResourceName = null;
            if (properties != null) {
                configurationResourceName = (String) properties.get(NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME);
            }
            if (configurationResourceName == null || configurationResourceName.length() == 0) {
                Configuration configuration = ConfigurationFactory.parseConfiguration();
                manager = new CacheManager(configuration);
            } else {
                URL url;
                try {
                    url = new URL(configurationResourceName);
                } catch (MalformedURLException e) {
                    url = loadResource(configurationResourceName);
                }
                Configuration configuration = HibernateUtil.loadAndCorrectConfiguration(url);
                manager = new CacheManager(HibernateUtil.overwriteCacheManagerIfConfigured(configuration, properties));
            }
            mbeanRegistrationHelper.registerMBean(manager, properties);
        } catch (net.sf.ehcache.CacheException e) {
            if (e.getMessage().startsWith("Cannot parseConfiguration CacheManager. Attempt to create a new instance of " +
                    "CacheManager using the diskStorePath")) {
                throw new CacheException("Attempt to restart an already started EhCacheRegionFactory. " +
                        "Use sessionFactory.close() between repeated calls to buildSessionFactory. " +
                        "Consider using SingletonEhCacheRegionFactory. Error from ehcache was: " + e.getMessage());
            } else {
                throw new CacheException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        try {
            if (manager != null) {
                mbeanRegistrationHelper.unregisterMBean();
                manager.shutdown();
                manager = null;
            }
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }
}
