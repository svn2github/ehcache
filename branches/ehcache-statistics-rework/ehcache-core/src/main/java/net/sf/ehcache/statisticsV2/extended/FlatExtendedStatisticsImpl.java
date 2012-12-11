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

import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.CacheOperationOutcomes.SearchOutcome;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.store.StoreOperationOutcomes.GetOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

public class FlatExtendedStatisticsImpl implements FlatExtendedStatistics {

    private final ExtendedStatistics extended;

    public FlatExtendedStatisticsImpl(ExtendedStatistics extended) {
        this.extended=extended;
    }

    @Override
    public ExtendedStatistics getExtended() {
        return extended;
    }

    @Override
    public void setStatisticsTimeToDisable(long time, TimeUnit unit) {
        extended.setStatisticsTimeToDisable(time, unit);
    }

    @Override
    public Operation cacheHitOperation() {
        return extended.get().component(CacheOperationOutcomes.GetOutcome.HIT);
    }

    @Override
    public Operation cacheMissExpiredOperation() {
        return extended.get().component(CacheOperationOutcomes.GetOutcome.MISS_EXPIRED);
    }

    @Override
    public Operation cacheMissNotFoundOperation() {
        return extended.get().component(CacheOperationOutcomes.GetOutcome.MISS_NOT_FOUND);
    }

    @Override
    public Operation cacheMissOperation() {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation cachePutAddedOperation() {
        return extended.put().component(CacheOperationOutcomes.PutOutcome.ADDED);
    }

    @Override
    public Operation cachePutReplacedOperation() {
        return extended.put().component(CacheOperationOutcomes.PutOutcome.UPDATED);
    }

    @Override
    public Operation cachePutOperation() {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation cacheRemoveOperation() {
        return extended.remove().component(CacheOperationOutcomes.RemoveOutcome.SUCCESS);
    }

    @Override
    public Operation localHeapHitOperation() {
        return extended.heapGet().component(GetOutcome.HIT);
    }

    @Override
    public Operation localHeapMissOperation() {
        return extended.heapGet().component(GetOutcome.MISS);
    }

    @Override
    public Operation localHeapPutAddedOperation() {

        return extended.heapPut().component(StoreOperationOutcomes.PutOutcome.ADDED);
    }

    @Override
    public Operation localHeapPutReplacedOperation() {
        return extended.heapPut().component(PutOutcome.ADDED);
    }

    @Override
    public Operation localHeapPutOperation() {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation localHeapRemoveOperation() {
        return extended.heapRemove().component(RemoveOutcome.SUCCESS);
    }

    @Override
    public Operation localOffHeapHitOperation() {
        return extended.offheapGet().component(GetOutcome.HIT);
    }

    @Override
    public Operation localOffHeapMissOperation() {
        return extended.offheapGet().component(GetOutcome.MISS);
    }

    @Override
    public Operation localOffHeapPutAddedOperation() {
        return extended.offheapPut().component(PutOutcome.ADDED);
    }

    @Override
    public Operation localOffHeapPutReplacedOperation() {
        return extended.offheapPut().component(PutOutcome.UPDATED);
    }

    @Override
    public Operation localOffHeapPutOperation() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation localOffHeapRemoveOperation() {
        return extended.offheapRemove().component(RemoveOutcome.SUCCESS);
    }

    @Override
    public Operation diskHeapHitOperation() {
        return extended.diskGet().component(GetOutcome.HIT);
    }

    @Override
    public Operation diskHeapMissOperation() {
        return extended.diskGet().component(GetOutcome.MISS);
    }

    @Override
    public Operation diskHeapPutAddedOperation() {
        return extended.diskPut().component(PutOutcome.ADDED);
    }

    @Override
    public Operation diskHeapPutReplacedOperation() {
        return extended.diskPut().component(PutOutcome.UPDATED);
    }

    @Override
    public Operation diskHeapPutOperation() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation diskHeapRemoveOperation() {
        return extended.diskRemove().component(RemoveOutcome.SUCCESS);
    }

    @Override
    public Operation cacheSearchOperation() {
        return extended.search().component(SearchOutcome.SUCCESS);
    }

    @Override
    public Operation xaCommitSuccessOperation() {
        return extended.xaCommit().component(XaCommitOutcome.COMMITTED);
    }

    @Override
    public Operation xaCommitExceptionOperation() {
        return extended.xaCommit().component(XaCommitOutcome.EXCEPTION);
    }

    @Override
    public Operation xaCommitReadOnlyOperation() {
        return extended.xaCommit().component(XaCommitOutcome.READ_ONLY);
    }

    @Override
    public Operation xaRollbackOperation() {
        return extended.xaRollback().component(XaRollbackOutcome.ROLLEDBACK);
    }

    @Override
    public Operation xaRollbackExceptionOperation() {
        return extended.xaRollback().component(XaRollbackOutcome.EXCEPTION);
    }

    @Override
    public Operation xaRecoveryOperation() {
        return extended.xaRecovery().component(XaRecoveryOutcome.RECOVERED);
    }

    @Override
    public Operation evictionOperation() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation expiredOperation() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }


}
