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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.CacheOperationOutcomes.EvictionOutcome;
import net.sf.ehcache.CacheOperationOutcomes.SearchOutcome;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Result;
import net.sf.ehcache.statistics.extended.ExtendedStatisticsImpl;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.store.StoreOperationOutcomes.GetOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

import org.terracotta.statistics.StatisticsManager;

/**
 * StatisticsGateway rollup class.
 * 
 * @author cschanck
 */
public class StatisticsGateway implements FlatStatistics {

    /** The Constant DEFAULT_HISTORY_SIZE. Nuumber of history elements kept. */
    public static final int DEFAULT_HISTORY_SIZE = 30;

    /** The Constant DEFAULT_INTERVAL_SECS. Sampling interval in seconds. */
    public static final int DEFAULT_INTERVAL_SECS = 1;

    /** The Constant DEFAULT_SEARCH_INTERVAL_SECS. Sampling interval for search related stats. */
    public static final int DEFAULT_SEARCH_INTERVAL_SECS = 10;

    /** The Constant DEFAULT_WINDOW_SIZE_SECS. */
    public static final long DEFAULT_WINDOW_SIZE_SECS = 1;

    private static final int DEFAULT_TIME_TO_DISABLE_MINS = 5;

    /** The core. */
    private final CoreStatistics core;

    /** The extended statistics. */
    private final ExtendedStatisticsImpl extended;

    /** The associated cache name. */
    private final String assocCacheName;

    /**
     * Instantiates a new statistics placeholder.
     * 
     * @param ehcache the ehcache
     * @param executor the executor
     */
    public StatisticsGateway(Ehcache ehcache, ScheduledExecutorService executor) {
        StatisticsManager statsManager = new StatisticsManager();
        statsManager.root(ehcache);
        this.assocCacheName = ehcache.getName();
        ManagementRESTServiceConfiguration mRest = null;
        if (ehcache != null && ehcache.getCacheManager() != null && 
                ehcache.getCacheManager().getConfiguration() != null) {
            mRest = ehcache.getCacheManager().getConfiguration().getManagementRESTService();
        }

        this.extended = new ExtendedStatisticsImpl(statsManager, executor, DEFAULT_TIME_TO_DISABLE_MINS, TimeUnit.MINUTES,
                getProperSampleHistorySize(mRest), 
                getProperSampleIntervalSeconds(mRest), 
                getProperSampleSearchIntervalSeconds(mRest));

        this.core = new CoreStatisticsImpl(extended);
    }

    private int getProperSampleSearchIntervalSeconds(ManagementRESTServiceConfiguration mRest) {
        return mRest == null ? StatisticsGateway.DEFAULT_SEARCH_INTERVAL_SECS : mRest.getSampleSearchIntervalSeconds();
    }

    private int getProperSampleIntervalSeconds(ManagementRESTServiceConfiguration mRest) {
        return mRest == null ? StatisticsGateway.DEFAULT_INTERVAL_SECS : mRest.getSampleIntervalSeconds();
    }

    private int getProperSampleHistorySize(ManagementRESTServiceConfiguration mRest) {
        return mRest == null ? StatisticsGateway.DEFAULT_HISTORY_SIZE : mRest.getSampleHistorySize();
    }

    /**
     * Gets the core.
     * 
     * @return the core
     */
    public CoreStatistics getCore() {
        return core;
    }

    /**
     * Gets the extended.
     * 
     * @return the extended
     */
    public ExtendedStatistics getExtended() {
        return extended;
    }

    /**
     * Gets the associated cache name.
     * 
     * @return the associated cache name
     */
    public String getAssociatedCacheName() {
        return assocCacheName;
    }

    @Override
    public void setStatisticsTimeToDisable(long time, TimeUnit unit) {
        extended.setTimeToDisable(time, unit);
    }

    @Override
    public Result cacheGetOperation() {
        return extended.allGet();
    }

    @Override
    public Result cacheHitOperation() {
        return extended.get().component(CacheOperationOutcomes.GetOutcome.HIT);
    }

    @Override
    public Result cacheMissExpiredOperation() {
        return extended.get().component(CacheOperationOutcomes.GetOutcome.MISS_EXPIRED);
    }

    @Override
    public Result cacheMissNotFoundOperation() {
        return extended.get().component(CacheOperationOutcomes.GetOutcome.MISS_NOT_FOUND);
    }

    @Override
    public Result cacheMissOperation() {
        return extended.allMiss();
    }

    @Override
    public Result cachePutAddedOperation() {
        return extended.put().component(CacheOperationOutcomes.PutOutcome.ADDED);
    }

    @Override
    public Result cachePutReplacedOperation() {
        return extended.put().component(CacheOperationOutcomes.PutOutcome.UPDATED);
    }

    @Override
    public Result cachePutOperation() {
        return extended.allPut();
    }

    @Override
    public Result cacheRemoveOperation() {
        return extended.remove().component(CacheOperationOutcomes.RemoveOutcome.SUCCESS);
    }

