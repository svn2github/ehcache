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
import org.terracotta.statistics.OperationStatistic;
import org.terracotta.statistics.ValueStatistic;

/**
 * The Class CountStatistic.
 *
 * @author cdennis
 */
class CountStatistic extends AbstractStatistic<Long> {

    private final ValueStatistic<Long> statistic;

    /**
     * Instantiates a new count statistic.
     *
     * @param <T> the generic type
     * @param source the source
     * @param targets the targets
     * @param executor the executor
     * @param historySize the history size
     * @param historyNanos the history nanos
     */
    public <T extends Enum<T>> CountStatistic(OperationStatistic<T> source, Set<T> targets, ScheduledExecutorService executor,
            int historySize, long historyNanos) {
        super(executor, historySize, historyNanos);
        this.statistic = source.statistic(targets);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.AbstractStatistic#value()
     */
    @Override
    public Long value() {
        // reading the value doesn't touch the statistic
        return readStatistic();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.AbstractStatistic#stopStatistic()
     */
    @Override
    void stopStatistic() {
        // no-op
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.AbstractStatistic#startStatistic()
     */
    @Override
    void startStatistic() {
        // no-op
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.extended.AbstractStatistic#readStatistic()
     */
    @Override
    Long readStatistic() {
        return statistic.value();
    }
}
