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
import net.sf.ehcache.transaction.SoftLockFactory;
import net.sf.ehcache.transaction.SoftLockManager;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.util.UpdateChecker;
import net.sf.ehcache.writer.writebehind.WriteBehind;

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
     * Create a WriteBehind instance for the given cache.
     *
     * @param cache cache to create write behind for
     * @return a write behind instance
     */
    WriteBehind createWriteBehind(Cache cache);

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
     * Create a transaction map for the associated cache manager
     *
     * @return a transaction map for the cache manager
     */
    TransactionIDFactory createTransactionIDFactory();

    /**
     * Create a soft-lock map for the given cache
     *
     * @return a soft-lcok map for the given cache
     */
    SoftLockManager createSoftLockManager(Ehcache cache, SoftLockFactory lockFactory);

    /**
     * Called on {@code CacheManager} creation.
     */
    void startup();

    /**
     * Called on {@code CacheManager} shutdown and on exception during CacheManager bootstrapping.
     */
    void dispose();

    /**
     * Create update checker
     *
     * @return UpdateChecker instance
     */
    UpdateChecker createUpdateChecker();
}
