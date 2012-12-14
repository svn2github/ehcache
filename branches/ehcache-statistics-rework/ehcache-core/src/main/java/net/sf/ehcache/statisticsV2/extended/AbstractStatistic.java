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
abstract class AbstractStatistic<T> implements Statistic<T> {
    
    private final SampledStatistic<T> history;
    private boolean active = false;
    private long touchTimestamp = -1;

    public AbstractStatistic(ScheduledExecutorService executor, int historySize, long historyNanos) {
        this.history = new SampledStatistic<T>(new ValueStatistic<T>() {

            @Override
            public T value() {
                return readStatistic();
            }
        }, executor, historySize, historyNanos);
    }

    @Override
    public final synchronized boolean active() {
        return active;
    }

    @Override
    public T value() {
        touch();
        return readStatistic();
    }

    @Override
    public final List<Timestamped<T>> history() {
        touch();
        return history.history();
    }

    private final synchronized void touch() {
        touchTimestamp = Time.absoluteTime();
        start();
    }

    final synchronized void start() {
        if (!active) {
            startStatistic();
            history.startSampling();
            active = true;
        }
    }

    final synchronized boolean expire(long expiry) {
        if (touchTimestamp < expiry) {
            if (active) {
                history.stopSampling();
                stopStatistic();
                active = false;
            }
            return true;
        } else {
            return false;
        }
    }

    final void setHistory(int historySize, long historyNanos) {
        history.adjust(historySize, historyNanos);
    }
    
    abstract void stopStatistic();
    abstract void startStatistic();
    abstract T readStatistic();
}
