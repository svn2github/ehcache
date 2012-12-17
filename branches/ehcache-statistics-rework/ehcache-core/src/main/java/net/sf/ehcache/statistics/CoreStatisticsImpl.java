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

import java.util.Arrays;
import java.util.EnumSet;

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.CacheOperationOutcomes.EvictionOutcome;
import net.sf.ehcache.CacheOperationOutcomes.ExpiredOutcome;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Operation;
import net.sf.ehcache.store.StoreOperationOutcomes.GetOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

public class CoreStatisticsImpl implements CoreStatistics {

    private final ExtendedStatistics extended;
    private final CountOperation cacheGet;
    private final CountOperation cachePut;
    private final CountOperation cacheRemove;
    private final CountOperation localHeapGet;
    private final CountOperation localHeapPut;
    private final CountOperation localHeapRemove;
    private final CountOperation localOffHeapGet;
    private final CountOperation localOffHeapPut;
    private final CountOperation localOffHeapRemove;
    private final CountOperation diskGet;
    private final CountOperation diskPut;
    private final CountOperation diskRemove;
    private final CountOperation xaCommit;
    private final CountOperation xaRecovery;
    private final CountOperation xaRollback;
    private final CountOperation evicted;
    private final CountOperation expired;

    public CoreStatisticsImpl(ExtendedStatistics extended) {
        this.extended = extended;
        this.cacheGet = asCountOperation(extended.get());
        this.cachePut = asCountOperation(extended.put());
        this.cacheRemove = asCountOperation(extended.remove());

        this.localHeapGet = asCountOperation(extended.heapGet());
        this.localHeapPut = asCountOperation(extended.heapPut());
        this.localHeapRemove = asCountOperation(extended.heapRemove());

        this.localOffHeapGet = asCountOperation(extended.offheapGet());
        this.localOffHeapPut = asCountOperation(extended.offheapPut());
        this.localOffHeapRemove = asCountOperation(extended.offheapRemove());

        this.diskGet = asCountOperation(extended.diskGet());
        this.diskPut = asCountOperation(extended.diskPut());
        this.diskRemove = asCountOperation(extended.diskRemove());

        this.xaCommit = asCountOperation(extended.xaCommit());
        this.xaRecovery = asCountOperation(extended.xaRecovery());
        this.xaRollback = asCountOperation(extended.xaRollback());

        this.evicted = asCountOperation(extended.eviction());
        this.expired = asCountOperation(extended.expiration());

    }

    private static <T extends Enum<T>> CountOperation asCountOperation(final Operation<T> compoundOp) {
        return new CountOperation<T>() {
            @Override
            public long value(T result) {
                return compoundOp.component(result).count().value();
            }

            @Override
            public long value(T... results) {
                return compoundOp.compound(EnumSet.copyOf(Arrays.asList(results))).count().value();
            }

        };
    }

    @Override
    public CountOperation<CacheOperationOutcomes.GetOutcome> get() {
        return cacheGet;
    }

    @Override
    public CountOperation<CacheOperationOutcomes.PutOutcome> put() {
        return cachePut;
    }

    @Override
    public CountOperation<CacheOperationOutcomes.RemoveOutcome> remove() {
        return cachePut;
    }

    @Override
    public CountOperation<GetOutcome> localHeapGet() {
        return localHeapGet;
    }

    @Override
    public CountOperation<PutOutcome> localHeapPut() {
        return localHeapPut;
    }

    @Override
    public CountOperation<RemoveOutcome> localHeapRemove() {
        return localHeapRemove;
    }

    @Override
    public CountOperation<GetOutcome> localOffHeapGet() {
        return localOffHeapGet;
    }

    @Override
    public CountOperation<PutOutcome> localOffHeapPut() {
        return localOffHeapPut;
    }

    @Override
    public CountOperation<RemoveOutcome> localOffHeapRemove() {
        return localOffHeapRemove;
    }

    @Override
    public CountOperation<GetOutcome> diskGet() {
        return diskGet;
    }

    @Override
    public CountOperation<PutOutcome> diskPut() {
        return diskPut;
    }

    @Override
    public CountOperation<RemoveOutcome> diskRemove() {
        return diskRemove;
    }

    public CountOperation<XaCommitOutcome> xaCommit() {
        return xaCommit;
    }

    @Override
    public CountOperation<XaRecoveryOutcome> xaRecovery() {
        return xaRecovery;
    }

    @Override
    public CountOperation<XaRollbackOutcome> xaRollback() {
        return xaRollback;
    }

    @Override
    public CountOperation<EvictionOutcome> cacheEviction() {
        return evicted;
    }

    @Override
    public CountOperation<ExpiredOutcome> cacheExpiration() {
        return expired;
    }

}