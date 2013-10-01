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

import java.util.concurrent.TimeUnit;

import net.sf.ehcache.statistics.extended.ExtendedStatistics.Result;

/**
 * The Interface FlatStatistics; the almighty confluence of all basic statistics.
 *
 * @author cschanck
 */
public interface FlatStatistics  {

    /**
     * Cache hit ratio
     * @return
     */
    double cacheHitRatio();
    
    /**
     * Cache hit count.
     *
     * @return the long
     */
    long cacheHitCount();

    /**
     * Cache miss expired count.
     *
     * @return the long
     */
    long cacheMissExpiredCount();

    /**
     * Cache miss not found count.
     *
     * @return the long
     */
    long cacheMissNotFoundCount();

    /**
     * Cache miss count.
     *
     * @return the long
     */
    long cacheMissCount();

    /**
     * Cache put added count.
     *
     * @return the long
     */
    long cachePutAddedCount();

    /**
     * Cache put updated count.
     *
     * @return the long
     */
    long cachePutUpdatedCount();

    /**
     * Cache put count.
     *
     * @return the long
     */
    long cachePutCount();

    /**
     * Cache remove count.
     *
     * @return the long
     */
    long cacheRemoveCount();

    /**
     * Local heap hit count.
     *
     * @return the long
     */
    long localHeapHitCount();

    /**
     * Local heap miss count.
     *
     * @return the long
     */
    long localHeapMissCount();

    /**
     * Local heap put added count.
     *
     * @return the long
     */
    long localHeapPutAddedCount();

    /**
     * Local heap put updated count.
     *
     * @return the long
     */
    long localHeapPutUpdatedCount();

    /**
     * Local heap put count.
     *
     * @return the long
     */
    long localHeapPutCount();

    /**
     * Local heap remove count.
     *
     * @return the long
     */
    long localHeapRemoveCount();

    /**
     * Local off heap hit count.
     *
     * @return the long
     */
    long localOffHeapHitCount();

    /**
     * Local off heap miss count.
     *
     * @return the long
     */
    long localOffHeapMissCount();

    /**
     * Local off heap put added count.
     *
     * @return the long
     */
    long localOffHeapPutAddedCount();

    /**
     * Local off heap put updated count.
     *
     * @return the long
     */
    long localOffHeapPutUpdatedCount();

    /**
     * Local of heap put count.
     *
     * @return the long
     */
    long localOffHeapPutCount();

    /**
     * Local off heap remove count.
     *
     * @return the long
     */
    long localOffHeapRemoveCount();

    /**
     * Local disk hit count.
     *
     * @return the long
     */
    long localDiskHitCount();

    /**
     * Local disk miss count.
     *
     * @return the long
     */
    long localDiskMissCount();

    /**
     * Local disk put added count.
     *
     * @return the long
     */
    long localDiskPutAddedCount();

    /**
     * Local disk put updated count.
     *
     * @return the long
     */
    long localDiskPutUpdatedCount();

    /**
     * Local disk put count.
     *
     * @return the long
     */
    long localDiskPutCount();

    /**
     * Local disk remove count.
     *
     * @return the long
     */
    long localDiskRemoveCount();

    /**
     * Xa commit read only count.
     *
     * @return the long
     */
    long xaCommitReadOnlyCount();

    /**
     * Xa commit exception count.
     *
     * @return the long
     */
    long xaCommitExceptionCount();

    /**
     * Xa commit committed count.
     *
     * @return the long
     */
    long xaCommitCommittedCount();

    /**
     * Xa commit count.
     *
     * @return the long
     */
    long xaCommitCount();

    /**
     * Xa recovery nothing count.
     *
     * @return the long
     */
    long xaRecoveryNothingCount();

    /**
     * Xa recovery recovered count.
     *
     * @return the long
     */
    long xaRecoveryRecoveredCount();

    /**
     * Xa recovery count.
     *
     * @return the long
     */
    long xaRecoveryCount();

    /**
     * Xa rollback exception count.
     *
     * @return the long
     */
    long xaRollbackExceptionCount();

    /**
     * Xa rollback success count.
     *
     * @return the long
     */
    long xaRollbackSuccessCount();

    /**
     * Xa rollback count.
     *
     * @return the long
     */
    long xaRollbackCount();

    /**
     * Cache expired count.
     *
     * @return the long
     */
    long cacheExpiredCount();

    /**
     * Cache evicted count.
     *
     * @return the long
     */
    long cacheEvictedCount();

    /**
     * Sets the statistics time to disable.
     *
     * @param time the time
     * @param unit the unit
     */
    void setStatisticsTimeToDisable(long time, TimeUnit unit);

    /**
     * Cache get operation.
     *
     * @return the result
     */
    Result cacheGetOperation();

    /**
     * Cache hit operation.
     *
     * @return the result
     */
    Result cacheHitOperation();

    /**
     * Cache miss expired operation.
     *
     * @return the result
     */
    Result cacheMissExpiredOperation();

