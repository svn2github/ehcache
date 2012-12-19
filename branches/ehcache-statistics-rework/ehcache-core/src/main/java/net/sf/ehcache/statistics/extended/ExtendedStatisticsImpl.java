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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
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
import net.sf.ehcache.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.terracotta.context.TreeNode;
import org.terracotta.context.query.Matcher;
import org.terracotta.statistics.ConstantValueStatistic;
import org.terracotta.statistics.OperationStatistic;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.Time;
import org.terracotta.statistics.ValueStatistic;

import static org.terracotta.context.query.QueryBuilder.*;
import static org.terracotta.context.query.Matchers.*;

public class ExtendedStatisticsImpl implements ExtendedStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedStatisticsImpl.class);
    
    private final Map<StandardPassThroughStatistic, ValueStatistic<?>> standardPassThroughs = new EnumMap<StandardPassThroughStatistic, ValueStatistic<?>>(StandardPassThroughStatistic.class);
    private final Map<StandardOperationStatistic, Operation<?>> standardOperations = new EnumMap<StandardOperationStatistic, Operation<?>>(StandardOperationStatistic.class);
    private final ConcurrentMap<OperationStatistic<?>, CompoundOperationImpl<?>> customOperations = new ConcurrentHashMap<OperationStatistic<?>, CompoundOperationImpl<?>>();
    private final StatisticsManager manager;
    private final ScheduledExecutorService executor;
    private final Runnable disableTask = new Runnable() {
        @Override
        public void run() {
            long expireThreshold = Time.absoluteTime() - timeToDisableUnit.toMillis(timeToDisable);
            for (Operation<?> o : standardOperations.values()) {
                if (o instanceof CompoundOperationImpl<?>) {
                    ((CompoundOperationImpl<?>) o).expire(expireThreshold);
                }
            }
            for (Iterator<CompoundOperationImpl<?>> it = customOperations.values().iterator(); it.hasNext(); ) {
                if (it.next().expire(expireThreshold)) {
                    it.remove();
                }
            }
        }
    };

    private long timeToDisable;
    private TimeUnit timeToDisableUnit;
    private ScheduledFuture disableStatus;

    public ExtendedStatisticsImpl(StatisticsManager manager, ScheduledExecutorService executor, long timeToDisable, TimeUnit unit) {
        this.manager = manager;
        this.executor = executor;
        this.timeToDisable = timeToDisable;
        this.timeToDisableUnit = unit;
        this.disableStatus = this.executor.scheduleAtFixedRate(disableTask, timeToDisable, timeToDisable, unit);

        for (final StandardPassThroughStatistic t : StandardPassThroughStatistic.values()) {
            Set<ValueStatistic<?>> results = findPassThroughStatistic(manager, t.statisticName(), t.tags());
            switch (results.size()) {
                case 0:
                    LOGGER.debug("Mocking Pass-Through Statistic: {}", t);
                    standardPassThroughs.put(t, ConstantValueStatistic.instance(t.absentValue()));
                    break;
                case 1:
                    ValueStatistic<?> statistic = (ValueStatistic<?>) results.iterator().next();
                    standardPassThroughs.put(t, statistic);
                    break;
                default:
                    throw new IllegalStateException("Duplicate statistics found for " + t);
            }
        }

        for (final StandardOperationStatistic t : StandardOperationStatistic.values()) {
            Set<OperationStatistic> results = findOperationStatistic(manager, t.type(), t.operationName(), t.tags());
            switch (results.size()) {
                case 0:
                    if (t.required()) {
                        throw new IllegalStateException("Required statistic " + t + " not found");
                    } else {
                        LOGGER.debug("Mocking Operation Statistic: {}", t);
                        standardOperations.put(t, NullCompoundOperation.instance());
                    }
                    break;
                case 1:
                    OperationStatistic source = results.iterator().next();
                    standardOperations.put(t, new CompoundOperationImpl(source, t.type(), t.window(), TimeUnit.SECONDS, executor, t.history(), t.interval(), TimeUnit.SECONDS));
                    break;
                default:
                    throw new IllegalStateException("Duplicate statistics found for " + t);
            }
        }
    }
    
    @Override
    public synchronized void setTimeToDisable(long time, TimeUnit unit) {
        timeToDisable = time;
        timeToDisableUnit = unit;
        if (disableStatus != null) {
            disableStatus.cancel(false);
            disableStatus = executor.scheduleAtFixedRate(disableTask, timeToDisable, timeToDisable, timeToDisableUnit);
        }
    }

    @Override
    public synchronized void setAlwaysOn(boolean enabled) {
        if (enabled) {
            if (disableStatus != null) {
                disableStatus.cancel(false);
                disableStatus = null;
            }
            for (Operation<?> o : standardOperations.values()) {
                o.setAlwaysOn(true);
            }
        } else {
            if (disableStatus == null) {
                disableStatus = executor.scheduleAtFixedRate(disableTask, 0, timeToDisable, timeToDisableUnit);
            }
            for (Operation<?> o : standardOperations.values()) {
                o.setAlwaysOn(false);
            }
        }
    }

    @Override
    public Operation<GetOutcome> get() {
        return (Operation<GetOutcome>) standardOperations.get(StandardOperationStatistic.CACHE_GET);
    }

    @Override
    public Operation<PutOutcome> put() {
        return (Operation<PutOutcome>) standardOperations.get(StandardOperationStatistic.CACHE_PUT);
    }

    @Override
    public Operation<RemoveOutcome> remove() {
        return (Operation<RemoveOutcome>) standardOperations.get(StandardOperationStatistic.CACHE_REMOVE);
    }

    @Override
    public Operation<SearchOutcome> search() {
        return (Operation<CacheOperationOutcomes.SearchOutcome>) standardOperations.get(StandardOperationStatistic.SEARCH);
    }

    @Override
    public Operation<StoreOperationOutcomes.GetOutcome> heapGet() {
        return (Operation<StoreOperationOutcomes.GetOutcome>) standardOperations.get(StandardOperationStatistic.HEAP_GET);
    }

    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome> heapPut() {
        return (Operation<StoreOperationOutcomes.PutOutcome>) standardOperations.get(StandardOperationStatistic.HEAP_PUT);
    }

    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome> heapRemove() {
        return (Operation<StoreOperationOutcomes.RemoveOutcome>) standardOperations.get(StandardOperationStatistic.HEAP_REMOVE);
    }

    @Override
    public Operation<StoreOperationOutcomes.GetOutcome> offheapGet() {
        return (Operation<StoreOperationOutcomes.GetOutcome>) standardOperations.get(StandardOperationStatistic.OFFHEAP_GET);
    }

    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome> offheapPut() {
        return (Operation<StoreOperationOutcomes.PutOutcome>) standardOperations.get(StandardOperationStatistic.OFFHEAP_PUT);
    }

    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome> offheapRemove() {
        return (Operation<StoreOperationOutcomes.RemoveOutcome>) standardOperations.get(StandardOperationStatistic.OFFHEAP_REMOVE);
    }

    @Override
    public Operation<StoreOperationOutcomes.GetOutcome> diskGet() {
        return (Operation<StoreOperationOutcomes.GetOutcome>) standardOperations.get(StandardOperationStatistic.DISK_GET);
    }

    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome> diskPut() {
        return (Operation<StoreOperationOutcomes.PutOutcome>) standardOperations.get(StandardOperationStatistic.DISK_PUT);
    }

    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome> diskRemove() {
        return (Operation<StoreOperationOutcomes.RemoveOutcome>) standardOperations.get(StandardOperationStatistic.DISK_REMOVE);
    }

    @Override
    public Operation<XaCommitOutcome> xaCommit() {
        return (Operation<XaCommitOutcome>) standardOperations.get(StandardOperationStatistic.XA_COMMIT);
    }

    @Override
    public Operation<XaRollbackOutcome> xaRollback() {
        return (Operation<XaRollbackOutcome>) standardOperations.get(StandardOperationStatistic.XA_ROLLBACK);
    }

    @Override
    public Operation<XaRecoveryOutcome> xaRecovery() {
        return (Operation<XaRecoveryOutcome>) standardOperations.get(StandardOperationStatistic.XA_RECOVERY);
    }

    @Override
    public Operation<EvictionOutcome> eviction() {
        return (Operation<CacheOperationOutcomes.EvictionOutcome>) standardOperations.get(StandardOperationStatistic.EVICTION);
    }

    @Override
    public Operation<ExpiredOutcome> expiry() {
        return (Operation<CacheOperationOutcomes.ExpiredOutcome>) standardOperations.get(StandardOperationStatistic.EXPIRY);
    }

    @Override
    public <T extends Enum<T>> Set<Operation<T>> operations(Class<T> outcome, String name, String... tags) {
        Set<OperationStatistic<T>> sources = findOperationStatistic(manager, outcome, name, new HashSet<String>(Arrays.asList(tags)));
        if (sources.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<Operation<T>> operations = new HashSet();
            for (OperationStatistic<T> source : sources) {
                CompoundOperationImpl<T> operation = (CompoundOperationImpl<T>) customOperations.get(source);
                if (operation == null) {
                    operation = new CompoundOperationImpl<T>(source, source.type, 1, TimeUnit.SECONDS, executor, 0, 1, TimeUnit.SECONDS);
                    CompoundOperationImpl<T> racer = (CompoundOperationImpl<T>) customOperations.putIfAbsent(source, operation);
                    if (racer != null) {
                        operation = racer;
                    }
                }
                operations.add(operation);
            }
            return operations;
        }
    }
    
    @Override
    public long getLocalHeapSize() {
        return ((Integer) standardPassThroughs.get(StandardPassThroughStatistic.LOCAL_HEAP_SIZE).value()).longValue();
    }

    @Override
    public long getLocalHeapSizeInBytes() {
        return ((Long) standardPassThroughs.get(StandardPassThroughStatistic.LOCAL_HEAP_SIZE_BYTES).value()).longValue();
    }

    @Override
    public long getLocalOffHeapSize() {
        return ((Long) standardPassThroughs.get(StandardPassThroughStatistic.LOCAL_OFFHEAP_SIZE).value()).longValue();
    }

    @Override
    public long getLocalOffHeapSizeInBytes() {
        return ((Long) standardPassThroughs.get(StandardPassThroughStatistic.LOCAL_OFFHEAP_SIZE_BYTES).value()).longValue();
    }

    @Override
    public long getLocalDiskSize() {
        return ((Integer) standardPassThroughs.get(StandardPassThroughStatistic.LOCAL_DISK_SIZE).value()).longValue();
    }

    @Override
    public long getLocalDiskSizeInBytes() {
        return ((Long) standardPassThroughs.get(StandardPassThroughStatistic.LOCAL_DISK_SIZE_BYTES).value()).longValue();
    }

    @Override
    public long getRemoteSize() {
        return ((Long) standardPassThroughs.get(StandardPassThroughStatistic.REMOTE_SIZE).value()).longValue();
    }

    @Override
    public long getSize() {
        return ((Integer) standardPassThroughs.get(StandardPassThroughStatistic.CACHE_SIZE).value()).longValue();
    }

    @Override
    public long getWriterQueueLength() {
        return ((Long) standardPassThroughs.get(StandardPassThroughStatistic.WRITER_QUEUE_LENGTH).value()).longValue();
    }

    private static <T extends Enum<T>> Set<OperationStatistic<T>> findOperationStatistic(StatisticsManager manager, Class<T> type, String name, final Set<String> tags) {
        Set<TreeNode> operationStatisticNodes = manager.query(queryBuilder().descendants().filter(context(identifier(subclassOf(OperationStatistic.class)))).build());
        Set<TreeNode> result = queryBuilder().filter(context(attributes(allOf(hasAttribute("name", name), hasAttribute("tags", new Matcher<Set<String>>() {
            @Override
            protected boolean matchesSafely(Set<String> object) {
                return object.containsAll(tags);
            }
        }))))).build().execute(operationStatisticNodes);
        
        if (result.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<OperationStatistic<T>> statistics = new HashSet<OperationStatistic<T>>();
            for (TreeNode node : result) {
                statistics.add((OperationStatistic<T>) node.getContext().attributes().get("this"));
            }
            return statistics;
        }
    }

    private static Set<ValueStatistic<?>> findPassThroughStatistic(StatisticsManager manager, String name, final Set<String> tags) {
        Set<TreeNode> passThroughStatisticNodes = manager.query(queryBuilder().descendants().filter(context(identifier(subclassOf(ValueStatistic.class)))).build());
        Set<TreeNode> result = queryBuilder().filter(context(attributes(allOf(hasAttribute("name", name), hasAttribute("tags", new Matcher<Set<String>>() {
            @Override
            protected boolean matchesSafely(Set<String> object) {
                return object.containsAll(tags);
            }
        }))))).build().execute(passThroughStatisticNodes);
        
        if (result.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<ValueStatistic<?>> statistics = new HashSet<ValueStatistic<?>>();
            for (TreeNode node : result) {
                statistics.add((ValueStatistic<?>) node.getContext().attributes().get("this"));
            }
            return statistics;
        }
    }
}
