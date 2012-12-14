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

package net.sf.ehcache.statisticsV2;

import java.util.concurrent.TimeUnit;

import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Result;
import net.sf.ehcache.statisticsV2.extended.FlatExtendedStatistics;

public class DelegateFlatStatistics implements FlatStatistics {

    private  FlatCoreStatistics flatCore;
    private  FlatExtendedStatistics flatExtended;

    public DelegateFlatStatistics() {
    }

    public DelegateFlatStatistics(FlatCoreStatistics flatCore,FlatExtendedStatistics flatExtended) {
        this.flatCore=flatCore;
        this.flatExtended=flatExtended;
    }

    public FlatCoreStatistics getFlatCore() {
        return flatCore;
    }

    public FlatExtendedStatistics getFlatExtended() {
        return flatExtended;
    }

    public void init(FlatCoreStatistics flatCore,FlatExtendedStatistics flatExtended) {
        this.flatCore = flatCore;
        this.flatExtended = flatExtended;
    }

    public long cacheHitCount() {
        return flatCore.cacheHitCount();
    }

    public long cacheMissExpiredCount() {
        return flatCore.cacheMissExpiredCount();
    }

    public long cacheMissNotFoundCount() {
        return flatCore.cacheMissNotFoundCount();
    }

    public long cacheMissCount() {
        return flatCore.cacheMissCount();
    }

    public long cachePutAddedCount() {
        return flatCore.cachePutAddedCount();
    }

    public long cachePutUpdatedCount() {
        return flatCore.cachePutUpdatedCount();
    }

    public long cachePutCount() {
        return flatCore.cachePutCount();
    }

    public long cacheRemoveCount() {
        return flatCore.cacheRemoveCount();
    }

    public long localHeapHitCount() {
        return flatCore.localHeapHitCount();
    }

    public long localHeapMissCount() {
        return flatCore.localHeapMissCount();
    }

    public long localHeapPutAddedCount() {
        return flatCore.localHeapPutAddedCount();
    }

    public long localHeapPutUpdatedCount() {
        return flatCore.localHeapPutUpdatedCount();
    }

    public long localHeapPutCount() {
        return flatCore.localHeapPutCount();
    }

    public long localHeapRemoveCount() {
        return flatCore.localHeapRemoveCount();
    }

    public long localOffHeapHitCount() {
        return flatCore.localOffHeapHitCount();
    }

    public long localOffHeapMissCount() {
        return flatCore.localOffHeapMissCount();
    }

    public long localOffHeapPutAddedCount() {
        return flatCore.localOffHeapPutAddedCount();
    }

    public long localOffHeapPutUpdatedCount() {
        return flatCore.localOffHeapPutUpdatedCount();
    }

    public long localOfHeapPutCount() {
        return flatCore.localOfHeapPutCount();
    }

    public long localOffHeapRemoveCount() {
        return flatCore.localOffHeapRemoveCount();
    }

    public long diskHitCount() {
        return flatCore.diskHitCount();
    }

    public long diskMissCount() {
        return flatCore.diskMissCount();
    }

    public long diskPutAddedCount() {
        return flatCore.diskPutAddedCount();
    }

    public long diskPutUpdatedCount() {
        return flatCore.diskPutUpdatedCount();
    }

    public long diskPutCount() {
        return flatCore.diskPutCount();
    }

    public long diskRemoveCount() {
        return flatCore.diskRemoveCount();
    }

    public long xaCommitReadOnlyCount() {
        return flatCore.xaCommitReadOnlyCount();
    }

    public long xaCommitExceptionCount() {
        return flatCore.xaCommitExceptionCount();
    }

    public long xaCommitCommittedCount() {
        return flatCore.xaCommitCommittedCount();
    }

    public long xaCommitCount() {
        return flatCore.xaCommitCount();
    }

    public long xaRecoveryNothingCount() {
        return flatCore.xaRecoveryNothingCount();
    }

    public long xaRecoveryRecoveredCount() {
        return flatCore.xaRecoveryRecoveredCount();
    }

    public long xaRecoveryCount() {
        return flatCore.xaRecoveryCount();
    }

    public long xaRollbackExceptionCount() {
        return flatCore.xaRollbackExceptionCount();
    }

    public long xaRollbackSuccessCount() {
        return flatCore.xaRollbackSuccessCount();
    }

    public long xaRollbackCount() {
        return flatCore.xaRollbackCount();
    }

    public void getStatisticsTimeToDisable(long time, TimeUnit unit) {
        flatExtended.setStatisticsTimeToDisable(time, unit);
    }

    public Result cacheGetOperation() {
        return flatExtended.cacheGetOperation();
    }

    public Result cacheHitOperation() {
        return flatExtended.cacheHitOperation();
    }

    public Result cacheMissExpiredOperation() {
        return flatExtended.cacheMissExpiredOperation();
    }

