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

import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.store.Store;

/**
 * Interface implemented by classes providing access to extended functionality.
 *
 * @author Chris Dennis
 */
public interface FeaturesManager {

    /**
     * Fully qualified classname of the enterprise features manager
     */
    public static final String ENTERPRISE_FM_CLASSNAME = "net.sf.ehcache.EnterpriseFeaturesManager";

    /**
     * Create a store for the given cache.
     *
     * @param cache cache to create a store for
     * @param onHeapPool on-heap pool
     * @param onDiskPool on-disk pool
     * @return a store for the given cache
     */
    Store createStore(Cache cache, Pool onHeapPool, Pool onDiskPool);

    /**
     * Called on {@code CacheManager} creation.
     */
    void startup();

    /**
     * Called on {@code CacheManager} shutdown.
     */
    void shutdown();
}
