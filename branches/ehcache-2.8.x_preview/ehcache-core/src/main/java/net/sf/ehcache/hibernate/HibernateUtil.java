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

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Chris Dennis
 * @author Abhishek Sanoujam
 */
public final class HibernateUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HibernateUtil.class);

    private HibernateUtil() { }

    /**
     * Create a cache manager configuration from the supplied url, correcting it for Hibernate compatibility.
     * <p>
     * Currently correcting for Hibernate compatibility means simply switching any identity based value modes to serialization.
     */
    static Configuration loadAndCorrectConfiguration(URL url) {
        Configuration config = ConfigurationFactory.parseConfiguration(url);
        if (config.getDefaultCacheConfiguration() != null
            && config.getDefaultCacheConfiguration().isTerracottaClustered()) {
            setupHibernateTimeoutBehavior(config.getDefaultCacheConfiguration().getTerracottaConfiguration().getNonstopConfiguration());
        }

        for (CacheConfiguration cacheConfig : config.getCacheConfigurations().values()) {
            if (cacheConfig.isTerracottaClustered()) {
                setupHibernateTimeoutBehavior(cacheConfig.getTerracottaConfiguration().getNonstopConfiguration());
            }
        }
        return config;
    }

    private static void setupHibernateTimeoutBehavior(NonstopConfiguration nonstopConfig) {
        nonstopConfig.getTimeoutBehavior().setType(TimeoutBehaviorType.EXCEPTION.getTypeName());
    }

    /**
     * Will overwrite the CacheManager name from the passed in configuration with the value of
     * net.sf.ehcache.hibernate.AbstractEhcacheRegionFactory#NET_SF_EHCACHE_CACHE_MANAGER_NAME of the passed in Properties
     * @param configuration the configuration
     * @param properties the properties passed in from Hibernate
     * @return the configuration object passed in, only for convenience
     */
    static Configuration overwriteCacheManagerIfConfigured(final Configuration configuration, final Properties properties) {
        final String cacheManagerName = properties.getProperty(AbstractEhcacheRegionFactory.NET_SF_EHCACHE_CACHE_MANAGER_NAME);
        if (cacheManagerName != null) {
            configuration.setName(cacheManagerName);
        }
        return configuration;
    }
}