    public Result cacheMissNotFoundOperation() {
        return flatExtended.cacheMissNotFoundOperation();
    }

    public Result cacheMissOperation() {
        return flatExtended.cacheMissOperation();
    }

    public Result cachePutAddedOperation() {
        return flatExtended.cachePutAddedOperation();
    }

    public Result cachePutReplacedOperation() {
        return flatExtended.cachePutReplacedOperation();
    }

    public Result cachePutOperation() {
        return flatExtended.cachePutOperation();
    }

    public Result cacheRemoveOperation() {
        return flatExtended.cacheRemoveOperation();
    }

    public Result localHeapHitOperation() {
        return flatExtended.localHeapHitOperation();
    }

    public Result localHeapMissOperation() {
        return flatExtended.localHeapMissOperation();
    }

    public Result localHeapPutAddedOperation() {
        return flatExtended.localHeapPutAddedOperation();
    }

    public Result localHeapPutReplacedOperation() {
        return flatExtended.localHeapPutReplacedOperation();
    }

    public Result localHeapPutOperation() {
        return flatExtended.localHeapPutOperation();
    }

    public Result localHeapRemoveOperation() {
        return flatExtended.localHeapRemoveOperation();
    }

    public Result localOffHeapHitOperation() {
        return flatExtended.localOffHeapHitOperation();
    }

    public Result localOffHeapMissOperation() {
        return flatExtended.localOffHeapMissOperation();
    }

    public Result localOffHeapPutAddedOperation() {
        return flatExtended.localOffHeapPutAddedOperation();
    }

    public Result localOffHeapPutReplacedOperation() {
        return flatExtended.localOffHeapPutReplacedOperation();
    }

    public Result localOffHeapPutOperation() {
        return flatExtended.localOffHeapPutOperation();
    }

    public Result localOffHeapRemoveOperation() {
        return flatExtended.localOffHeapRemoveOperation();
    }

    public Result localDiskHitOperation() {
        return flatExtended.localDiskHitOperation();
    }

    public Result localDiskMissOperation() {
        return flatExtended.localDiskMissOperation();
    }

    public Result localDiskPutAddedOperation() {
        return flatExtended.localDiskPutAddedOperation();
    }

    public Result localDiskPutReplacedOperation() {
        return flatExtended.localDiskPutReplacedOperation();
    }

    public Result localDiskPutOperation() {
        return flatExtended.localDiskPutOperation();
    }

    public Result localDiskRemoveOperation() {
        return flatExtended.localDiskRemoveOperation();
    }

    public Result cacheSearchOperation() {
        return flatExtended.cacheSearchOperation();
    }

    public Result xaCommitSuccessOperation() {
        return flatExtended.xaCommitSuccessOperation();
    }

    public Result xaCommitExceptionOperation() {
        return flatExtended.xaCommitExceptionOperation();
    }

    public Result xaCommitReadOnlyOperation() {
        return flatExtended.xaCommitReadOnlyOperation();
    }

    public Result xaRollbackOperation() {
        return flatExtended.xaRollbackOperation();
    }

    public Result xaRollbackExceptionOperation() {
        return flatExtended.xaRollbackExceptionOperation();
    }

    public Result xaRecoveryOperation() {
        return flatExtended.xaRecoveryOperation();
    }

    public long getLocalHeapSizeInBytes() {
        return flatExtended.getLocalHeapSizeInBytes();
    }

    public long getLocalHeapSize() {
        return flatExtended.getLocalHeapSize();
    }

    public long getWriterQueueLength() {
        return flatExtended.getWriterQueueLength();
    }

    public long getLocalDiskSize() {
        return flatExtended.getLocalDiskSize();
    }

    public long getLocalOffHeapSize() {
        return flatExtended.getLocalOffHeapSize();
    }

    public long getLocalDiskSizeInBytes() {
        return flatExtended.getLocalDiskSizeInBytes();
    }

    public long getLocalOffHeapSizeInBytes() {
        return flatExtended.getLocalOffHeapSizeInBytes();
    }
    
    public long getRemoteSize() {
        return flatExtended.getRemoteSize();
    }

    public long getSize() {
        return flatExtended.getSize();
    }

    @Override
    public void setStatisticsTimeToDisable(long time, TimeUnit unit) {
           flatExtended.setStatisticsTimeToDisable(time, unit);
    }

    @Override
    public long cacheExpiredCount() {
        return flatExtended.cacheExpiredOperation().count().value();
    }

    @Override
    public long cacheEvictedCount() {
        return flatExtended.cacheEvictionOperation().count().value();
    }

    @Override
    public Result cacheEvictionOperation() {
        return flatExtended.cacheEvictionOperation();
    }

    @Override
    public Result cacheExpiredOperation() {
        return flatExtended.cacheExpiredOperation();
    }

}
