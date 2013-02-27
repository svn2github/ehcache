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

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.terracotta.statistics.SourceStatistic;
import org.terracotta.statistics.derived.EventRateSimpleMovingAverage;
import org.terracotta.statistics.derived.OperationResultFilter;
import org.terracotta.statistics.observer.ChainedOperationObserver;

/**
 * The Class RateStatistic.
 *
 * @param <T> the generic type
 * @author cdennis
 */
public class RateStatistic<T extends Enum<T>> extends AbstractStatistic<Double> {

    /** The source. */
    private final SourceStatistic<ChainedOperationObserver<T>> source;

    /** The rate observer. */
    private final ChainedOperationObserver<T> rateObserver;

    /** The rate. */
    private final EventRateSimpleMovingAverage rate;

    /**
     * Instantiates a new rate statistic.
     *
     * @param statistic the statistic
     * @param targets the targets
     * @param averageNanos the average nanos
     * @param executor the executor
     * @param historySize the history size
     * @param historyNanos the history nanos
     */
    public RateStatistic(SourceStatistic<ChainedOperationObserver<T>> statistic, Set<T> targets, long averageNanos,
            ScheduledExecutorService executor, int historySize, long historyNanos) {
        super(executor, historySize, historyNanos);
        this.source = statistic;
        this.rate = new EventRateSimpleMovingAverage(averageNanos, TimeUnit.NANOSECONDS);
        this.rateObserver = new OperationResultFilter<T>(targets, rate);
    }

    /**
     * Sets the window.
     *
     * @param averageNanos the new window
     */
    void setWindow(long averageNanos) {
        rate.setWindow(averageNanos, TimeUnit.NANOSECONDS);
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.extended.AbstractStatistic#stopStatistic()
     */
    @Override
    void stopStatistic() {
        source.removeDerivedStatistic(rateObserver);
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.extended.AbstractStatistic#startStatistic()
     */
    @Override
    void startStatistic() {
        source.addDerivedStatistic(rateObserver);
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.extended.AbstractStatistic#readStatistic()
     */
    @Override
    Double readStatistic() {
        return rate.value();
    }
}
