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

package net.sf.ehcache.config.generator;

import java.util.Map;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

/**
 * Utility class for generating configuration texts.
 *
 * <p />
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @deprecated Use {@link ConfigurationUtil#generateCacheManagerConfigurationText(Configuration)} or
 *             {@link ConfigurationUtil#generateCacheConfigurationText(CacheConfiguration)} instead
 */
@Deprecated
public class ConfigurationGenerator {

    /**
     * Generates the configuration text for the provided {@link Configuration}, the default {@link CacheConfiguration} and the map of
     * {@link CacheConfiguration}'s
     *
     * @param configuration
     * @param defaultCacheConfiguration
     * @param cacheConfigs
     * @return String equivalent to an ehcache.xml for the input parameters
     * @deprecated use appropriate methods in {@link ConfigurationUtil} instead
     */
    @Deprecated
    public String generate(Configuration configuration, CacheConfiguration defaultCacheConfiguration,
            Map<String, CacheConfiguration> cacheConfigs) {
        return ConfigurationUtil.generateCacheManagerConfigurationText(configuration);
    }

    /**
     * Generates configuration text for a specific cache using the input {@link CacheConfiguration}
     *
     * @param cacheConfiguration
     * @return String containing configuration for the input {@link CacheConfiguration}
     * @deprecated use appropriate methods in {@link ConfigurationUtil} instead
     */
    @Deprecated
    public String generate(Configuration configuration, CacheConfiguration cacheConfiguration) {
        return ConfigurationUtil.generateCacheConfigurationText(configuration, cacheConfiguration);
    }

}
