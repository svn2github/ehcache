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
public class RatioStatistic extends AbstractStatistic<Double> {

    private final ValueStatistic<Double> ratio;
    
    public RatioStatistic(final Statistic<? extends Number> numerator, final Statistic<? extends Number> denominator, ScheduledExecutorService executor, int historySize, long historyNanos) {
        super(executor, historySize, historyNanos);
        this.ratio = new ValueStatistic<Double>() {
            @Override
            public Double value() {
                return numerator.value().doubleValue() / denominator.value().doubleValue();
            }
        };
    }
    
    @Override
    public Double value() {
        //XXX is this correct?
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
    Double readStatistic() {
        return ratio.value();
    }
}
