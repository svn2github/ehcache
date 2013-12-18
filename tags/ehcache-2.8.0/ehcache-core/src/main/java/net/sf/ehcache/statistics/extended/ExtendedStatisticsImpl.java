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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.terracotta.context.query.Matchers.attributes;
import static org.terracotta.context.query.Matchers.context;
import static org.terracotta.context.query.Matchers.hasAttribute;
import static org.terracotta.context.query.Matchers.identifier;
import static org.terracotta.context.query.Matchers.subclassOf;
import static org.terracotta.context.query.QueryBuilder.queryBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.CacheOperationOutcomes.ClusterEventOutcomes;
import net.sf.ehcache.CacheOperationOutcomes.EvictionOutcome;
import net.sf.ehcache.CacheOperationOutcomes.ExpiredOutcome;
import net.sf.ehcache.CacheOperationOutcomes.GetOutcome;
import net.sf.ehcache.CacheOperationOutcomes.NonStopOperationOutcomes;
import net.sf.ehcache.CacheOperationOutcomes.PutOutcome;
import net.sf.ehcache.CacheOperationOutcomes.RemoveOutcome;
import net.sf.ehcache.CacheOperationOutcomes.SearchOutcome;
import net.sf.ehcache.statistics.StatisticsGateway;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;
import net.sf.ehcache.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.context.TreeNode;
import org.terracotta.context.query.Matcher;
import org.terracotta.context.query.Matchers;
import org.terracotta.context.query.Query;
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

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedStatisticsImpl.class);

    /** The standard pass throughs. */
    private final ConcurrentMap<StandardPassThroughStatistic, Statistic<Number>> standardPassThroughs = 
            new ConcurrentHashMap<StandardPassThroughStatistic, Statistic<Number>>();

    /** The standard operations. */
    private final ConcurrentMap<StandardOperationStatistic, Operation<?>> standardOperations = 
            new ConcurrentHashMap<StandardOperationStatistic, Operation<?>>();

    /** The custom operations. */
    private final ConcurrentMap<OperationStatistic<?>, CompoundOperationImpl<?>> customOperations = 
            new ConcurrentHashMap<OperationStatistic<?>, CompoundOperationImpl<?>>();

    /** custom pass thru stats*/
    private final ConcurrentHashMap<Collection<String>, Set<Statistic<Number>>> customPassthrus =
        new ConcurrentHashMap<Collection<String>, Set<Statistic<Number>>>();

    /** The manager. */
    private final StatisticsManager manager;

    /** The executor. */
    private final ScheduledExecutorService executor;

    /** The disable task. */
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

    /** The time to disable. */
    private long timeToDisable;

    /** The time to disable unit. */
    private TimeUnit timeToDisableUnit;

    /** The disable status. */
    private ScheduledFuture disableStatus;

    /** The all cache get. */
    private final Result allCacheGet;

    /** The all cache miss. */
    private final Result allCacheMiss;

    /** The all cache put. */
    private final Result allCachePut;

    /** The all heap put. */
    private final Result allHeapPut;

    /** The all off heap put. */
    private final Result allOffHeapPut;

    /** The all disk put. */
    private final Result allDiskPut;

    /** The cache hit ratio. */
    private Statistic<Double> cacheHitRatio;

    /** on stop timeout ratio */
    private Statistic<Double> nonStopTimeoutRatio;

    private final int defaultHistorySize;

    private final long defaultIntervalSeconds;

    private final long defaultSearchIntervalSeconds;

    /**
     * Instantiates a new extended statistics impl.
     * 
     * @param manager the manager
     * @param executor the executor
     * @param timeToDisable the time to disable
     * @param unit the unit
     */
    public ExtendedStatisticsImpl(StatisticsManager manager, ScheduledExecutorService executor, long timeToDisable, TimeUnit unit,
            int defaultHistorySize, long defaultIntervalSeconds, long defaultSearchIntervalSeconds) {
        this.manager = manager;
        this.executor = executor;
        this.timeToDisable = timeToDisable;
        this.timeToDisableUnit = unit;
        this.defaultHistorySize = defaultHistorySize;
        this.defaultIntervalSeconds = defaultIntervalSeconds;
        this.defaultSearchIntervalSeconds = defaultSearchIntervalSeconds;

        this.disableStatus = this.executor.scheduleAtFixedRate(disableTask, timeToDisable, timeToDisable, unit);

        findStandardPassThruStatistics();
        findStandardOperationStatistics();

        // well known compound results.
        this.allCacheGet = get().compound(ALL_CACHE_GET_OUTCOMES);
        this.allCacheMiss = get().compound(ALL_CACHE_MISS_OUTCOMES);
        this.allCachePut = put().compound(ALL_CACHE_PUT_OUTCOMES);
        this.allHeapPut = heapPut().compound(ALL_STORE_PUT_OUTCOMES);
        this.allOffHeapPut = offheapPut().compound(ALL_STORE_PUT_OUTCOMES);
        this.allDiskPut = diskPut().compound(ALL_STORE_PUT_OUTCOMES);

        this.cacheHitRatio = get().ratioOf(EnumSet.of(CacheOperationOutcomes.GetOutcome.HIT),
                EnumSet.allOf(CacheOperationOutcomes.GetOutcome.class));
        this.nonStopTimeoutRatio = nonstop().ratioOf(
                EnumSet.of(CacheOperationOutcomes.NonStopOperationOutcomes.REJOIN_TIMEOUT,
                        CacheOperationOutcomes.NonStopOperationOutcomes.TIMEOUT),
                EnumSet.allOf(CacheOperationOutcomes.NonStopOperationOutcomes.class));

    }

    /**
     * Find standard operation statistics.
     */
    private void findStandardOperationStatistics() {
        for (final StandardOperationStatistic t : StandardOperationStatistic.values()) {
            OperationStatistic statistic = findOperationStatistic(manager, t);
            if (statistic == null) {
                if (t.required()) {
                    throw new IllegalStateException("Required statistic " + t + " not found");
                } else {
                    LOGGER.debug("Mocking Operation Statistic: {}", t);
                    standardOperations.put(t, NullCompoundOperation.instance(t.type()));
                }
            } else {
                standardOperations.put(t, new CompoundOperationImpl(statistic, t.type(), StatisticsGateway.DEFAULT_WINDOW_SIZE_SECS,
                        SECONDS, executor, defaultHistorySize, t.isSearch() ? defaultSearchIntervalSeconds : defaultIntervalSeconds,
                        SECONDS));
            }
        }
    }

    /**
     * Find standard pass thru statistics.
     */
    private void findStandardPassThruStatistics() {
        for (final StandardPassThroughStatistic t : StandardPassThroughStatistic.values()) {
            ValueStatistic statistic = findPassThroughStatistic(manager, t);
            if (statistic == null) {
                LOGGER.debug("Mocking Pass-Through Statistic: {}", t);
                standardPassThroughs.put(t, NullStatistic.instance(t.absentValue()));
            } else {
                standardPassThroughs.put(t,
                    new SemiExpiringStatistic(statistic, executor, defaultHistorySize, SECONDS.toNanos(defaultIntervalSeconds)));
            }
        }
    }

    @Override
    public Set<Statistic<Number>> passthru(String name, Set<String> tags) {
        ArrayList<String> key = new ArrayList<String>(tags.size() + 1);
        key.addAll(tags);
        Collections.sort(key);
        key.add(name);

        if (customPassthrus.containsKey(key)) {
            return customPassthrus.get(key);
        }
        // lets make sure we don't get it twice.
        synchronized (customPassthrus) {
            if (customPassthrus.containsKey(key)) {
                return customPassthrus.get(key);
            }
            Set<ValueStatistic<?>> interim = findPassThroughStatistic(manager,
                EhcacheQueryBuilder.cache().descendants(),
                name,
                tags);
            if (interim.isEmpty()) {
                return Collections.EMPTY_SET;
            }
            Set<Statistic<Number>> ret = new HashSet<Statistic<Number>>(interim.size());
            for (ValueStatistic<?> vs : interim) {
                SemiExpiringStatistic stat = new SemiExpiringStatistic(vs, executor, defaultHistorySize, SECONDS.toNanos(defaultIntervalSeconds));
                ret.add(stat);
            }
            customPassthrus.put(key, ret);
            return ret;
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
        return (Operation<GetOutcome>) getStandardOperation(StandardOperationStatistic.CACHE_GET);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#put()
     */
    @Override
    public Operation<PutOutcome> put() {
        return (Operation<PutOutcome>) getStandardOperation(StandardOperationStatistic.CACHE_PUT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#remove()
     */
    @Override
    public Operation<RemoveOutcome> remove() {
        return (Operation<RemoveOutcome>) getStandardOperation(StandardOperationStatistic.CACHE_REMOVE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#search()
     */
    @Override
    public Operation<SearchOutcome> search() {
        return (Operation<CacheOperationOutcomes.SearchOutcome>) getStandardOperation(StandardOperationStatistic.SEARCH);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#heapGet()
     */
    @Override
    public Operation<StoreOperationOutcomes.GetOutcome> heapGet() {
        return (Operation<StoreOperationOutcomes.GetOutcome>) getStandardOperation(StandardOperationStatistic.HEAP_GET);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#heapPut()
     */
    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome> heapPut() {
        return (Operation<StoreOperationOutcomes.PutOutcome>) getStandardOperation(StandardOperationStatistic.HEAP_PUT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#heapRemove()
     */
    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome> heapRemove() {
        return (Operation<StoreOperationOutcomes.RemoveOutcome>) getStandardOperation(StandardOperationStatistic.HEAP_REMOVE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#offheapGet()
     */
    @Override
    public Operation<StoreOperationOutcomes.GetOutcome> offheapGet() {
        return (Operation<StoreOperationOutcomes.GetOutcome>) getStandardOperation(StandardOperationStatistic.OFFHEAP_GET);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#offheapPut()
     */
    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome> offheapPut() {
        return (Operation<StoreOperationOutcomes.PutOutcome>) getStandardOperation(StandardOperationStatistic.OFFHEAP_PUT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#offheapRemove()
     */
    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome> offheapRemove() {
        return (Operation<StoreOperationOutcomes.RemoveOutcome>) getStandardOperation(StandardOperationStatistic.OFFHEAP_REMOVE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#diskGet()
     */
    @Override
    public Operation<StoreOperationOutcomes.GetOutcome> diskGet() {
        return (Operation<StoreOperationOutcomes.GetOutcome>) getStandardOperation(StandardOperationStatistic.DISK_GET);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#diskPut()
     */
    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome> diskPut() {
        return (Operation<StoreOperationOutcomes.PutOutcome>) getStandardOperation(StandardOperationStatistic.DISK_PUT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#diskRemove()
     */
    @Override
    public Operation<net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome> diskRemove() {
        return (Operation<StoreOperationOutcomes.RemoveOutcome>) getStandardOperation(StandardOperationStatistic.DISK_REMOVE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#xaCommit()
     */
    @Override
    public Operation<XaCommitOutcome> xaCommit() {
        return (Operation<XaCommitOutcome>) getStandardOperation(StandardOperationStatistic.XA_COMMIT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#xaRollback()
     */
    @Override
    public Operation<XaRollbackOutcome> xaRollback() {
        return (Operation<XaRollbackOutcome>) getStandardOperation(StandardOperationStatistic.XA_ROLLBACK);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#xaRecovery()
     */
    @Override
    public Operation<XaRecoveryOutcome> xaRecovery() {
        return (Operation<XaRecoveryOutcome>) getStandardOperation(StandardOperationStatistic.XA_RECOVERY);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#eviction()
     */
    @Override
    public Operation<EvictionOutcome> eviction() {
        return (Operation<CacheOperationOutcomes.EvictionOutcome>) getStandardOperation(StandardOperationStatistic.EVICTION);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#expiry()
     */
    @Override
    public Operation<ExpiredOutcome> expiry() {
        return (Operation<CacheOperationOutcomes.ExpiredOutcome>) getStandardOperation(StandardOperationStatistic.EXPIRY);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getCacheHitRatio()
     */
    @Override
    public Statistic<Double> cacheHitRatio() {
        return cacheHitRatio;
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
        Set<OperationStatistic<T>> sources = findOperationStatistic(manager, queryBuilder().descendants().build(), outcome, name,
                new HashSet<String>(Arrays.asList(tags)));
        if (sources.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<Operation<T>> operations = new HashSet();
            for (OperationStatistic<T> source : sources) {
                CompoundOperationImpl<T> operation = (CompoundOperationImpl<T>) customOperations.get(source);
                if (operation == null) {
                    operation = new CompoundOperationImpl<T>(source, source.type(), 1, SECONDS, executor, 0, 1, SECONDS);
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
    public Statistic<Number> localHeapSize() {
        return getStandardPassThrough(StandardPassThroughStatistic.LOCAL_HEAP_SIZE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getLocalHeapSizeInBytes()
     */
    @Override
    public Statistic<Number> localHeapSizeInBytes() {
        return getStandardPassThrough(StandardPassThroughStatistic.LOCAL_HEAP_SIZE_BYTES);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getLocalOffHeapSize()
     */
    @Override
    public Statistic<Number> localOffHeapSize() {
        return getStandardPassThrough(StandardPassThroughStatistic.LOCAL_OFFHEAP_SIZE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getLocalOffHeapSizeInBytes()
     */
    @Override
    public Statistic<Number> localOffHeapSizeInBytes() {
        return getStandardPassThrough(StandardPassThroughStatistic.LOCAL_OFFHEAP_SIZE_BYTES);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getLocalDiskSize()
     */
    @Override
    public Statistic<Number> localDiskSize() {
        return getStandardPassThrough(StandardPassThroughStatistic.LOCAL_DISK_SIZE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getLocalDiskSizeInBytes()
     */
    @Override
    public Statistic<Number> localDiskSizeInBytes() {
        return getStandardPassThrough(StandardPassThroughStatistic.LOCAL_DISK_SIZE_BYTES);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getRemoteSize()
     */
    @Override
    public Statistic<Number> remoteSize() {
        return getStandardPassThrough(StandardPassThroughStatistic.REMOTE_SIZE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getSize()
     */
    @Override
    public Statistic<Number> size() {
        return getStandardPassThrough(StandardPassThroughStatistic.CACHE_SIZE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#getWriterQueueLength()
     */
    @Override
    public Statistic<Number> writerQueueLength() {
        return getStandardPassThrough(StandardPassThroughStatistic.WRITER_QUEUE_LENGTH);
    }

    /**
     * Gets the standard operation.
     * 
     * @param statistic the statistic
     * @return the standard operation
     */
    private Operation<?> getStandardOperation(StandardOperationStatistic statistic) {
        Operation<?> operation = standardOperations.get(statistic);
        if (operation instanceof NullCompoundOperation<?>) {
            OperationStatistic<?> discovered = findOperationStatistic(manager, statistic);
            if (discovered == null) {
                return operation;
            } else {
                Operation<?> newOperation = new CompoundOperationImpl(discovered, statistic.type(),
                        StatisticsGateway.DEFAULT_WINDOW_SIZE_SECS, SECONDS, executor, defaultHistorySize,
                        statistic.isSearch() ? defaultSearchIntervalSeconds : defaultIntervalSeconds, SECONDS);
                if (standardOperations.replace(statistic, operation, newOperation)) {
                    return newOperation;
                } else {
                    return standardOperations.get(statistic);
                }
            }
        } else {
            return operation;
        }
    }

    /**
     * Gets the standard pass through.
     * 
     * @param statistic the statistic
     * @return the standard pass through
     */
    private Statistic<Number> getStandardPassThrough(StandardPassThroughStatistic statistic) {
        Statistic<Number> passThrough = standardPassThroughs.get(statistic);
        if (passThrough instanceof NullStatistic<?>) {
            ValueStatistic discovered = findPassThroughStatistic(manager, statistic);
            if (discovered == null) {
                return passThrough;
            } else {
                Statistic<Number> newPassThrough = new SemiExpiringStatistic(discovered, executor, defaultHistorySize,
                        SECONDS.toNanos(defaultIntervalSeconds));
                if (standardPassThroughs.replace(statistic, passThrough, newPassThrough)) {
                    return newPassThrough;
                } else {
                    return standardPassThroughs.get(statistic);
                }
            }
        } else {
            return passThrough;
        }
    }

    /**
     * Find operation statistic.
     * 
     * @param manager the manager
     * @param statistic the statistic
     * @return the operation statistic
     */
    private static OperationStatistic findOperationStatistic(StatisticsManager manager, StandardOperationStatistic statistic) {
        Set<OperationStatistic<? extends Enum>> results = findOperationStatistic(manager,
            statistic.context(),
            statistic.type(),
            statistic.operationName(),
            statistic.tags());
        switch (results.size()) {
            case 0:
                return null;
            case 1:
                return results.iterator().next();
            default:
                throw new IllegalStateException("Duplicate statistics found for " + statistic);
        }
    }

    /**
     * Find pass through statistic.
     * 
     * @param manager the manager
     * @param statistic the statistic
     * @return the value statistic
     */
    private static ValueStatistic findPassThroughStatistic(StatisticsManager manager, StandardPassThroughStatistic statistic) {
        Set<ValueStatistic<?>> results = findPassThroughStatistic(manager, statistic.context(), statistic.statisticName(), statistic.tags());
        switch (results.size()) {
            case 0:
                return null;
            case 1:
                return results.iterator().next();
            default:
                throw new IllegalStateException("Duplicate statistics found for " + statistic);
        }
    }

    /**
     * Find operation statistic.
     * 
     * @param <T> the generic type
     * @param manager the manager
     * @param contextQuery the context query
     * @param type the type
     * @param name the name
     * @param tags the tags
     * @return the sets the
     */
    private static <T extends Enum<T>> Set<OperationStatistic<T>> findOperationStatistic(StatisticsManager manager, Query contextQuery,
            Class<T> type, String name, final Set<String> tags) {
        Set<TreeNode> operationStatisticNodes = manager.query(queryBuilder().chain(contextQuery).children()
                .filter(context(identifier(subclassOf(OperationStatistic.class)))).build());
        Set<TreeNode> result = queryBuilder()
                .filter(context(attributes(Matchers.<Map<String, Object>> allOf(hasAttribute("type", type), hasAttribute("name", name),
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

    /**
     * Find pass through statistic.
     * 
     * @param manager the manager
     * @param contextQuery the context query
     * @param name the name
     * @param tags the tags
     * @return the sets the
     */
    private static Set<ValueStatistic<?>> findPassThroughStatistic(StatisticsManager manager, Query contextQuery, String name,
    final Set<String> tags) {
        Set<TreeNode> passThroughStatisticNodes = manager.query(queryBuilder().chain(contextQuery).children()
                .filter(context(identifier(subclassOf(ValueStatistic.class)))).build());
        Set<TreeNode> result = queryBuilder()
                .filter(context(attributes(Matchers.<Map<String, Object>> allOf(hasAttribute("name", name),
                        hasAttribute("tags", new Matcher<Set<String>>() {
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

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#clusterEvent()
     */
    @Override
    public Operation<ClusterEventOutcomes> clusterEvent() {
        return (Operation<ClusterEventOutcomes>) getStandardOperation(StandardOperationStatistic.CLUSTER_EVENT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#clusterEvent()
     */
    @Override
    public Operation<NonStopOperationOutcomes> nonstop() {
        return (Operation<NonStopOperationOutcomes>) getStandardOperation(StandardOperationStatistic.NONSTOP);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.statistics.extended.ExtendedStatistics#lastRejoinTimeStampInNanos()
     */
    @Override
    public Statistic<Number> mostRecentRejoinTimeStampMillis() {
        return getStandardPassThrough(StandardPassThroughStatistic.LAST_REJOIN_TIMESTAMP);
    }

    @Override
    public Statistic<Double> nonstopTimeoutRatio() {
        return nonStopTimeoutRatio;
    }

}
