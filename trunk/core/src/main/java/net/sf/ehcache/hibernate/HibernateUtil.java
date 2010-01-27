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
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.ValueMode;

import org.hibernate.cache.CacheException;

/**
 *
 * @author Chris Dennis
 */
public final class HibernateUtil {

    private HibernateUtil() { }

    /**
     * Create a cache manager configuration from the supplied url, correcting it for Hibernate compatibility.
     * <p>
     * Currently correcting for Hibernate compatibility means simply switching any identity based value modes to serialization.
     */
    static Configuration loadAndCorrectConfiguration(URL url) {
        Configuration config = ConfigurationFactory.parseConfiguration(url);
        if (config.getDefaultCacheConfiguration().isTerracottaClustered()) {
            config.getDefaultCacheConfiguration().getTerracottaConfiguration().setValueMode(ValueMode.SERIALIZATION.name());
        }

        for (CacheConfiguration cacheConfig : config.getCacheConfigurations().values()) {
            if (cacheConfig.isTerracottaClustered()) {
                cacheConfig.getTerracottaConfiguration().setValueMode(ValueMode.SERIALIZATION.name());
            }
        }
        return config;
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


}
