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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheOperationOutcomes.GetOutcome;
import net.sf.ehcache.CacheOperationOutcomes.PutOutcome;
import net.sf.ehcache.CacheOperationOutcomes.RemoveOutcome;
import net.sf.ehcache.CacheOperationOutcomes.SearchOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

import org.terracotta.context.TreeNode;
import org.terracotta.statistics.SourceStatistic;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.ValueStatistic;
import org.terracotta.statistics.archive.StatisticArchive;
import org.terracotta.statistics.archive.StatisticSampler;
import org.terracotta.statistics.archive.Timestamped;
import org.terracotta.statistics.derived.EventParameterSimpleMovingAverage;
import org.terracotta.statistics.derived.EventRateSimpleMovingAverage;
import org.terracotta.statistics.derived.LatencySampling;
import org.terracotta.statistics.derived.OperationResultFilter;
import org.terracotta.statistics.observer.OperationObserver;

import static org.terracotta.context.query.QueryBuilder.queryBuilder;
import static org.terracotta.context.query.Matchers.*;

public class ExtendedStatisticsImpl implements ExtendedStatistics {
    
    /**
     * The default interval in seconds for the {@link SampledRateCounter} for recording the average search rate counter
     */
    public static int DEFAULT_SEARCH_INTERVAL_SECS = 10;

    /**
     * The default history size for {@link SampledCounter} objects.
     */
    public static int DEFAULT_HISTORY_SIZE = 30;

    /**
     * The default interval for sampling events for {@link SampledCounter} objects.
     */
    public static int DEFAULT_INTERVAL_SECS = 1;

    private final CompoundOperation<GetOutcome> getCompound;
    private final CompoundOperation<PutOutcome> putCompound;
    private final CompoundOperation<RemoveOutcome> removeCompound;
    private final CompoundOperation<?> evictedCompound;
    private final CompoundOperation<?> expiredCompound;
    private final CompoundOperation<StoreOperationOutcomes.GetOutcome> heapGetCompound;
    private final CompoundOperation<StoreOperationOutcomes.GetOutcome> offheapGetCompound;
    private final CompoundOperation<StoreOperationOutcomes.GetOutcome> diskGetCompound;
    private final CompoundOperation<SearchOutcome> searchCompound;
    private final CompoundOperation<XaCommitOutcome> xaCommitCompound;
    private final CompoundOperation<XaRollbackOutcome> xaRollbackCompound;
    
    public ExtendedStatisticsImpl(StatisticsManager manager) {
        getCompound = new CompoundOperationImpl<GetOutcome>(ExtendedStatisticsImpl.<GetOutcome>extractCacheStat(manager, "get"), GetOutcome.class);
        putCompound = new CompoundOperationImpl<PutOutcome>(ExtendedStatisticsImpl.<PutOutcome>extractCacheStat(manager, "put"), PutOutcome.class);
        removeCompound = new CompoundOperationImpl<RemoveOutcome>(ExtendedStatisticsImpl.<RemoveOutcome>extractCacheStat(manager, "put"), RemoveOutcome.class);
        evictedCompound = new CompoundOperationImpl<PutOutcome>(ExtendedStatisticsImpl.<PutOutcome>extractCacheStat(manager, "put"), PutOutcome.class);
        expiredCompound = new CompoundOperationImpl<PutOutcome>(ExtendedStatisticsImpl.<PutOutcome>extractCacheStat(manager, "put"), PutOutcome.class);
        heapGetCompound = new CompoundOperationImpl<StoreOperationOutcomes.GetOutcome>(ExtendedStatisticsImpl.<StoreOperationOutcomes.GetOutcome>extractCacheStat(manager, "put"), StoreOperationOutcomes.GetOutcome.class);
        offheapGetCompound = new CompoundOperationImpl<StoreOperationOutcomes.GetOutcome>(ExtendedStatisticsImpl.<StoreOperationOutcomes.GetOutcome>extractCacheStat(manager, "put"), StoreOperationOutcomes.GetOutcome.class);
        diskGetCompound = new CompoundOperationImpl<StoreOperationOutcomes.GetOutcome>(ExtendedStatisticsImpl.<StoreOperationOutcomes.GetOutcome>extractCacheStat(manager, "put"), StoreOperationOutcomes.GetOutcome.class);
        searchCompound = new CompoundOperationImpl<SearchOutcome>(ExtendedStatisticsImpl.<SearchOutcome>extractCacheStat(manager, "put"), SearchOutcome.class);
        xaCommitCompound = new CompoundOperationImpl<XaCommitOutcome>(ExtendedStatisticsImpl.<XaCommitOutcome>extractCacheStat(manager, "put"), XaCommitOutcome.class);
        xaRollbackCompound = new CompoundOperationImpl<XaRollbackOutcome>(ExtendedStatisticsImpl.<XaRollbackOutcome>extractCacheStat(manager, "put"), XaRollbackOutcome.class);
    }
    
    private static <T extends Enum<T>> SourceStatistic<OperationObserver<T>> extractCacheStat(StatisticsManager manager, String name) {
        TreeNode node = manager.queryForSingleton(queryBuilder().children().ensureUnique()
                .children().filter(context(allOf(identifier(subclassOf(SourceStatistic.class)), attributes(hasAttribute("name", name))))).build());
        return (SourceStatistic<OperationObserver<T>>) node.getContext().attributes().get("this");
    }
    
