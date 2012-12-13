/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Statistic;
import org.terracotta.statistics.OperationStatistic;
import org.terracotta.statistics.Time;
import org.terracotta.statistics.ValueStatistic;
import org.terracotta.statistics.archive.Timestamped;

/**
 *
 * @author cdennis
 */
class CountStatistic implements Statistic<Long> {
    private final ValueStatistic<Long> statistic;
    private final SampledStatistic<Long> history;
    private boolean alive = false;
    private long touchTimestamp = -1;

    public <T extends Enum<T>> CountStatistic(OperationStatistic<T> source, Set<T> targets, ScheduledExecutorService executor, int historySize, long historyNanos) {
        this.statistic = source.statistic(targets);
        this.history = new SampledStatistic<Long>(statistic, executor, historySize, historyNanos);
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
    public Long value() {
        return statistic.value();
    }

    @Override
    public List<Timestamped<Long>> history() {
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
