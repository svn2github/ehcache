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

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import net.sf.ehcache.statistics.extended.ExtendedStatistics.Statistic;

import org.terracotta.statistics.Time;
import org.terracotta.statistics.ValueStatistic;
import org.terracotta.statistics.archive.Timestamped;

/**
 * The Class AbstractStatistic.
 *
 * @param <T> the generic type
 * @author cdennis
 */
abstract class AbstractStatistic<T extends Number> implements Statistic<T> {

    /** The history. */
    private final SampledStatistic<T> history;

    /** The active. */
    private boolean active = false;

    /** The touch timestamp. */
    private long touchTimestamp = -1;

    /**
     * Instantiates a new abstract statistic.
     *
     * @param executor the executor
     * @param historySize the history size
     * @param historyNanos the history nanos
     */
    public AbstractStatistic(ScheduledExecutorService executor, int historySize, long historyNanos) {
        this.history = new SampledStatistic<T>(new ValueStatistic<T>() {

            @Override
            public T value() {
                return readStatistic();
            }
        }, executor, historySize, historyNanos);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Statistic#active()
     */
    @Override
    public final synchronized boolean active() {
        return active;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Statistic#value()
     */
    @Override
    public T value() {
        touch();
        return readStatistic();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Statistic#history()
     */
    @Override
    public final List<Timestamped<T>> history() {
        touch();
        return history.history();
    }

    /**
     * Touch.
     */
    private final synchronized void touch() {
        touchTimestamp = Time.absoluteTime();
        start();
    }

    /**
     * Start.
     */
    final synchronized void start() {
        if (!active) {
            startStatistic();
            history.startSampling();
            active = true;
        }
    }

    /**
     * Expire.
     *
     * @param expiry the expiry
     * @return true, if successful
     */
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

    /**
     * Sets the history.
     *
     * @param historySize the history size
     * @param historyNanos the history nanos
     */
    final void setHistory(int historySize, long historyNanos) {
        history.adjust(historySize, historyNanos);
    }

    /**
     * Stop statistic.
     */
    abstract void stopStatistic();

    /**
     * Start statistic.
     */
    abstract void startStatistic();

    /**
     * Read statistic.
     *
     * @return the t
     */
    abstract T readStatistic();
}
