/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
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
    private final CountStatistic count;
    private final RateStatistic rate;
    private final LatencyImpl latency;
    
    public OperationImpl(OperationStatistic<T> source, Set<T> targets, long averageNanos, ScheduledExecutorService executor, int historySize, long historyNanos) {
        this.source = source;
        this.count = new CountStatistic(source, targets, executor, historySize, historyNanos);
        this.latency = new LatencyImpl(source, targets, averageNanos, executor, historySize, historyNanos);
        this.rate = new RateStatistic(source, targets, averageNanos, executor, historySize, historyNanos);
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
    public Statistic<Long> count() {
        return count;
    }

    void start() {
        count.start();
        rate.start();
        latency.start();
    }

    boolean expire(long expiryTime) {
        if (count.expire(expiryTime) & rate.expire(expiryTime) & latency.expire(expiryTime)) {
            return true;
        } else {
            return false;
        }
    }

    void setWindow(long averageNanos) {
        rate.setWindow(averageNanos);
        latency.setWindow(averageNanos);
    }

    void setHistory(int historySize, long historyNanos) {
        count.setHistory(historySize, historyNanos);
        rate.setHistory(historySize, historyNanos);
        latency.setHistory(historySize, historyNanos);
    }
}
