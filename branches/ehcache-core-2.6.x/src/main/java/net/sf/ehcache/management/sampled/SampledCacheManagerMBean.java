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

package net.sf.ehcache.management.sampled;

/**
 * An MBean for CacheManager exposing sampled cache usage statistics
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 */
public interface SampledCacheManagerMBean extends CacheManagerSampler {
    /**
     * CACHE_MANAGER_CHANGED
     */
    final String CACHE_MANAGER_CHANGED = "CacheManagerChanged";

    /**
     * CACHES_ENABLED
     */
    final String CACHES_ENABLED = "CachesEnabled";

    /**
     * CACHES_CLEARED
     */
    final String CACHES_CLEARED = "CachesCleared";

    /**
     * STATISTICS_RESET
     */
    final String STATISTICS_RESET = "StatisticsReset";

    /**
     * STATISTICS_ENABLED
     */
    final String STATISTICS_ENABLED = "StatisticsEnabled";

    /**
     * Gets the name used to register this mbean.
     */
    String getMBeanRegisteredName();
}