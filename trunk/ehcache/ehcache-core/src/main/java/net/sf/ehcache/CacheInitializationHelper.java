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

package net.sf.ehcache;

/**
 * Helper class to initialize an {@link Ehcache} with a {@link CacheManager} without
 * adding the {@link Ehcache} to the {@link CacheManager}.
 *
 * @author Tim Wu
 */
public class CacheInitializationHelper {

    private final CacheManager cacheManager;

    /**
     * Create a cache initializer with the given {@link CacheManager}
     *
     * @param cacheManager
     */
    public CacheInitializationHelper(final CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Initialize the {@link Ehcache}.
     *
     * @param cache
     */
    public void initializeEhcache(final Ehcache cache) {
        this.cacheManager.initializeEhcache(cache, false);
    }

    /**
     * Initialize the given {@link Ehcache} using the given {@link CacheManager}
     *
     * @param cacheManager
     * @param cache
     */
    public static void initializeEhcache(final CacheManager cacheManager, final Ehcache cache) {
        cacheManager.initializeEhcache(cache, false);
    }

    /**
     * Get a currently initializing {@link CacheManager} by name
     *
     * @param name name of the {@link CacheManager}, can be null
     * @return the initializing {@link CacheManager}
     */
    public static CacheManager getInitializingCacheManager(String name) {
      return CacheManager.getInitializingCacheManager(name);
    }
}
