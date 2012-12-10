/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Statistic;
import org.terracotta.statistics.SourceStatistic;
import org.terracotta.statistics.Time;
import org.terracotta.statistics.archive.Timestamped;
import org.terracotta.statistics.derived.EventRateSimpleMovingAverage;
import org.terracotta.statistics.derived.OperationResultFilter;
import org.terracotta.statistics.observer.OperationObserver;

/**
 *
 * @author cdennis
 */
public class RateStatistic<T extends Enum<T>> implements Statistic<Double> {

    private final SourceStatistic<OperationObserver<T>> source;
    private final OperationObserver<T> rateObserver;
    private final EventRateSimpleMovingAverage rate;
    private final SampledStatistic<Double> history;
    
    private boolean alive = false;
    private long touchTimestamp = -1;
    
    public RateStatistic(SourceStatistic<OperationObserver<T>> statistic, Set<T> targets, long averagePeriod, TimeUnit averageUnit, ScheduledExecutorService executor, int historySize, long historyPeriod, TimeUnit historyUnit) {
        this.source = statistic;
        this.rate = new EventRateSimpleMovingAverage(averagePeriod, averageUnit);
        this.rateObserver = new OperationResultFilter<T>(targets, rate);
        this.history = new SampledStatistic<Double>(rate, executor, historySize, historyPeriod, historyUnit);
    }
    
    public synchronized void start() {
        if (!alive) {
            source.addDerivedStatistic(rateObserver);
            history.startSampling();
            alive = true;
        }
    }

    public synchronized void stop() {
        if (alive) {
            history.stopSampling();
            source.removeDerivedStatistic(rateObserver);
            alive = false;
        }
    }

    @Override
    public Double value() {
        touch();
        return rate.rate(TimeUnit.SECONDS);
    }

    @Override
    public List<Timestamped<Double>> history() throws UnsupportedOperationException {
        touch();
        return history.history();
    }

    private synchronized void touch() {
        touchTimestamp = Time.absoluteTime();
        start();
    }
    
    public synchronized boolean expire(long expiry) {
        if (touchTimestamp < expiry) {
            stop();
            return true;
        } else {
            return false;
        }
    }
}