    /**
     * Cache miss not found operation.
     *
     * @return the result
     */
    Result cacheMissNotFoundOperation();

    /**
     * Cache miss operation.
     *
     * @return the result
     */
    Result cacheMissOperation();

    /**
     * Cache put added operation.
     *
     * @return the result
     */
    Result cachePutAddedOperation();

    /**
     * Cache put replaced operation.
     *
     * @return the result
     */
    Result cachePutReplacedOperation();

    /**
     * Cache put operation.
     *
     * @return the result
     */
    Result cachePutOperation();

    /**
     * Cache remove operation.
     *
     * @return the result
     */
    Result cacheRemoveOperation();

    /**
     * Local heap hit operation.
     *
     * @return the result
     */
    Result localHeapHitOperation();

    /**
     * Local heap miss operation.
     *
     * @return the result
     */
    Result localHeapMissOperation();

    /**
     * Local heap put added operation.
     *
     * @return the result
     */
    Result localHeapPutAddedOperation();

    /**
     * Local heap put replaced operation.
     *
     * @return the result
     */
    Result localHeapPutReplacedOperation();

    /**
     * Local heap put operation.
     *
     * @return the result
     */
    Result localHeapPutOperation();

    /**
     * Local heap remove operation.
     *
     * @return the result
     */
    Result localHeapRemoveOperation();

    /**
     * Local off heap hit operation.
     *
     * @return the result
     */
    Result localOffHeapHitOperation();

    /**
     * Local off heap miss operation.
     *
     * @return the result
     */
    Result localOffHeapMissOperation();

    /**
     * Local off heap put added operation.
     *
     * @return the result
     */
    Result localOffHeapPutAddedOperation();

    /**
     * Local off heap put replaced operation.
     *
     * @return the result
     */
    Result localOffHeapPutReplacedOperation();

    /**
     * Local off heap put operation.
     *
     * @return the result
     */
    Result localOffHeapPutOperation();

    /**
     * Local off heap remove operation.
     *
     * @return the result
     */
    Result localOffHeapRemoveOperation();

    /**
     * Local disk hit operation.
     *
     * @return the result
     */
    Result localDiskHitOperation();

    /**
     * Local disk miss operation.
     *
     * @return the result
     */
    Result localDiskMissOperation();

    /**
     * Local disk put added operation.
     *
     * @return the result
     */
    Result localDiskPutAddedOperation();

    /**
     * Local disk put replaced operation.
     *
     * @return the result
     */
    Result localDiskPutReplacedOperation();

    /**
     * Local disk put operation.
     *
     * @return the result
     */
    Result localDiskPutOperation();

    /**
     * Local disk remove operation.
     *
     * @return the result
     */
    Result localDiskRemoveOperation();

    /**
     * Cache search operation.
     *
     * @return the result
     */
    Result cacheSearchOperation();

    /**
     * Xa commit success operation.
     *
     * @return the result
     */
    Result xaCommitSuccessOperation();

    /**
     * Xa commit exception operation.
     *
     * @return the result
     */
    Result xaCommitExceptionOperation();

    /**
     * Xa commit read only operation.
     *
     * @return the result
     */
    Result xaCommitReadOnlyOperation();

    /**
     * Xa rollback operation.
     *
     * @return the result
     */
    Result xaRollbackOperation();

    /**
     * Xa rollback exception operation.
     *
     * @return the result
     */
    Result xaRollbackExceptionOperation();

    /**
     * Xa recovery operation.
     *
     * @return the result
     */
    Result xaRecoveryOperation();

    /**
     * Cache eviction operation.
     *
     * @return the result
     */
    Result cacheEvictionOperation();

    /**
     * Cache expired operation.
     *
     * @return the result
     */
    Result cacheExpiredOperation();
    
    /**
     * Gets the size.
     *
     * @return the size
     */
    long getSize();

    /**
     * Gets the local heap size.
     *
     * @return the local heap size
     */
    long getLocalHeapSize();

    /**
     * Gets the local heap size in bytes.
     *
     * @return the local heap size in bytes
     */
    long getLocalHeapSizeInBytes();

    /**
     * Gets the local off heap size.
     *
     * @return the local off heap size
     */
    long getLocalOffHeapSize();

    /**
     * Gets the local off heap size in bytes.
     *
     * @return the local off heap size in bytes
     */
    long getLocalOffHeapSizeInBytes();

    /**
     * Gets the local disk size.
     *
     * @return the local disk size
     */
    long getLocalDiskSize();

    /**
     * Gets the local disk size in bytes.
     *
     * @return the local disk size in bytes
     */
    long getLocalDiskSizeInBytes();

    /**
     * Gets the remote size.
     *
     * @return the remote size
     */
    long getRemoteSize();

    /**
     * Gets the writer queue length.
     *
     * @return the writer queue length
     */
    long getWriterQueueLength();

}
