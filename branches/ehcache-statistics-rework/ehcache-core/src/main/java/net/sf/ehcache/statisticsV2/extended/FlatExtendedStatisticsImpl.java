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

package net.sf.ehcache.statisticsV2.extended;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.CacheOperationOutcomes.EvictionOutcome;
import net.sf.ehcache.CacheOperationOutcomes.SearchOutcome;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Result;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.store.StoreOperationOutcomes.GetOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

public class FlatExtendedStatisticsImpl implements FlatExtendedStatistics {

    private final static Set<CacheOperationOutcomes.PutOutcome> ALL_CACHE_PUT_OUTCOMES = EnumSet.allOf(CacheOperationOutcomes.PutOutcome.class);
    private final static Set<CacheOperationOutcomes.GetOutcome> ALL_CACHE_GET_OUTCOMES = EnumSet.allOf(CacheOperationOutcomes.GetOutcome.class);
    private final static Set<CacheOperationOutcomes.GetOutcome> ALL_CACHE_MISS_OUTCOMES = EnumSet.of(CacheOperationOutcomes.GetOutcome.MISS_EXPIRED, CacheOperationOutcomes.GetOutcome.MISS_NOT_FOUND);
    private final static Set<StoreOperationOutcomes.PutOutcome> ALL_STORE_PUT_OUTCOMES = EnumSet.allOf(StoreOperationOutcomes.PutOutcome.class);

    private final ExtendedStatistics extended;

    public FlatExtendedStatisticsImpl(ExtendedStatistics extended) {
        this.extended=extended;
    }

    @Override
    public void setStatisticsTimeToDisable(long time, TimeUnit unit) {
        extended.setTimeToDisable(time, unit);
    }

    @Override
    public Result cacheGetOperation() {
        return extended.get().compound(ALL_CACHE_GET_OUTCOMES);
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
        return extended.get().compound(ALL_CACHE_MISS_OUTCOMES);
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
        return extended.put().compound(ALL_CACHE_PUT_OUTCOMES);
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
        return extended.heapPut().compound(ALL_STORE_PUT_OUTCOMES);
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
        return extended.offheapPut().compound(ALL_STORE_PUT_OUTCOMES);
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
        return extended.diskPut().compound(ALL_STORE_PUT_OUTCOMES);
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
        return extended.expiration().component(CacheOperationOutcomes.ExpiredOutcome.SUCCESS);
    }

    @Override
    public long getLocalHeapSizeInBytes() {
        return extended.getLocalHeapSizeInBytes();
    }

    @Override
    public long getLocalHeapSize() {
        return extended.getLocalHeapSize();
    }

    @Override
    public long getWriterQueueLength() {
        return extended.getWriterQueueLength();
    }

    @Override
    public long getLocalDiskSize() {
        return extended.getLocalDiskSize();
    }

    @Override
    public long getLocalOffHeapSize() {
        return extended.getLocalHeapSize();
    }

    @Override
    public long getLocalDiskSizeInBytes() {
        return extended.getLocalDiskSizeInBytes();
    }

    @Override
    public long getLocalOffHeapSizeInBytes() {
        return extended.getLocalOffHeapSizeInBytes();
    }

    @Override
    public long getSize() {
        return extended.getSize();
    }
}
