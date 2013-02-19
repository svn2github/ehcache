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

package net.sf.ehcache.management.sampled;

import net.sf.ehcache.statistics.extended.ExtendedStatistics.Statistic;
import net.sf.ehcache.util.counter.sampled.SampledRateCounter;

/**
 * The Class SampledRateCounterProxy.
 *
 * @param <E> the element type
 *
 * @author cschanck
 */
public class SampledRateCounterProxy<E extends Number> extends SampledCounterProxy<E> implements SampledRateCounter {

    /**
     * Instantiates a new sampled rate counter proxy.
     *
     * @param rate the rate
     */
    public SampledRateCounterProxy(Statistic<E> rate) {
        super(rate);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.util.counter.sampled.SampledRateCounter#increment(long, long)
     */
    @Override
    public void increment(long numerator, long denominator) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.util.counter.sampled.SampledRateCounter#decrement(long, long)
     */
    @Override
    public void decrement(long numerator, long denominator) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.util.counter.sampled.SampledRateCounter#setValue(long, long)
     */
    @Override
    public void setValue(long numerator, long denominator) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.util.counter.sampled.SampledRateCounter#setNumeratorValue(long)
     */
    @Override
    public void setNumeratorValue(long newValue) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.util.counter.sampled.SampledRateCounter#setDenominatorValue(long)
     */
    @Override
    public void setDenominatorValue(long newValue) {
        throw new UnsupportedOperationException();
    }

}
