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

import static org.terracotta.context.query.Matchers.allOf;
import static org.terracotta.context.query.Matchers.attributes;
import static org.terracotta.context.query.Matchers.context;
import static org.terracotta.context.query.Matchers.hasAttribute;
import static org.terracotta.context.query.Matchers.identifier;
import static org.terracotta.context.query.Matchers.subclassOf;
import static org.terracotta.context.query.QueryBuilder.queryBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
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

/**
 * The Class ExtendedStatisticsImpl.
 *
 * @author cschanck
 */
public class ExtendedStatisticsImpl implements ExtendedStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedStatisticsImpl.class);

    private final Map<StandardPassThroughStatistic, ValueStatistic<?>> standardPassThroughs =
            new EnumMap<StandardPassThroughStatistic, ValueStatistic<?>>(
                    StandardPassThroughStatistic.class);
    private final Map<StandardOperationStatistic, Operation<?>> standardOperations =
            new EnumMap<StandardOperationStatistic, Operation<?>>(
                    StandardOperationStatistic.class);
    private final ConcurrentMap<OperationStatistic<?>, CompoundOperationImpl<?>> customOperations =
            new ConcurrentHashMap<OperationStatistic<?>, CompoundOperationImpl<?>>();
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
            for (Iterator<CompoundOperationImpl<?>> it = customOperations.values().iterator(); it.hasNext();) {
                if (it.next().expire(expireThreshold)) {
                    it.remove();
                }
            }
        }
    };

    private long timeToDisable;
    private TimeUnit timeToDisableUnit;
    private ScheduledFuture disableStatus;

    private final Result allCacheGet;

    private final Result allCacheMiss;

    private final Result allCachePut;

    private final Result allHeapPut;

    private final Result allOffHeapPut;

    private final Result allDiskPut;

    /**
     * Instantiates a new extended statistics impl.
     *
     * @param manager the manager
     * @param executor the executor
     * @param timeToDisable the time to disable
     * @param unit the unit
     */
    public ExtendedStatisticsImpl(StatisticsManager manager, ScheduledExecutorService executor, long timeToDisable, TimeUnit unit) {
        this.manager = manager;
        this.executor = executor;
        this.timeToDisable = timeToDisable;
        this.timeToDisableUnit = unit;
        this.disableStatus = this.executor.scheduleAtFixedRate(disableTask, timeToDisable, timeToDisable, unit);

        findStandardPassThruStatistics(manager);

        findStandardOperationStatistics(manager, executor);

        // well known compound results.
        this.allCacheGet = get().compound(ALL_CACHE_GET_OUTCOMES);
        this.allCacheMiss = get().compound(ALL_CACHE_MISS_OUTCOMES);
        this.allCachePut = put().compound(ALL_CACHE_PUT_OUTCOMES);
        this.allHeapPut = heapPut().compound(ALL_STORE_PUT_OUTCOMES);
        this.allOffHeapPut = offheapPut().compound(ALL_STORE_PUT_OUTCOMES);
        this.allDiskPut = diskPut().compound(ALL_STORE_PUT_OUTCOMES);

    }

    private void findStandardOperationStatistics(StatisticsManager manager, ScheduledExecutorService executor) {
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
                    standardOperations.put(t,
                            new CompoundOperationImpl(source, t.type(), t.window(), TimeUnit.SECONDS, executor, t.history(), t.interval(),
                                    TimeUnit.SECONDS));
                    break;
                default:
                    throw new IllegalStateException("Duplicate statistics found for " + t);
            }
        }
    }

    private void findStandardPassThruStatistics(StatisticsManager manager) {
        for (final StandardPassThroughStatistic t : StandardPassThroughStatistic.values()) {
            Set<ValueStatistic<?>> results = findPassThroughStatistic(manager, t.statisticName(), t.tags());
            switch (results.size()) {
                case 0:
                    LOGGER.debug("Mocking Pass-Through Statistic: {}", t);
                    standardPassThroughs.put(t, ConstantValueStatistic.instance(t.absentValue()));
                    break;
                case 1:
                    ValueStatistic<?> statistic = results.iterator().next();
                    standardPassThroughs.put(t, statistic);
                    break;
                default:
                    throw new IllegalStateException("Duplicate statistics found for " + t);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#setTimeToDisable(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public synchronized void setTimeToDisable(long time, TimeUnit unit) {
        timeToDisable = time;
        timeToDisableUnit = unit;
        if (disableStatus != null) {
            disableStatus.cancel(false);
            disableStatus = executor.scheduleAtFixedRate(disableTask, timeToDisable, timeToDisable, timeToDisableUnit);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#setAlwaysOn(boolean)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#get()
     */
    @Override
    public Operation<GetOutcome> get() {
        return (Operation<GetOutcome>) standardOperations.get(StandardOperationStatistic.CACHE_GET);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#put()
     */
    @Override
    public Operation<PutOutcome> put() {
        return (Operation<PutOutcome>) standardOperations.get(StandardOperationStatistic.CACHE_PUT);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#remove()
     */
    @Override
    public Operation<RemoveOutcome> remove() {
        return (Operation<RemoveOutcome>) standardOperations.get(StandardOperationStatistic.CACHE_REMOVE);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#search()
     */
    @Override
    public Operation<SearchOutcome> search() {
        return (Operation<CacheOperationOutcomes.SearchOutcome>) standardOperations.get(StandardOperationStatistic.SEARCH);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#heapGet()
     */
    @Override
    public Operation<StoreOperationOutcomes.GetOutcome> heapGet() {
        return (Operation<StoreOperationOutcomes.GetOutcome>) standardOperations.get(StandardOperationStatistic.HEAP_GET);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#heapPut()
     */
    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome> heapPut() {
        return (Operation<StoreOperationOutcomes.PutOutcome>) standardOperations.get(StandardOperationStatistic.HEAP_PUT);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#heapRemove()
     */
    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome> heapRemove() {
        return (Operation<StoreOperationOutcomes.RemoveOutcome>) standardOperations.get(StandardOperationStatistic.HEAP_REMOVE);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#offheapGet()
     */
    @Override
    public Operation<StoreOperationOutcomes.GetOutcome> offheapGet() {
        return (Operation<StoreOperationOutcomes.GetOutcome>) standardOperations.get(StandardOperationStatistic.OFFHEAP_GET);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#offheapPut()
     */
    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome> offheapPut() {
        return (Operation<StoreOperationOutcomes.PutOutcome>) standardOperations.get(StandardOperationStatistic.OFFHEAP_PUT);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#offheapRemove()
     */
    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome> offheapRemove() {
        return (Operation<StoreOperationOutcomes.RemoveOutcome>) standardOperations.get(StandardOperationStatistic.OFFHEAP_REMOVE);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#diskGet()
     */
    @Override
    public Operation<StoreOperationOutcomes.GetOutcome> diskGet() {
        return (Operation<StoreOperationOutcomes.GetOutcome>) standardOperations.get(StandardOperationStatistic.DISK_GET);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#diskPut()
     */
    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome> diskPut() {
        return (Operation<StoreOperationOutcomes.PutOutcome>) standardOperations.get(StandardOperationStatistic.DISK_PUT);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#diskRemove()
     */
    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome> diskRemove() {
        return (Operation<StoreOperationOutcomes.RemoveOutcome>) standardOperations.get(StandardOperationStatistic.DISK_REMOVE);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#xaCommit()
     */
    @Override
    public Operation<XaCommitOutcome> xaCommit() {
        return (Operation<XaCommitOutcome>) standardOperations.get(StandardOperationStatistic.XA_COMMIT);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#xaRollback()
     */
    @Override
    public Operation<XaRollbackOutcome> xaRollback() {
        return (Operation<XaRollbackOutcome>) standardOperations.get(StandardOperationStatistic.XA_ROLLBACK);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#xaRecovery()
     */
    @Override
    public Operation<XaRecoveryOutcome> xaRecovery() {
        return (Operation<XaRecoveryOutcome>) standardOperations.get(StandardOperationStatistic.XA_RECOVERY);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#eviction()
     */
    @Override
    public Operation<EvictionOutcome> eviction() {
        return (Operation<CacheOperationOutcomes.EvictionOutcome>) standardOperations.get(StandardOperationStatistic.EVICTION);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#expiry()
     */
    @Override
    public Operation<ExpiredOutcome> expiry() {
        return (Operation<CacheOperationOutcomes.ExpiredOutcome>) standardOperations.get(StandardOperationStatistic.EXPIRY);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#allGet()
     */
    @Override
    public Result allGet() {
        return allCacheGet;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#allMiss()
     */
    @Override
    public Result allMiss() {
        return allCacheMiss;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#allPut()
     */
    @Override
    public Result allPut() {
        return allCachePut;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#heapAllPut()
     */
    @Override
    public Result heapAllPut() {
        return allHeapPut;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#offHeapAllPut()
     */
    @Override
    public Result offHeapAllPut() {
        return allOffHeapPut;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#diskAllPut()
     */
    @Override
    public Result diskAllPut() {
        return allDiskPut;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#operations(java.lang.Class, java.lang.String, java.lang.String[])
     */
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
                    operation = new CompoundOperationImpl<T>(source, source.type(), 1, TimeUnit.SECONDS, executor, 0, 1, TimeUnit.SECONDS);
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

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getLocalHeapSize()
     */
    @Override
    public ValueStatistic<Long> getLocalHeapSize() {
        return (ValueStatistic<Long>) standardPassThroughs.get(StandardPassThroughStatistic.LOCAL_HEAP_SIZE);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getLocalHeapSizeInBytes()
     */
    @Override
    public ValueStatistic<Long> getLocalHeapSizeInBytes() {
        return (ValueStatistic<Long>) standardPassThroughs.get(StandardPassThroughStatistic.LOCAL_HEAP_SIZE_BYTES);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getLocalOffHeapSize()
     */
    @Override
    public ValueStatistic<Long> getLocalOffHeapSize() {
        return (ValueStatistic<Long>) standardPassThroughs.get(StandardPassThroughStatistic.LOCAL_OFFHEAP_SIZE);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getLocalOffHeapSizeInBytes()
     */
    @Override
    public ValueStatistic<Long> getLocalOffHeapSizeInBytes() {
        return (ValueStatistic<Long>) standardPassThroughs.get(StandardPassThroughStatistic.LOCAL_OFFHEAP_SIZE_BYTES);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getLocalDiskSize()
     */
    @Override
    public ValueStatistic<Long> getLocalDiskSize() {
        return (ValueStatistic<Long>) standardPassThroughs.get(StandardPassThroughStatistic.LOCAL_DISK_SIZE);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getLocalDiskSizeInBytes()
     */
    @Override
    public ValueStatistic<Long> getLocalDiskSizeInBytes() {
        return (ValueStatistic<Long>) standardPassThroughs.get(StandardPassThroughStatistic.LOCAL_DISK_SIZE_BYTES);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getRemoteSize()
     */
    @Override
    public ValueStatistic<Long> getRemoteSize() {
        return (ValueStatistic<Long>) standardPassThroughs.get(StandardPassThroughStatistic.REMOTE_SIZE);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getSize()
     */
    @Override
    public ValueStatistic<Long> getSize() {
        return (ValueStatistic<Long>) standardPassThroughs.get(StandardPassThroughStatistic.CACHE_SIZE);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getWriterQueueLength()
     */
    @Override
    public ValueStatistic<Long> getWriterQueueLength() {
        return (ValueStatistic<Long>) standardPassThroughs.get(StandardPassThroughStatistic.WRITER_QUEUE_LENGTH);
    }

    /**
     * Find operation statistic.
     *
     * @param <T> the generic type
     * @param manager the manager
     * @param type the type
     * @param name the name
     * @param tags the tags
     * @return the sets the
     */
    private static <T extends Enum<T>> Set<OperationStatistic<T>> findOperationStatistic(StatisticsManager manager, Class<T> type,
            String name, final Set<String> tags) {
        Set<TreeNode> operationStatisticNodes = manager.query(queryBuilder().descendants()
                .filter(context(identifier(subclassOf(OperationStatistic.class)))).build());
        Set<TreeNode> result = queryBuilder()
                .filter(context(attributes(allOf(hasAttribute("type", type), hasAttribute("name", name),
                        hasAttribute("tags", new Matcher<Set<String>>() {
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
        Set<TreeNode> passThroughStatisticNodes = manager.query(queryBuilder().descendants()
                .filter(context(identifier(subclassOf(ValueStatistic.class)))).build());
        Set<TreeNode> result = queryBuilder()
                .filter(context(attributes(allOf(hasAttribute("name", name), hasAttribute("tags", new Matcher<Set<String>>() {
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
