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

package net.sf.ehcache.statistics.extended;

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

    void setTimeToDisable(long time, TimeUnit unit);
    void setAlwaysOn(boolean alwaysOn);

    Operation<CacheOperationOutcomes.GetOutcome> get();
    Operation<CacheOperationOutcomes.PutOutcome> put();
    Operation<CacheOperationOutcomes.RemoveOutcome> remove();

    Operation<StoreOperationOutcomes.GetOutcome> heapGet();
    Operation<StoreOperationOutcomes.GetOutcome> offheapGet();
    Operation<StoreOperationOutcomes.GetOutcome> diskGet();

    Operation<StoreOperationOutcomes.PutOutcome> heapPut();
    Operation<StoreOperationOutcomes.PutOutcome> offheapPut();
    Operation<StoreOperationOutcomes.PutOutcome> diskPut();

    Operation<StoreOperationOutcomes.RemoveOutcome> heapRemove();
    Operation<StoreOperationOutcomes.RemoveOutcome> offheapRemove();
    Operation<StoreOperationOutcomes.RemoveOutcome> diskRemove();

    Operation<CacheOperationOutcomes.SearchOutcome> search();

    Operation<XaCommitOutcome> xaCommit();
    Operation<XaRollbackOutcome> xaRollback();
    Operation<XaRecoveryOutcome> xaRecovery();

    Operation<CacheOperationOutcomes.EvictionOutcome> eviction();
    Operation<CacheOperationOutcomes.ExpiredOutcome> expiration();

    <T extends Enum<T>> Set<Operation<T>> operations(Class<T> outcome, String name, String ... tags);

    public interface Operation<T extends Enum<T>> {
        Result component(T result);
        Result compound(Set<T> results);

        Statistic<Double> ratioOf(Set<T> numerator, Set<T> denomiator);

        void setAlwaysOn(boolean enable);
        void setWindow(long time, TimeUnit unit);
        void setHistory(int samples, long time, TimeUnit unit);
    }

    public interface Result {
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
        boolean active();
        
        T value();
        List<Timestamped<T>> history();
    }

    // pass through stats
    long getSize();

    long getLocalHeapSize();

    long getLocalHeapSizeInBytes();

    long getLocalOffHeapSize();

    long getLocalOffHeapSizeInBytes();

    long getLocalDiskSize();

    long getLocalDiskSizeInBytes();
    
    long getRemoteSize();

    long getWriterQueueLength();
}