    @Override
    public void setStatisticsTimeToDisable(long time, TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void setStatisticsEnabled(boolean enabled) {
        
    }

    @Override
    public CompoundOperation<GetOutcome> get() {
        return getCompound;
    }

    @Override
    public CompoundOperation<PutOutcome> put() {
        return putCompound;
    }

    @Override
    public CompoundOperation<RemoveOutcome> remove() {
        return removeCompound;
    }

    @Override
    public CompoundOperation<?> evicted() {
        return evictedCompound;
    }

    @Override
    public CompoundOperation<?> expired() {
        return expiredCompound;
    }

    @Override
    public CompoundOperation<StoreOperationOutcomes.GetOutcome> heapGet() {
        return heapGetCompound;
    }

    @Override
    public CompoundOperation<StoreOperationOutcomes.GetOutcome> offheapGet() {
        return offheapGetCompound;
    }

    @Override
    public CompoundOperation<StoreOperationOutcomes.GetOutcome> diskGet() {
        return diskGetCompound;
    }

    @Override
    public CompoundOperation<SearchOutcome> search() {
        return searchCompound;
    }

    @Override
    public CompoundOperation<XaCommitOutcome> xaCommit() {
        return xaCommitCompound;
    }

    @Override
    public CompoundOperation<XaRollbackOutcome> xaRollback() {
        return xaRollbackCompound;
    }
    
    static class CompoundOperationImpl<T extends Enum<T>> implements CompoundOperation<T> {

        private final Map<T, Operation> operations;
        
        public CompoundOperationImpl(SourceStatistic<OperationObserver<T>> source, Class<T> type) {
            this.operations = new EnumMap(type);
            for (T result : type.getEnumConstants()) {
                operations.put(result, new OperationImpl(source, result));
            }
        }
                
        @Override
        public Operation component(T result) {
            return operations.get(result);
        }
    }
    
    static class OperationImpl<T extends Enum<T>> implements Operation {

        private final OperationObserver<T> rateObserver;
        private final StatisticImpl<Double> rateStatistic;
        private final LatencyImpl latency;
        private final SourceStatistic<OperationObserver<T>> source;

        public OperationImpl(SourceStatistic<OperationObserver<T>> source, T target) {
            this.source = source;
            this.latency = new LatencyImpl(source, target);
            
            EventRateSimpleMovingAverage rate = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
            this.rateObserver = new OperationResultFilter<T>(target, rate);
            this.rateStatistic = new StatisticImpl(rate);
        }

        public void start() {
            source.addDerivedStatistic(rateObserver);
            rateStatistic.start();
        }
        
        public void stop() {
            source.removeDerivedStatistic(rateObserver);
            rateStatistic.stop();
        }
        
        @Override
        public Statistic<Double> rate() {
            return rateStatistic;
        }

        @Override
        public Latency latency() throws UnsupportedOperationException {
            return latency;
        }
    }
    
    static class StatisticImpl<T extends Number> implements Statistic<T> {

        private final ValueStatistic<T> statistic;
        private final StatisticSampler<T> sampler;
        private final StatisticArchive<T> history;
        
        public StatisticImpl(ValueStatistic<T> statistic) {
            this.statistic = statistic;
            this.history = new StatisticArchive<T>(DEFAULT_HISTORY_SIZE);
            this.sampler = new StatisticSampler<T>(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS, statistic, history);
        }

        public void start() {
            sampler.start();
        }
        
        public void stop() {
            sampler.stop();
        }
        
        @Override
        public T value() {
            return statistic.value();
        }

        @Override
        public List<Timestamped<T>> history() {
            return history.getArchive();
        }
    }
    
    static class LatencyImpl<T extends Enum<T>> implements Latency {

        private final SourceStatistic<OperationObserver<T>> source;
        
        private final LatencySampling<T> latencySampler;
        private final EventParameterSimpleMovingAverage average = new EventParameterSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
        private final StatisticImpl<Long> minimumStatistic = new StatisticImpl<Long>(average.minimumStatistic());
        private final StatisticImpl<Long> maximumStatistic = new StatisticImpl<Long>(average.maximumStatistic());
        private final StatisticImpl<Double> averageStatistic = new StatisticImpl<Double>(average.averageStatistic());

        public LatencyImpl(SourceStatistic<OperationObserver<T>> statistic, T target) {
            this.latencySampler = new LatencySampling(target, 1.0);
            latencySampler.addDerivedStatistic(average);
            this.source = statistic;
        }
        
        public void start() {
            source.addDerivedStatistic(latencySampler);
            minimumStatistic.start();
            maximumStatistic.start();
            averageStatistic.start();
        }
        
        public void stop() {
            source.removeDerivedStatistic(latencySampler);
            minimumStatistic.stop();
            maximumStatistic.stop();
            averageStatistic.stop();
        }
        
        @Override
        public Statistic<Long> minimum() {
            return minimumStatistic;
        }

        @Override
        public Statistic<Long> maximum() {
            return maximumStatistic;
        }

        @Override
        public Statistic<Double> average() {
            return averageStatistic;
        }
    }
}
