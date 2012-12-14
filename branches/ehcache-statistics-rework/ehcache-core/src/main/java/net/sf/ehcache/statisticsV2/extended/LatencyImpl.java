/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Latency;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Statistic;
import org.terracotta.statistics.SourceStatistic;
import org.terracotta.statistics.Time;
import org.terracotta.statistics.ValueStatistic;
import org.terracotta.statistics.archive.Timestamped;
import org.terracotta.statistics.derived.EventParameterSimpleMovingAverage;
import org.terracotta.statistics.derived.LatencySampling;
import org.terracotta.statistics.observer.OperationObserver;

/**
 *
 * @author cdennis
 */
class LatencyImpl<T extends Enum<T>> implements Latency {
    private final SourceStatistic<OperationObserver<T>> source;
    private final LatencySampling<T> latencySampler;
    private final EventParameterSimpleMovingAverage average;
    private final StatisticImpl<Long> minimumStatistic;
    private final StatisticImpl<Long> maximumStatistic;
    private final StatisticImpl<Double> averageStatistic;

    private boolean active = false;
    private long touchTimestamp = -1;
    
    public LatencyImpl(SourceStatistic<OperationObserver<T>> statistic, Set<T> targets, long averageNanos, ScheduledExecutorService executor, int historySize, long historyNanos) {
        this.average = new EventParameterSimpleMovingAverage(averageNanos, TimeUnit.NANOSECONDS);
        this.minimumStatistic = new StatisticImpl<Long>(average.minimumStatistic(), executor, historySize, historyNanos);
        this.maximumStatistic = new StatisticImpl<Long>(average.maximumStatistic(), executor, historySize, historyNanos);
        this.averageStatistic = new StatisticImpl<Double>(average.averageStatistic(), executor, historySize, historyNanos);
        this.latencySampler = new LatencySampling(targets, 1.0);
        latencySampler.addDerivedStatistic(average);
        this.source = statistic;
    }

    synchronized void start() {
        if (!active) {
            source.addDerivedStatistic(latencySampler);
            minimumStatistic.startSampling();
            maximumStatistic.startSampling();
            averageStatistic.startSampling();
            active = true;
        }
    }

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

    private synchronized void touch() {
        touchTimestamp = Time.absoluteTime();
        start();
    }
    
    public synchronized boolean expire(long expiry) {
        if (touchTimestamp < expiry) {
            if (active) {
                source.removeDerivedStatistic(latencySampler);
                minimumStatistic.stopSampling();
                maximumStatistic.stopSampling();
                averageStatistic.stopSampling();
                active = false;
            }
            return true;
        } else {
            return false;
        }
    }

    void setWindow(long averageNanos) {
        average.setWindow(averageNanos, TimeUnit.NANOSECONDS);
    }

    void setHistory(int historySize, long historyNanos) {
        minimumStatistic.setHistory(historySize, historyNanos);
        maximumStatistic.setHistory(historySize, historyNanos);
        averageStatistic.setHistory(historySize, historyNanos);
    }
    
    class StatisticImpl<T> implements Statistic<T> {

        private final ValueStatistic<T> value;
        private final SampledStatistic<T> history;

        public StatisticImpl(ValueStatistic<T> value, ScheduledExecutorService executor, int historySize, long historyNanos) {
            this.value = value;
            this.history = new SampledStatistic<T>(value, executor, historySize, historyNanos);
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public T value() {
            touch();
            return value.value();
        }

        @Override
        public List<Timestamped<T>> history() throws UnsupportedOperationException {
            touch();
            return history.history();
        }

        private void startSampling() {
            history.startSampling();
        }
        
        private void stopSampling() {
            history.stopSampling();
        }

        private void setHistory(int historySize, long historyNanos) {
            history.adjust(historySize, historyNanos);
        }
    }
}