    @Override
    public Result localHeapHitOperation() {
        return extended.heapGet().component(GetOutcome.HIT);
    }

    @Override
    public Result localHeapMissOperation() {
        return extended.heapGet().component(GetOutcome.MISS);
    }

    @Override
    public Result localHeapPutAddedOperation() {

        return extended.heapPut().component(StoreOperationOutcomes.PutOutcome.ADDED);
    }

    @Override
    public Result localHeapPutReplacedOperation() {
        return extended.heapPut().component(PutOutcome.ADDED);
    }

    @Override
    public Result localHeapPutOperation() {
        return extended.heapAllPut();
    }

    @Override
    public Result localHeapRemoveOperation() {
        return extended.heapRemove().component(RemoveOutcome.SUCCESS);
    }

    @Override
    public Result localOffHeapHitOperation() {
        return extended.offheapGet().component(GetOutcome.HIT);
    }

    @Override
    public Result localOffHeapMissOperation() {
        return extended.offheapGet().component(GetOutcome.MISS);
    }

    @Override
    public Result localOffHeapPutAddedOperation() {
        return extended.offheapPut().component(PutOutcome.ADDED);
    }

    @Override
    public Result localOffHeapPutReplacedOperation() {
        return extended.offheapPut().component(PutOutcome.UPDATED);
    }

    @Override
    public Result localOffHeapPutOperation() {
        return extended.offHeapAllPut();
    }

    @Override
    public Result localOffHeapRemoveOperation() {
        return extended.offheapRemove().component(RemoveOutcome.SUCCESS);
    }

    @Override
    public Result localDiskHitOperation() {
        return extended.diskGet().component(GetOutcome.HIT);
    }

    @Override
    public Result localDiskMissOperation() {
        return extended.diskGet().component(GetOutcome.MISS);
    }

    @Override
    public Result localDiskPutAddedOperation() {
        return extended.diskPut().component(PutOutcome.ADDED);
    }

    @Override
    public Result localDiskPutReplacedOperation() {
        return extended.diskPut().component(PutOutcome.UPDATED);
    }

    @Override
    public Result localDiskPutOperation() {
        return extended.diskAllPut();
    }

    @Override
    public Result localDiskRemoveOperation() {
        return extended.diskRemove().component(RemoveOutcome.SUCCESS);
    }

    @Override
    public Result cacheSearchOperation() {
        return extended.search().component(SearchOutcome.SUCCESS);
    }

    @Override
    public Result xaCommitSuccessOperation() {
        return extended.xaCommit().component(XaCommitOutcome.COMMITTED);
    }

    @Override
    public Result xaCommitExceptionOperation() {
        return extended.xaCommit().component(XaCommitOutcome.EXCEPTION);
    }

    @Override
    public Result xaCommitReadOnlyOperation() {
        return extended.xaCommit().component(XaCommitOutcome.READ_ONLY);
    }

    @Override
    public Result xaRollbackOperation() {
        return extended.xaRollback().component(XaRollbackOutcome.ROLLEDBACK);
    }

    @Override
    public Result xaRollbackExceptionOperation() {
        return extended.xaRollback().component(XaRollbackOutcome.EXCEPTION);
    }

    @Override
    public Result xaRecoveryOperation() {
        return extended.xaRecovery().component(XaRecoveryOutcome.RECOVERED);
    }

    @Override
    public Result cacheEvictionOperation() {
        return extended.eviction().component(EvictionOutcome.SUCCESS);
    }

    @Override
    public Result cacheExpiredOperation() {
        return extended.expiry().component(CacheOperationOutcomes.ExpiredOutcome.SUCCESS);
    }

    @Override
    public long getLocalHeapSizeInBytes() {
        return extended.localHeapSizeInBytes().value().longValue();
    }

    @Override
    public long getLocalHeapSize() {
        return extended.localHeapSize().value().longValue();
    }

    @Override
    public long getWriterQueueLength() {
        return extended.writerQueueLength().value().longValue();
    }

    @Override
    public long getLocalDiskSize() {
        return extended.localDiskSize().value().longValue();
    }

    @Override
    public long getLocalOffHeapSize() {
        return extended.localOffHeapSize().value().longValue();
    }

    @Override
    public long getLocalDiskSizeInBytes() {
        return extended.localDiskSizeInBytes().value().longValue();
    }

    @Override
    public long getLocalOffHeapSizeInBytes() {
        return extended.localOffHeapSizeInBytes().value().longValue();
    }

    @Override
    public long getRemoteSize() {
        return extended.remoteSize().value().longValue();
    }

    @Override
    public long getSize() {
        return extended.size().value().longValue();
    }

    @Override
    public long cacheHitCount() {
        return core.get().value(CacheOperationOutcomes.GetOutcome.HIT);
    }

    @Override
    public long cacheMissExpiredCount() {
        return core.get().value(CacheOperationOutcomes.GetOutcome.MISS_EXPIRED);
    }

    @Override
    public long cacheMissNotFoundCount() {
        return core.get().value(CacheOperationOutcomes.GetOutcome.MISS_NOT_FOUND);
    }

