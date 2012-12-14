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
public class RateStatistic<T extends Enum<T>> extends AbstractStatistic<Double> {

    private final SourceStatistic<OperationObserver<T>> source;
    private final OperationObserver<T> rateObserver;
    private final EventRateSimpleMovingAverage rate;
    
    public RateStatistic(SourceStatistic<OperationObserver<T>> statistic, Set<T> targets, long averageNanos, ScheduledExecutorService executor, int historySize, long historyNanos) {
        super(executor, historySize, historyNanos);
        this.source = statistic;
        this.rate = new EventRateSimpleMovingAverage(averageNanos, TimeUnit.NANOSECONDS);
        this.rateObserver = new OperationResultFilter<T>(targets, rate);
    }
    
    void setWindow(long averageNanos) {
        rate.setWindow(averageNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    void stopStatistic() {
        source.removeDerivedStatistic(rateObserver);
    }

    @Override
    void startStatistic() {
        source.addDerivedStatistic(rateObserver);
    }

    @Override
    Double readStatistic() {
        return rate.value();
    }
}
