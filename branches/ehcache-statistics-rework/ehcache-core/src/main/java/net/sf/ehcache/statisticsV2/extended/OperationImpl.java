/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Latency;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Statistic;
import org.terracotta.statistics.SourceStatistic;
import org.terracotta.statistics.observer.OperationObserver;

/**
 *
 * @author cdennis
 */
class OperationImpl<T extends Enum<T>> implements Operation {
    private final RateStatistic rate;
    private final LatencyImpl latency;
    private final SourceStatistic<OperationObserver<T>> source;

    public OperationImpl(SourceStatistic<OperationObserver<T>> source, Set<T> targets, long averagePeriod, TimeUnit averageUnit, ScheduledExecutorService executor, int historySize, long historyPeriod, TimeUnit historyUnit) {
        this.source = source;
        this.latency = new LatencyImpl(source, targets, averagePeriod, averageUnit, executor, historySize, historyPeriod, historyUnit);
        this.rate = new RateStatistic(source, targets, averagePeriod, averageUnit, executor, historySize, historyPeriod, historyUnit);
    }

    public void start() {
        rate.start();
        latency.start();
    }

    public void stop() {
        rate.stop();
        latency.stop();
    }

    @Override
    public Statistic<Double> rate() {
        return rate;
    }

    @Override
    public Latency latency() throws UnsupportedOperationException {
        return latency;
    }
}