    @Override
    public long cacheMissCount() {
        return cacheMissExpiredCount() + cacheMissNotFoundCount();
    }

    @Override
    public long cachePutAddedCount() {
        return core.put().value(CacheOperationOutcomes.PutOutcome.ADDED);
    }

    @Override
    public long cachePutUpdatedCount() {
        return core.put().value(CacheOperationOutcomes.PutOutcome.UPDATED);
    }

    @Override
    public long cachePutCount() {
        return cachePutAddedCount() + cachePutUpdatedCount();
    }

    @Override
    public long cacheRemoveCount() {
        return core.remove().value(CacheOperationOutcomes.RemoveOutcome.SUCCESS);
    }

    @Override
    public long localHeapHitCount() {
        return core.localHeapGet().value(GetOutcome.HIT);
    }

    @Override
    public long localHeapMissCount() {
        return core.localHeapGet().value(GetOutcome.MISS);
    }

    @Override
    public long localHeapPutAddedCount() {
        return core.localHeapPut().value(PutOutcome.ADDED);
    }

    @Override
    public long localHeapPutUpdatedCount() {
        return core.localHeapPut().value(PutOutcome.UPDATED);
    }

    @Override
    public long localHeapPutCount() {
        return localHeapPutAddedCount() + localHeapPutUpdatedCount();
    }

    @Override
    public long localHeapRemoveCount() {
        return core.localOffHeapRemove().value(RemoveOutcome.SUCCESS);
    }

    @Override
    public long localOffHeapHitCount() {
        return core.localOffHeapGet().value(GetOutcome.HIT);
    }

    @Override
    public long localOffHeapMissCount() {
        return core.localOffHeapGet().value(GetOutcome.MISS);
    }

    @Override
    public long localOffHeapPutAddedCount() {
        return core.localOffHeapPut().value(PutOutcome.ADDED);
    }

    @Override
    public long localOffHeapPutUpdatedCount() {
        return core.localOffHeapPut().value(PutOutcome.UPDATED);
    }

    @Override
    public long localOffHeapPutCount() {
        return localOffHeapPutAddedCount() + localOffHeapPutUpdatedCount();
    }

    @Override
    public long localOffHeapRemoveCount() {
        return core.localOffHeapRemove().value(RemoveOutcome.SUCCESS);
    }

    @Override
    public long localDiskHitCount() {
        return core.localDiskGet().value(GetOutcome.HIT);
    }

    @Override
    public long localDiskMissCount() {
        return core.localDiskGet().value(GetOutcome.MISS);
    }

    @Override
    public long localDiskPutAddedCount() {
        return core.localDiskPut().value(PutOutcome.ADDED);
    }

    @Override
    public long localDiskPutUpdatedCount() {
        return core.localDiskPut().value(PutOutcome.UPDATED);
    }

    @Override
    public long localDiskPutCount() {
        return localDiskPutAddedCount() + localDiskPutUpdatedCount();
    }

    @Override
    public long localDiskRemoveCount() {
        return core.localDiskRemove().value(RemoveOutcome.SUCCESS);
    }

    @Override
    public long xaCommitReadOnlyCount() {
        return core.xaCommit().value(XaCommitOutcome.READ_ONLY);
    }

    @Override
    public long xaCommitExceptionCount() {
        return core.xaCommit().value(XaCommitOutcome.EXCEPTION);
    }

    @Override
    public long xaCommitCommittedCount() {
        return core.xaCommit().value(XaCommitOutcome.COMMITTED);
    }

    @Override
    public long xaCommitCount() {
        return xaCommitCommittedCount() + xaCommitExceptionCount() + xaCommitReadOnlyCount();
    }

    @Override
    public long xaRecoveryNothingCount() {
        return core.xaRecovery().value(XaRecoveryOutcome.NOTHING);
    }

    @Override
    public long xaRecoveryRecoveredCount() {
        return core.xaRecovery().value(XaRecoveryOutcome.RECOVERED);
    }

    @Override
    public long xaRecoveryCount() {
        return xaRecoveryNothingCount() + xaRecoveryRecoveredCount();
    }

    @Override
    public long xaRollbackExceptionCount() {
        return core.xaRollback().value(XaRollbackOutcome.EXCEPTION);
    }

    @Override
    public long xaRollbackSuccessCount() {
        return core.xaRollback().value(XaRollbackOutcome.ROLLEDBACK);
    }

    @Override
    public long xaRollbackCount() {
        return xaRollbackExceptionCount() + xaRollbackSuccessCount();
    }

    @Override
    public long cacheExpiredCount() {
        return core.cacheExpiration().value(CacheOperationOutcomes.ExpiredOutcome.SUCCESS);
    }

    @Override
    public long cacheEvictedCount() {
        return core.cacheEviction().value(CacheOperationOutcomes.EvictionOutcome.SUCCESS);
    }

    @Override
    public double cacheHitRatio() {
        return extended.cacheHitRatio().value().doubleValue();
    }

}
