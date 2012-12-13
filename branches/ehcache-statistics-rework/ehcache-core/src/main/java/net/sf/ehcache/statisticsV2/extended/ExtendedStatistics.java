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

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

import org.terracotta.statistics.archive.Timestamped;

public interface ExtendedStatistics {

    void setStatisticsTimeToDisable(long time, TimeUnit unit);
    void setStatisticsEnabled(boolean enable);

    CompoundOperation<CacheOperationOutcomes.GetOutcome> get();
    CompoundOperation<CacheOperationOutcomes.PutOutcome> put();
    CompoundOperation<CacheOperationOutcomes.RemoveOutcome> remove();

    CompoundOperation<StoreOperationOutcomes.GetOutcome> heapGet();
    CompoundOperation<StoreOperationOutcomes.GetOutcome> offheapGet();
    CompoundOperation<StoreOperationOutcomes.GetOutcome> diskGet();

    CompoundOperation<StoreOperationOutcomes.PutOutcome> heapPut();
    CompoundOperation<StoreOperationOutcomes.PutOutcome> offheapPut();
    CompoundOperation<StoreOperationOutcomes.PutOutcome> diskPut();

    CompoundOperation<StoreOperationOutcomes.RemoveOutcome> heapRemove();
    CompoundOperation<StoreOperationOutcomes.RemoveOutcome> offheapRemove();
    CompoundOperation<StoreOperationOutcomes.RemoveOutcome> diskRemove();

    CompoundOperation<CacheOperationOutcomes.SearchOutcome> search();

    CompoundOperation<XaCommitOutcome> xaCommit();
    CompoundOperation<XaRollbackOutcome> xaRollback();
    CompoundOperation<XaRecoveryOutcome> xaRecovery();

    CompoundOperation<CacheOperationOutcomes.EvictionOutcome> eviction();
    CompoundOperation<CacheOperationOutcomes.ExpiredOutcome> expiration();

    public interface CompoundOperation<T> {
        Operation component(T result);
        Operation compound(Set<T> results);
 
        void setAlwaysOn(boolean enable);
        void setWindow(long time, TimeUnit unit);
        void setHistory(int samples, long time, TimeUnit unit);
    }

    public interface Operation {
        Statistic<Long> count();
        Statistic<Double> rate();
        Latency latency();
    }

    public interface Latency {
        Statistic<Long> minimum();
        Statistic<Long> maximum();
        Statistic<Double> average();
    }

    public interface Statistic<T> {
        T value();
        List<Timestamped<T>> history();
    }

    // pass through stats
    long getLocalHeapSizeInBytes();

    long calculateInMemorySize();

    long getMemoryStoreSize();

    int getDiskStoreSize();

    long calculateOffHeapSize();

    long getOffHeapStoreSize();

    long getObjectCount();

    long getMemoryStoreObjectCount();

    long getDiskStoreObjectCount();

    long getLocalHeapSize();

    long getWriterQueueSize();

    long getOffHeapStoreObjectCount();

    String getLocalHeapSizeString();

    long getWriterQueueLength();

    long getLocalDiskSize();

    long getLocalOffHeapSize();

    long getLocalDiskSizeInBytes();

    long getLocalOffHeapSizeInBytes();

    long getSize();

}