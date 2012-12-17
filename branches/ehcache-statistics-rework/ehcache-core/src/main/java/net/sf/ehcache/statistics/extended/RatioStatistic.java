/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.ehcache.statistics.extended;

import java.util.concurrent.ScheduledExecutorService;

import net.sf.ehcache.statistics.extended.ExtendedStatistics.Statistic;

import org.terracotta.statistics.ValueStatistic;

/**
 * The Class RatioStatistic.
 *
 * @author cdennis
 */
public class RatioStatistic extends AbstractStatistic<Double> {

    /** The ratio. */
    private final ValueStatistic<Double> ratio;

    /**
     * Instantiates a new ratio statistic.
     *
     * @param numerator the numerator
     * @param denominator the denominator
     * @param executor the executor
     * @param historySize the history size
     * @param historyNanos the history nanos
     */
    public RatioStatistic(final Statistic<? extends Number> numerator, final Statistic<? extends Number> denominator,
            ScheduledExecutorService executor, int historySize, long historyNanos) {
        super(executor, historySize, historyNanos);
        this.ratio = new ValueStatistic<Double>() {
            @Override
            public Double value() {
                return numerator.value().doubleValue() / denominator.value().doubleValue();
            }
        };
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.extended.AbstractStatistic#value()
     */
    @Override
    public Double value() {
        // XXX is this correct?
        // reading the value doesn't touch the statistic
        return readStatistic();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.extended.AbstractStatistic#stopStatistic()
     */
    @Override
    void stopStatistic() {
        // no-op
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.extended.AbstractStatistic#startStatistic()
     */
    @Override
    void startStatistic() {
        // no-op
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.extended.AbstractStatistic#readStatistic()
     */
    @Override
    Double readStatistic() {
        return ratio.value();
    }
}
