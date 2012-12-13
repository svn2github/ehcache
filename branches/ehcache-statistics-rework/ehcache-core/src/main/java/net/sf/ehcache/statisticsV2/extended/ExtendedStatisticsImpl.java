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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.sf.ehcache.CacheOperationOutcomes;

import net.sf.ehcache.CacheOperationOutcomes.EvictionOutcome;
import net.sf.ehcache.CacheOperationOutcomes.ExpiredOutcome;
import net.sf.ehcache.CacheOperationOutcomes.GetOutcome;
import net.sf.ehcache.CacheOperationOutcomes.PutOutcome;
import net.sf.ehcache.CacheOperationOutcomes.RemoveOutcome;
import net.sf.ehcache.CacheOperationOutcomes.SearchOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

import org.terracotta.context.TreeNode;
import org.terracotta.statistics.ConstantValueStatistic;
import org.terracotta.statistics.OperationStatistic;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.Time;
import org.terracotta.statistics.ValueStatistic;

public class ExtendedStatisticsImpl implements ExtendedStatistics {

    private final Map<PassThroughType, ValueStatistic<?>> passThroughs = new EnumMap<PassThroughType, ValueStatistic<?>>(PassThroughType.class);
    private final Map<OperationType, CompoundOperation<?>> operations = new EnumMap<OperationType, CompoundOperation<?>>(OperationType.class);
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    private final Runnable disableTask = new Runnable() {
        @Override
        public void run() {
            for (CompoundOperation<?> o : operations.values()) {
                if (o instanceof CompoundOperationImpl<?>) {
                    ((CompoundOperationImpl<?>) o).expire(Time.absoluteTime() - timeToDisableUnit.toMillis(timeToDisable));
                }
            }
        }
    };

    private long timeToDisable;
    private TimeUnit timeToDisableUnit;
    private ScheduledFuture disableStatus;

    public ExtendedStatisticsImpl(StatisticsManager manager, long timeToDisable, TimeUnit unit) {
        this.timeToDisable = timeToDisable;
        this.timeToDisableUnit = unit;
        this.disableStatus = this.executor.scheduleAtFixedRate(disableTask, timeToDisable, timeToDisable, unit);

        for (PassThroughType t : PassThroughType.values()) {
            Set<TreeNode> result = manager.query(t.query());
            switch (result.size()) {
                case 0:
                    passThroughs.put(t, ConstantValueStatistic.instance(t.absentValue()));
                    break;
                case 1:
                    ValueStatistic<?> statistic = (ValueStatistic<?>) result.iterator().next().getContext().attributes().get("this");
                    passThroughs.put(t, statistic);
                    break;
                default:
                    throw new IllegalStateException("Duplicate statistics found for " + t);
            }
        }
        for (OperationType t : OperationType.values()) {
            Set<TreeNode> result = manager.query(t.query());
            switch (result.size()) {
                case 0:
                    operations.put(t, NullCompoundOperation.instance());
                    break;
                case 1:
                    OperationStatistic source = (OperationStatistic) result.iterator().next().getContext().attributes().get("this");
                    operations.put(t, new CompoundOperationImpl(source, t.type(), t.window(), TimeUnit.SECONDS, executor, t.history(), t.interval(), TimeUnit.SECONDS));
                    break;
                default:
                    throw new IllegalStateException("Duplicate statistics found for " + t);
            }
        }
    }

    @Override
    public synchronized void setStatisticsTimeToDisable(long time, TimeUnit unit) {
        timeToDisable = time;
        timeToDisableUnit = unit;
        if (disableStatus != null) {
            disableStatus.cancel(false);
            disableStatus = executor.scheduleAtFixedRate(disableTask, timeToDisable, timeToDisable, timeToDisableUnit);
        }
    }

    @Override
    public synchronized void setStatisticsEnabled(boolean enabled) {
        if (enabled) {
            if (disableStatus != null) {
                disableStatus.cancel(false);
                disableStatus = null;
            }
            for (CompoundOperation<?> o : operations.values()) {
                o.setAlwaysOn(true);
            }
        } else {
            if (disableStatus == null) {
                disableStatus = executor.scheduleAtFixedRate(disableTask, 0, timeToDisable, timeToDisableUnit);
            }
            for (CompoundOperation<?> o : operations.values()) {
                o.setAlwaysOn(false);
            }
        }
    }

    @Override
    public CompoundOperation<GetOutcome> get() {
        return (CompoundOperation<GetOutcome>) operations.get(OperationType.CACHE_GET);
    }

    @Override
    public CompoundOperation<PutOutcome> put() {
        return (CompoundOperation<PutOutcome>) operations.get(OperationType.CACHE_PUT);
    }

    @Override
    public CompoundOperation<RemoveOutcome> remove() {
        return (CompoundOperation<RemoveOutcome>) operations.get(OperationType.CACHE_REMOVE);
    }

