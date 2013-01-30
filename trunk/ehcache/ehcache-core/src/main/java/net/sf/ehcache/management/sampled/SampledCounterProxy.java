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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sf.ehcache.statistics.extended.ExtendedStatistics.Statistic;
import net.sf.ehcache.util.counter.sampled.SampledCounter;
import net.sf.ehcache.util.counter.sampled.TimeStampedCounterValue;

import org.terracotta.statistics.archive.Timestamped;

/**
 * The Class SampledCounterProxy.
 * 
 * @param <E> the element type
 * @author cschanck
 */
public class SampledCounterProxy<E extends Number> implements SampledCounter {

    /** The rate. */
    protected final Statistic<E> rate;

    /**
     * Instantiates a new sampled counter proxy.
     * 
     * @param rate the rate
     */
    public SampledCounterProxy(Statistic<E> rate) {
        this.rate = rate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.util.counter.sampled.SampledCounter#getMostRecentSample()
     */
    @Override
    public TimeStampedCounterValue getMostRecentSample() {
        return new TimeStampedCounterValue(System.currentTimeMillis(), rate.value().longValue());
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.util.counter.sampled.SampledCounter#getAllSampleValues()
     */
    @Override
    public TimeStampedCounterValue[] getAllSampleValues() {
        ArrayList<TimeStampedCounterValue> arr = new ArrayList<TimeStampedCounterValue>();
        for (Timestamped<E> ts : rate.history()) {
            arr.add(new TimeStampedCounterValue(ts.getTimestamp(), ts.getSample().longValue()));
        }
        return sortAndPresent(arr);
    }

    /**
     * Sort and present the List of values
     * 
     * @param arr
     * @return
     */
    protected TimeStampedCounterValue[] sortAndPresent(List<TimeStampedCounterValue> arr) {
        Collections.sort(arr, new Comparator<TimeStampedCounterValue>() {

            @Override
            public int compare(TimeStampedCounterValue o1, TimeStampedCounterValue o2) {
                return (int) (o1.getTimestamp() - o2.getTimestamp());
            }
        });
        return arr.toArray(new TimeStampedCounterValue[arr.size()]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.util.counter.Counter#setValue(long)
     */
    @Override
    public void setValue(long newValue) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.util.counter.Counter#increment()
     */
    @Override
    public long increment() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.util.counter.Counter#decrement()
     */
    @Override
    public long decrement() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.util.counter.Counter#getAndSet(long)
     */
    @Override
    public long getAndSet(long newValue) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.util.counter.Counter#getValue()
     */
    @Override
    public long getValue() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.util.counter.Counter#increment(long)
     */
    @Override
    public long increment(long amount) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.util.counter.Counter#decrement(long)
     */
    @Override
    public long decrement(long amount) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.util.counter.sampled.SampledCounter#shutdown()
     */
    @Override
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.util.counter.sampled.SampledCounter#getAndReset()
     */
    @Override
    public long getAndReset() {
        throw new UnsupportedOperationException();
    }

}
