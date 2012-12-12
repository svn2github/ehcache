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

import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation;
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

    public long getEvictionCount() {
        return flatCore.getEvictionCount();
    }

    public long getExpiredCount() {
        return flatCore.getExpiredCount();
    }

    public long getEvictedCount() {
        return flatCore.getEvictedCount();
    }

    public void getStatisticsTimeToDisable(long time, TimeUnit unit) {
        flatExtended.setStatisticsTimeToDisable(time, unit);
    }

    public Operation cacheGetOperation() {
        return flatExtended.cacheGetOperation();
    }

    public Operation cacheHitOperation() {
        return flatExtended.cacheHitOperation();
    }

    public Operation cacheMissExpiredOperation() {
        return flatExtended.cacheMissExpiredOperation();
    }

    public Operation cacheMissNotFoundOperation() {
        return flatExtended.cacheMissNotFoundOperation();
    }

    public Operation cacheMissOperation() {
        return flatExtended.cacheMissOperation();
    }

    public Operation cachePutAddedOperation() {
        return flatExtended.cachePutAddedOperation();
    }

    public Operation cachePutReplacedOperation() {
        return flatExtended.cachePutReplacedOperation();
    }

    public Operation cachePutOperation() {
        return flatExtended.cachePutOperation();
    }

    public Operation cacheRemoveOperation() {
        return flatExtended.cacheRemoveOperation();
    }

    public Operation localHeapHitOperation() {
        return flatExtended.localHeapHitOperation();
    }

    public Operation localHeapMissOperation() {
        return flatExtended.localHeapMissOperation();
    }

    public Operation localHeapPutAddedOperation() {
        return flatExtended.localHeapPutAddedOperation();
    }

    public Operation localHeapPutReplacedOperation() {
        return flatExtended.localHeapPutReplacedOperation();
    }

    public Operation localHeapPutOperation() {
        return flatExtended.localHeapPutOperation();
    }

    public Operation localHeapRemoveOperation() {
        return flatExtended.localHeapRemoveOperation();
    }

    public Operation localOffHeapHitOperation() {
        return flatExtended.localOffHeapHitOperation();
    }

    public Operation localOffHeapMissOperation() {
        return flatExtended.localOffHeapMissOperation();
    }

    public Operation localOffHeapPutAddedOperation() {
        return flatExtended.localOffHeapPutAddedOperation();
    }

    public Operation localOffHeapPutReplacedOperation() {
        return flatExtended.localOffHeapPutReplacedOperation();
    }

    public Operation localOffHeapPutOperation() {
        return flatExtended.localOffHeapPutOperation();
    }

    public Operation localOffHeapRemoveOperation() {
        return flatExtended.localOffHeapRemoveOperation();
    }

    public Operation diskHeapHitOperation() {
        return flatExtended.diskHeapHitOperation();
    }

    public Operation diskHeapMissOperation() {
        return flatExtended.diskHeapMissOperation();
    }

    public Operation diskHeapPutAddedOperation() {
        return flatExtended.diskHeapPutAddedOperation();
    }

    public Operation diskHeapPutReplacedOperation() {
        return flatExtended.diskHeapPutReplacedOperation();
    }

    public Operation diskHeapPutOperation() {
        return flatExtended.diskHeapPutOperation();
    }

    public Operation diskHeapRemoveOperation() {
        return flatExtended.diskHeapRemoveOperation();
    }

    public Operation cacheSearchOperation() {
        return flatExtended.cacheSearchOperation();
    }

    public Operation xaCommitSuccessOperation() {
        return flatExtended.xaCommitSuccessOperation();
    }

    public Operation xaCommitExceptionOperation() {
        return flatExtended.xaCommitExceptionOperation();
    }

    public Operation xaCommitReadOnlyOperation() {
        return flatExtended.xaCommitReadOnlyOperation();
    }

    public Operation xaRollbackOperation() {
        return flatExtended.xaRollbackOperation();
    }

    public Operation xaRollbackExceptionOperation() {
        return flatExtended.xaRollbackExceptionOperation();
    }

    public Operation xaRecoveryOperation() {
        return flatExtended.xaRecoveryOperation();
    }

    public Operation evictionOperation() {
        return flatExtended.evictionOperation();
    }

    public Operation expiredOperation() {
        return flatExtended.expiredOperation();
    }

    public long getLocalHeapSizeInBytes() {
        return flatExtended.getLocalHeapSizeInBytes();
    }

    public long calculateInMemorySize() {
        return flatExtended.calculateInMemorySize();
    }

    public long getMemoryStoreSize() {
        return flatExtended.getMemoryStoreSize();
    }

    public int getDiskStoreSize() {
        return flatExtended.getDiskStoreSize();
    }

    public long calculateOffHeapSize() {
        return flatExtended.calculateOffHeapSize();
    }

    public long getOffHeapStoreSize() {
        return flatExtended.getOffHeapStoreSize();
    }

    public long getObjectCount() {
        return flatExtended.getObjectCount();
    }

    public long getMemoryStoreObjectCount() {
        return flatExtended.getMemoryStoreObjectCount();
    }

    public long getDiskStoreObjectCount() {
        return flatExtended.getDiskStoreObjectCount();
    }

    public long getLocalHeapSize() {
        return flatExtended.getLocalHeapSize();
    }

    public long getWriterQueueSize() {
        return flatExtended.getWriterQueueSize();
    }

    public long getOffHeapStoreObjectCount() {
        return flatExtended.getOffHeapStoreObjectCount();
    }

    public String getLocalHeapSizeString() {
        return flatExtended.getLocalHeapSizeString();
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

    public long getSize() {
        return flatExtended.getSize();
    }

    @Override
    public void setStatisticsTimeToDisable(long time, TimeUnit unit) {
           flatExtended.setStatisticsTimeToDisable(time, unit);
    }

}
