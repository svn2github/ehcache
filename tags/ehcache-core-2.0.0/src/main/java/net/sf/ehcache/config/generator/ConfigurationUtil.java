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

package net.sf.ehcache.config.generator;

import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

/**
 * Utility class with static methods for generating configuration texts in different ways based on input
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 */
public abstract class ConfigurationUtil {

    /**
     * Generate configuration text based on the input {@link ConfigurationSource}
     * 
     * @param configSource
     * @return String representing the input {@link ConfigurationSource}
     */
    public static String generateConfigurationTextFromSource(ConfigurationSource configSource) {
        if (configSource == null) {
            throw new AssertionError("ConfigSource cannot be null");
        }
        Configuration config = configSource.createConfiguration();
        return generateConfigurationText(config, config.getDefaultCacheConfiguration(), config.getCacheConfigurations());
    }

    /**
     * Generates configuration text based on the input {@link Configuration}. {@link CacheConfiguration}'s from the input
     * {@link CacheManager} will override the {@link CacheConfiguration} present in the {@link Configuration}
     * 
     * @param cacheManager
     * @param configuration
     * @return String representing the configuration based on the input {@link Configuration} and {@link CacheManager}
     */
    public static String generateConfigurationTextFromConfiguration(CacheManager cacheManager, Configuration configuration) {
        if (configuration == null) {
            throw new AssertionError("Confuguration cannot be null");
        }
        if (cacheManager == null) {
            throw new AssertionError("CacheManager cannot be null");
        }
        Map<String, CacheConfiguration> cacheConfigs = configuration.getCacheConfigurations();
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                // use the actual CacheConfiguration from the cache and not the cache manager
                cacheConfigs.put(cache.getCacheConfiguration().getName(), cache.getCacheConfiguration());
            }
        }
        return generateConfigurationText(configuration, configuration.getDefaultCacheConfiguration(), cacheConfigs);
    }

    private static String generateConfigurationText(Configuration configuration, CacheConfiguration defaultCacheConfiguration,
            Map<String, CacheConfiguration> cacheConfigs) {
        return new ConfigurationGenerator().generate(configuration, defaultCacheConfiguration, cacheConfigs);
    }

    /**
     * Generates configuration text for a cache from the {@link ConfigurationSource}
     * 
     * @param configSource
     * @param cacheName
     * @return String representing the cache configuration for the input cacheName
     */
    public static String generateConfigurationTextForCacheFromSource(ConfigurationSource configSource, String cacheName) {
        if (configSource == null) {
            throw new AssertionError("ConfigSource cannot be null");
        }
        Configuration config = configSource.createConfiguration();
        CacheConfiguration cacheConfig = config.getCacheConfigurations().get(cacheName);
        if (cacheConfig == null) {
            return "";
        } else {
            return new ConfigurationGenerator().generate(cacheConfig);
        }
    }

    /**
     * Generates configuration text for a cache based on the {@link Cache} present in the input {@link CacheManager} for the input cacheName
     * 
     * @param cacheManager
     * @param cacheName
     * @return String representing configuration for the cacheName
     */
    public static String generateConfigurationTextForCache(CacheManager cacheManager, String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return "";
        } else {
            return new ConfigurationGenerator().generate(cache.getCacheConfiguration());
        }
    }
}
