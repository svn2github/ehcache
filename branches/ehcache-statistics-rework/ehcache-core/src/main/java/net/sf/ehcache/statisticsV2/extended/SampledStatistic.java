/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.terracotta.statistics.ValueStatistic;
import org.terracotta.statistics.archive.StatisticArchive;
import org.terracotta.statistics.archive.StatisticSampler;
import org.terracotta.statistics.archive.Timestamped;

/**
 *
 * @author cdennis
 */
class SampledStatistic<T> {
    private final StatisticSampler<T> sampler;
    private final StatisticArchive<T> history;

    public SampledStatistic(ValueStatistic<T> statistic, ScheduledExecutorService executor, int historySize, long periodNanos) {
        this.history = new StatisticArchive<T>(historySize);
        this.sampler = new StatisticSampler<T>(executor, periodNanos, TimeUnit.NANOSECONDS, statistic, history);
    }

    public void startSampling() {
        sampler.start();
    }

    public void stopSampling() {
        sampler.stop();
    }

    public List<Timestamped<T>> history() {
        return history.getArchive();
    }

    void adjust(int historySize, long historyNanos) {
        history.setCapacity(historySize);
        sampler.setPeriod(historyNanos, TimeUnit.NANOSECONDS);
    }
}
