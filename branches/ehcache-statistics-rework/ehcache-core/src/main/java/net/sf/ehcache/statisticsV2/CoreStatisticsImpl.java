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

import java.util.Arrays;
import java.util.HashSet;

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.CompoundOperation;
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

    public CoreStatisticsImpl(ExtendedStatistics extended) {
        this.extended=extended;
        this.cacheGet=asCountOperation(extended.get());
        this.cachePut=asCountOperation(extended.put());
        this.cacheRemove=asCountOperation(extended.remove());

        this.localHeapGet=asCountOperation(extended.heapGet());
        this.localHeapPut=asCountOperation(extended.heapPut());
        this.localHeapRemove=asCountOperation(extended.heapRemove());

        this.localOffHeapGet=asCountOperation(extended.offheapGet());
        this.localOffHeapPut=asCountOperation(extended.offheapPut());
        this.localOffHeapRemove=asCountOperation(extended.offheapRemove());

        this.diskGet=asCountOperation(extended.diskGet());
        this.diskPut=asCountOperation(extended.diskPut());
        this.diskRemove=asCountOperation(extended.diskRemove());

    }

    private static <T> CountOperation asCountOperation(final CompoundOperation<T> compoundOp) {
        return new CountOperation<T>() {
            @Override
            public long value(T result) {
                return compoundOp.component(result).count();
            }

            @Override
            public long value(T... results) {
                return compoundOp.compound(new HashSet<T>(Arrays.asList(results))).count();
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

    @Override
    public CountOperation<XaCommitOutcome> xaCommit() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public CountOperation<XaRecoveryOutcome> xaRecovery() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public CountOperation<XaRollbackOutcome> xaRollback() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getEvictionCount() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getExpiredCount() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheMissCountExpired() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getEvictedCount() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLocalHeapSizeInBytes() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long calculateInMemorySize() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getMemoryStoreSize() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public int getDiskStoreSize() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long calculateOffHeapSize() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getOffHeapStoreSize() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getObjectCount() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getMemoryStoreObjectCount() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getDiskStoreObjectCount() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLocalHeapSize() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getWriterQueueSize() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getOffHeapStoreObjectCount() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLocalHeapSizeString() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getWriterQueueLength() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLocalDiskSize() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLocalOffHeapSize() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLocalDiskSizeInBytes() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLocalOffHeapSizeInBytes() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }

    @Override
    public long getSize() {
        // TODO Auto-generated method stub
        // return 0;
        throw new UnsupportedOperationException();
    }
}