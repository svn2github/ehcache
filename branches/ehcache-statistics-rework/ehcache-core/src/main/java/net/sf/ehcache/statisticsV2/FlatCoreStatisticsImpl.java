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

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.store.StoreOperationOutcomes.GetOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

public class FlatCoreStatisticsImpl implements FlatCoreStatistics {

    private final CoreStatistics core;

    public FlatCoreStatisticsImpl(CoreStatistics core) {
        this.core=core;
    }

    public CoreStatistics getCore() {
        return core;
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
        return cacheMissExpiredCount()+cacheMissNotFoundCount();
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
        return cachePutAddedCount()+cachePutUpdatedCount();
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
        return localHeapPutAddedCount()+localHeapPutUpdatedCount();
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
        return core.localOffHeapGet().value(GetOutcome.MISS);
    }

    @Override
    public long localOffHeapPutUpdatedCount() {
        return core.localOffHeapPut().value(PutOutcome.UPDATED);
    }

    @Override
    public long localOfHeapPutCount() {
        return localOffHeapPutAddedCount()+localOffHeapPutUpdatedCount();
    }

    @Override
    public long localOffHeapRemoveCount() {
        return core.localOffHeapRemove().value(RemoveOutcome.SUCCESS);
    }

    @Override
    public long diskHitCount() {
        return core.diskGet().value(GetOutcome.HIT);
    }

    @Override
    public long diskMissCount() {
        return core.diskGet().value(GetOutcome.MISS);
    }

    @Override
    public long diskPutAddedCount() {
        return core.diskPut().value(PutOutcome.ADDED);
    }

    @Override
    public long diskPutUpdatedCount() {
        return core.diskPut().value(PutOutcome.UPDATED);
    }

    @Override
    public long diskPutCount() {
        return diskPutAddedCount()+diskPutUpdatedCount();
    }

    @Override
    public long diskRemoveCount() {
        return core.diskRemove().value(RemoveOutcome.SUCCESS);
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
        return xaCommitCommittedCount()+xaCommitExceptionCount()+xaCommitReadOnlyCount();
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
        return xaRecoveryNothingCount()+xaRecoveryRecoveredCount();
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
        return xaRollbackExceptionCount()+xaRollbackSuccessCount();
    }

    @Override
    public long getEvictionCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getExpiredCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getEvictedCount() {
        // TODO Auto-generated method stub
        return 0;
    }
}