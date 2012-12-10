/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Statistic;
import org.terracotta.statistics.ValueStatistic;
import org.terracotta.statistics.archive.StatisticArchive;
import org.terracotta.statistics.archive.StatisticSampler;
import org.terracotta.statistics.archive.Timestamped;

/**
 *
 * @author cdennis
 */
class StatisticImpl<T extends Number> implements Statistic<T> {
    private final ValueStatistic<T> statistic;
    private final StatisticSampler<T> sampler;
    private final StatisticArchive<T> history;

    public StatisticImpl(ValueStatistic<T> statistic, ScheduledExecutorService executor, int historySize, long period, TimeUnit unit) {
        this.statistic = statistic;
        this.history = new StatisticArchive<T>(historySize);
        this.sampler = new StatisticSampler<T>(executor, period, unit, statistic, history);
    }

    public void startSampling() {
        sampler.start();
    }

    public void stopSampling() {
        sampler.stop();
    }

    @Override
    public T value() {
        return statistic.value();
    }

    @Override
    public List<Timestamped<T>> history() {
        return history.getArchive();
    }
}
