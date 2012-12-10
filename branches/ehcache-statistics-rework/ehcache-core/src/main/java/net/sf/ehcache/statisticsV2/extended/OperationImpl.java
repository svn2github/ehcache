/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Latency;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Statistic;
import org.terracotta.statistics.SourceStatistic;
import org.terracotta.statistics.derived.EventRateSimpleMovingAverage;
import org.terracotta.statistics.derived.OperationResultFilter;
import org.terracotta.statistics.observer.OperationObserver;

/**
 *
 * @author cdennis
 */
class OperationImpl<T extends Enum<T>> implements Operation {
    private final OperationObserver<T> rateObserver;
    private final StatisticImpl<Double> rateStatistic;
    private final LatencyImpl latency;
    private final SourceStatistic<OperationObserver<T>> source;

    public OperationImpl(SourceStatistic<OperationObserver<T>> source, T target, long averagePeriod, TimeUnit averageUnit, ScheduledExecutorService executor, int historySize, long historyPeriod, TimeUnit historyUnit) {
        this.source = source;
        this.latency = new LatencyImpl(source, target, averagePeriod, averageUnit, executor, historySize, historyPeriod, historyUnit);
        EventRateSimpleMovingAverage rate = new EventRateSimpleMovingAverage(averagePeriod, averageUnit);
        this.rateObserver = new OperationResultFilter<T>(target, rate);
        this.rateStatistic = new StatisticImpl(rate, executor, historySize, historyPeriod, historyUnit);
    }

    public void start() {
        source.addDerivedStatistic(rateObserver);
        rateStatistic.startSampling();
        latency.start();
    }

    public void stop() {
        source.removeDerivedStatistic(rateObserver);
        rateStatistic.stopSampling();
        latency.stop();
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
