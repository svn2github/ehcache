/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Statistic;
import org.terracotta.statistics.Time;
import org.terracotta.statistics.ValueStatistic;
import org.terracotta.statistics.archive.Timestamped;

/**
 *
 * @author cdennis
 */
public class RatioStatistic implements Statistic<Double> {

    private final ValueStatistic<Double> ratio;
    private final SampledStatistic<Double> history;
    private boolean alive = false;
    private long touchTimestamp = -1;
    
    public RatioStatistic(final Statistic<? extends Number> numerator, final Statistic<? extends Number> denominator, ScheduledExecutorService executor, int historySize, long historyNanos) {
        this.ratio = new ValueStatistic<Double>() {
            @Override
            public Double value() {
                return numerator.value().doubleValue() / denominator.value().doubleValue();
            }
        };
        this.history = new SampledStatistic<Double>(ratio, executor, historySize, historyNanos);
    }
    
    public synchronized void start() {
        if (!alive) {
            history.startSampling();
            alive = true;
        }
    }

    public synchronized void stop() {
        if (alive) {
            history.stopSampling();
            alive = false;
        }
    }

    @Override
    public Double value() {
        return ratio.value();
    }

    @Override
    public List<Timestamped<Double>> history() {
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

    void setHistory(int historySize, long historyNanos) {
        history.adjust(historySize, historyNanos);
    }
}
