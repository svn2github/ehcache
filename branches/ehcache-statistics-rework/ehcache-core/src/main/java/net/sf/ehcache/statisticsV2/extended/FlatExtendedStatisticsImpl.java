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
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.store.StoreOperationOutcomes.GetOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome;

// TODO CRSS Start Here we are so fucked.
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
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation localHeapRemoveOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation localOffHeapHitOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation localOffHeapMissOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation localOffHeapPutAddedOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation localOffHeapPutReplacedOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation localOffHeapPutOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation localOffHeapRemoveOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation diskHeapHitOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation diskHeapMissOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation diskHeapPutAddedOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation diskHeapPutReplacedOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation diskHeapPutOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation diskHeapRemoveOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation cacheSearchOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation xaCommitSuccessOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation xaCommitExceptionOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation xaCommitReadOnlyOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation xaRollbackOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation xaRollbackExceptionOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation xaRecoveryOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation evictionOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Operation expiredOperation() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }


}
