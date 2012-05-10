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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.ValueMode;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;

import org.hibernate.cache.CacheException;
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
            if (ValueMode.IDENTITY.equals(config.getDefaultCacheConfiguration().getTerracottaConfiguration().getValueMode())) {
                LOG.warn("The default cache value mode for this Ehcache configuration is \"identity\". This is incompatible with clustered "
                        + "Hibernate caching - the value mode has therefore been switched to \"serialization\"");
                config.getDefaultCacheConfiguration().getTerracottaConfiguration().setValueMode(ValueMode.SERIALIZATION.name());
            }
            setupHibernateTimeoutBehavior(config.getDefaultCacheConfiguration().getTerracottaConfiguration().getNonstopConfiguration());
        }

        for (CacheConfiguration cacheConfig : config.getCacheConfigurations().values()) {
            if (cacheConfig.isTerracottaClustered()) {
                if (ValueMode.IDENTITY.equals(cacheConfig.getTerracottaConfiguration().getValueMode())) {
                LOG.warn("The value mode for the {0} cache is \"identity\". This is incompatible with clustered Hibernate caching - "
                        + "the value mode has therefore been switched to \"serialization\"", cacheConfig.getName());
                    cacheConfig.getTerracottaConfiguration().setValueMode(ValueMode.SERIALIZATION.name());
                }
                setupHibernateTimeoutBehavior(cacheConfig.getTerracottaConfiguration().getNonstopConfiguration());
            }
        }
        return config;
    }

    private static void setupHibernateTimeoutBehavior(NonstopConfiguration nonstopConfig) {
        nonstopConfig.getTimeoutBehavior().setType(TimeoutBehaviorType.EXCEPTION.getTypeName());
    }

    /**
     * Validates that the supplied Ehcache instance is valid for use as a Hibernate cache.
     */
    static void validateEhcache(Ehcache cache) throws CacheException {
        CacheConfiguration cacheConfig = cache.getCacheConfiguration();

        if (cacheConfig.isTerracottaClustered()) {
            TerracottaConfiguration tcConfig = cacheConfig.getTerracottaConfiguration();
            switch (tcConfig.getValueMode()) {
                case IDENTITY:
                    throw new CacheException("The clustered Hibernate cache " + cache.getName() + " is using IDENTITY value mode.\n"
                           + "Identity value mode cannot be used with Hibernate cache regions.");
                case SERIALIZATION:
                default:
                    // this is the recommended valueMode
                    break;
            }
        }
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
