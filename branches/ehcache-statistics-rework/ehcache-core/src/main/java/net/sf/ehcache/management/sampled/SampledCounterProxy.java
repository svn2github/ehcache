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

import org.terracotta.statistics.archive.Timestamped;

import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Statistic;
import net.sf.ehcache.util.counter.sampled.SampledCounter;
import net.sf.ehcache.util.counter.sampled.TimeStampedCounterValue;

public class SampledCounterProxy<E extends Number>implements SampledCounter {

    private final Statistic<E> rate;

    public SampledCounterProxy(Statistic<E> rate) {
        this.rate=rate;
    }

    @Override
    public TimeStampedCounterValue getMostRecentSample() {
        return new TimeStampedCounterValue(System.currentTimeMillis(),rate.value().longValue());
    }

    @Override
    public TimeStampedCounterValue[] getAllSampleValues() {
        ArrayList<TimeStampedCounterValue> arr=new ArrayList<TimeStampedCounterValue>();
        for(Timestamped<E> ts:rate.history()) {
            arr.add(new TimeStampedCounterValue(ts.getTimestamp(),ts.getSample().longValue()));
        }
        Collections.sort(arr,new Comparator<TimeStampedCounterValue>() {

            @Override
            public int compare(TimeStampedCounterValue o1, TimeStampedCounterValue o2) {
                return (int)(o1.getTimestamp()-o2.getTimestamp());
            }
        });
        return arr.toArray(new TimeStampedCounterValue[arr.size()]);
    }

    @Override
    public void setValue(long newValue) {
        throw new UnsupportedOperationException();
    }

    // NOOPS
    @Override
    public long increment() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long decrement() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getAndSet(long newValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long increment(long amount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long decrement(long amount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getAndReset() {
        throw new UnsupportedOperationException();
    }

}
