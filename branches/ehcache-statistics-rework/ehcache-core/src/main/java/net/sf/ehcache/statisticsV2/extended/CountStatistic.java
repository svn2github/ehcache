/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import org.terracotta.statistics.OperationStatistic;
import org.terracotta.statistics.ValueStatistic;

/**
 *
 * @author cdennis
 */
class CountStatistic extends AbstractStatistic<Long> {

    private final ValueStatistic<Long> statistic;
    
    public <T extends Enum<T>> CountStatistic(OperationStatistic<T> source, Set<T> targets, ScheduledExecutorService executor, int historySize, long historyNanos) {
        super(executor, historySize, historyNanos);
        this.statistic = source.statistic(targets);
    }

    
    @Override
    public Long value() {
        //reading the value doesn't touch the statistic
        return readStatistic();
    }

    @Override
    void stopStatistic() {
        //no-op
    }

    @Override
    void startStatistic() {
        //no-op
    }

    @Override
    Long readStatistic() {
        return statistic.value();
    }
}
