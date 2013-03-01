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

package net.sf.ehcache.util.ratestatistics;

import java.util.concurrent.TimeUnit;

/**
 * Abstract rate statistic implementation.
 * <p>
 * Provides exponentially decaying moving average functionality to subclasses.
 *
 * @author Chris Dennis
 */
abstract class AbstractRateStatistic implements RateStatistic {

    private final long rateAveragePeriod;

    /**
     * Create an abstract statistic using the specified time averaging period.
     *
     * @param averagePeriod average period
     * @param unit period time unit
     */
    AbstractRateStatistic(long averagePeriod, TimeUnit unit) {
        this.rateAveragePeriod = unit.toNanos(averagePeriod);
    }

    /**
     * Returns the time averaging period in nanoseconds.
     *
     * @return average period
     */
    long getRateAveragePeriod() {
        return rateAveragePeriod;
    }

    /**
     * Combines two timestamped values using a exponentially decaying weighted average.
     *
     * @param nowValue current value
     * @param now current value timestamp
     * @param thenAverage previous value
     * @param then previous value timestamp
     * @return weighted average
     */
    float iterateMovingAverage(float nowValue, long now, float thenAverage, long then) {
        if (getRateAveragePeriod() == 0) {
            return nowValue;
        } else {
            float alpha = (float) alpha(now, then);
            return alpha * nowValue + (1 - alpha) * thenAverage;
        }
    }

    private double alpha(long now, long then) {
        return -Math.expm1(-((double) (now - then)) / getRateAveragePeriod());
    }
}
