/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Latency;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Statistic;
import org.terracotta.statistics.OperationStatistic;

/**
 *
 * @author cdennis
 */
class OperationImpl<T extends Enum<T>> implements Operation {
    private final OperationStatistic<T> source;
    private final Set<T> targets;
    private final RateStatistic rate;
    private final LatencyImpl latency;

    public OperationImpl(OperationStatistic<T> source, Set<T> targets, long averagePeriod, TimeUnit averageUnit, ScheduledExecutorService executor, int historySize, long historyPeriod, TimeUnit historyUnit) {
        this.source = source;
        this.targets = EnumSet.copyOf(targets);
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

    @Override
    public long count() {
        return source.sum(targets);
    }

    boolean expire(long expiryTime) {
        if (rate.expire(expiryTime) && latency.expire(expiryTime)) {
            return true;
        } else {
            return false;
        }
    }
}