    @Override
    public CompoundOperation<SearchOutcome> search() {
        return (CompoundOperation<CacheOperationOutcomes.SearchOutcome>) operations.get(OperationType.SEARCH);
    }

    @Override
    public CompoundOperation<StoreOperationOutcomes.GetOutcome> heapGet() {
        return (CompoundOperation<StoreOperationOutcomes.GetOutcome>) operations.get(OperationType.HEAP_GET);
    }

    @Override
    public CompoundOperation<net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome> heapPut() {
        return (CompoundOperation<StoreOperationOutcomes.PutOutcome>) operations.get(OperationType.HEAP_PUT);
    }

    @Override
    public CompoundOperation<net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome> heapRemove() {
        return (CompoundOperation<StoreOperationOutcomes.RemoveOutcome>) operations.get(OperationType.HEAP_REMOVE);
    }

    @Override
    public CompoundOperation<StoreOperationOutcomes.GetOutcome> offheapGet() {
        return (CompoundOperation<StoreOperationOutcomes.GetOutcome>) operations.get(OperationType.OFFHEAP_GET);
    }

    @Override
    public CompoundOperation<net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome> offheapPut() {
        return (CompoundOperation<StoreOperationOutcomes.PutOutcome>) operations.get(OperationType.OFFHEAP_PUT);
    }

    @Override
    public CompoundOperation<net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome> offheapRemove() {
        return (CompoundOperation<StoreOperationOutcomes.RemoveOutcome>) operations.get(OperationType.OFFHEAP_REMOVE);
    }

    @Override
    public CompoundOperation<StoreOperationOutcomes.GetOutcome> diskGet() {
        return (CompoundOperation<StoreOperationOutcomes.GetOutcome>) operations.get(OperationType.DISK_GET);
    }

    @Override
    public CompoundOperation<net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome> diskPut() {
        return (CompoundOperation<StoreOperationOutcomes.PutOutcome>) operations.get(OperationType.DISK_PUT);
    }

    @Override
    public CompoundOperation<net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome> diskRemove() {
        return (CompoundOperation<StoreOperationOutcomes.RemoveOutcome>) operations.get(OperationType.DISK_REMOVE);
    }

    @Override
    public CompoundOperation<XaCommitOutcome> xaCommit() {
        return (CompoundOperation<XaCommitOutcome>) operations.get(OperationType.XA_COMMIT);
    }

    @Override
    public CompoundOperation<XaRollbackOutcome> xaRollback() {
        return (CompoundOperation<XaRollbackOutcome>) operations.get(OperationType.XA_ROLLBACK);
    }

    @Override
    public CompoundOperation<XaRecoveryOutcome> xaRecovery() {
        return (CompoundOperation<XaRecoveryOutcome>) operations.get(OperationType.XA_RECOVERY);
    }

    @Override
    public CompoundOperation<EvictionOutcome> eviction() {
        return (CompoundOperation<CacheOperationOutcomes.EvictionOutcome>) operations.get(OperationType.EVICTED);
    }

    @Override
    public CompoundOperation<ExpiredOutcome> expiration() {
        return (CompoundOperation<CacheOperationOutcomes.ExpiredOutcome>) operations.get(OperationType.EXPIRED);
    }

    @Override
    public long getLocalHeapSize() {
        return ((Integer) passThroughs.get(PassThroughType.LOCAL_HEAP_SIZE).value()).longValue();
    }

    @Override
    public long getLocalHeapSizeInBytes() {
        return ((Long) passThroughs.get(PassThroughType.LOCAL_HEAP_SIZE_BYTES).value()).longValue();
    }

    @Override
    public long getLocalOffHeapSize() {
        return ((Long) passThroughs.get(PassThroughType.LOCAL_OFFHEAP_SIZE).value()).longValue();
    }

    @Override
    public long getLocalOffHeapSizeInBytes() {
        return ((Long) passThroughs.get(PassThroughType.LOCAL_OFFHEAP_SIZE_BYTES).value()).longValue();
    }

    @Override
    public long getLocalDiskSize() {
        return ((Long) passThroughs.get(PassThroughType.LOCAL_DISK_SIZE).value()).longValue();
    }

    @Override
    public long getLocalDiskSizeInBytes() {
        return ((Long) passThroughs.get(PassThroughType.LOCAL_DISK_SIZE_BYTES).value()).longValue();
    }

    @Override
    public long getSize() {
        return ((Integer) passThroughs.get(PassThroughType.CACHE_SIZE).value()).longValue();
    }

    @Override
    public long getWriterQueueSize() {
        return ((Long) passThroughs.get(PassThroughType.WRITER_QUEUE_SIZE).value()).longValue();
    }
}
