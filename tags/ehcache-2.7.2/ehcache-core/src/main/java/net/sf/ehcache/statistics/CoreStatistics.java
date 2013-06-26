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

package net.sf.ehcache.statistics;

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.statistics.CoreStatistics.CountOperation;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

/**
 * The CoreStatistics interface.
 *
 * @author cschanck
 */
public interface CoreStatistics {

    /**
     * The Interface CountOperation.
     *
     * @param <T> the generic type
     */
    public interface CountOperation<T> {

        /**
         * Value.
         *
         * @param result the result
         * @return the long
         */
        long value(T result);

        /**
         * Value.
         *
         * @param results the results
         * @return the long
         */
        long value(T... results);
    }

    /**
     * Gets the.
     *
     * @return the count operation
     */
    public CountOperation<CacheOperationOutcomes.GetOutcome> get();

    /**
     * Put.
     *
     * @return the count operation
     */
    public CountOperation<CacheOperationOutcomes.PutOutcome> put();

    /**
     * Removes the.
     *
     * @return the count operation
     */
    public CountOperation<CacheOperationOutcomes.RemoveOutcome> remove();

    /**
     * Local heap get.
     *
     * @return the count operation
     */
    public CountOperation<StoreOperationOutcomes.GetOutcome> localHeapGet();

    /**
     * Local heap put.
     *
     * @return the count operation
     */
    public CountOperation<StoreOperationOutcomes.PutOutcome> localHeapPut();

    /**
     * Local heap remove.
     *
     * @return the count operation
     */
    public CountOperation<StoreOperationOutcomes.RemoveOutcome> localHeapRemove();

    /**
     * Local off heap get.
     *
     * @return the count operation
     */
    public CountOperation<StoreOperationOutcomes.GetOutcome> localOffHeapGet();

    /**
     * Local off heap put.
     *
     * @return the count operation
     */
    public CountOperation<StoreOperationOutcomes.PutOutcome> localOffHeapPut();

    /**
     * Local off heap remove.
     *
     * @return the count operation
     */
    public CountOperation<StoreOperationOutcomes.RemoveOutcome> localOffHeapRemove();

    /**
     * Local disk get.
     *
     * @return the count operation
     */
    public CountOperation<StoreOperationOutcomes.GetOutcome> localDiskGet();

    /**
     * Local disk put.
     *
     * @return the count operation
     */
    public CountOperation<StoreOperationOutcomes.PutOutcome> localDiskPut();

    /**
     * Local disk remove.
     *
     * @return the count operation
     */
    public CountOperation<StoreOperationOutcomes.RemoveOutcome> localDiskRemove();

    /**
     * Xa commit.
     *
     * @return the count operation
     */
    public CountOperation<XaCommitOutcome> xaCommit();

    /**
     * Xa recovery.
     *
     * @return the count operation
     */
    public CountOperation<XaRecoveryOutcome> xaRecovery();

    /**
     * Xa rollback.
     *
     * @return the count operation
     */
    public CountOperation<XaRollbackOutcome> xaRollback();

    /**
     * Cache eviction.
     *
     * @return the count operation
     */
    public CountOperation<CacheOperationOutcomes.EvictionOutcome> cacheEviction();

    /**
     * Cache expiration.
     *
     * @return the count operation
     */
    public CountOperation<CacheOperationOutcomes.ExpiredOutcome> cacheExpiration();

    /**
     * Cache cluster event.
     * 
     * @return the count operation
     */
    public CountOperation<CacheOperationOutcomes.ClusterEventOutcomes> cacheClusterEvent();

}