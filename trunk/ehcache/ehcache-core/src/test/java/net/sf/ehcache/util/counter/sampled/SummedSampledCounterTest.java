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

package net.sf.ehcache.util.counter.sampled;

import org.junit.Assert;
import org.junit.Test;

public class SummedSampledCounterTest {

    @Test
    public void testSummed() {

        SampledCounterConfig configSec = new SampledCounterConfig(1, 10, true, 0);
        SampledCounterImpl counter1 = new SampledCounterImpl(configSec);
        SampledCounterImpl counter2 = new SampledCounterImpl(configSec);
        SampledCounterImpl counter3 = new SampledCounterImpl(configSec);

        SummedSampledCounter summedCounter = new SummedSampledCounter(configSec, counter1, counter2, counter3);

        counter1.setValue(0);
        counter2.setValue(0);
        counter3.setValue(0);

        Assert.assertEquals(0, summedCounter.getValue());
        counter1.setValue(10);
        Assert.assertEquals(10, summedCounter.getValue());

        summedCounter.setValue(-10);
        Assert.assertEquals(10, summedCounter.getValue());
        Assert.assertEquals(10, summedCounter.getAndReset());
        Assert.assertEquals(10, summedCounter.getValue());

        counter2.setValue(20);
        counter3.setValue(30);

        summedCounter.recordSample();

        counter1.getAndReset();
        counter2.getAndReset();
        counter3.getAndReset();

        summedCounter.recordSample();

        System.out.println("summed");
        for (TimeStampedCounterValue tscv : summedCounter.getAllSampleValues()) {
            System.out.println("\t" + tscv);
        }

        Assert.assertEquals(0, summedCounter.getMostRecentSample().getCounterValue());
        Assert.assertEquals(0, summedCounter.getAllSampleValues()[0].getCounterValue());
        Assert.assertEquals(60, summedCounter.getAllSampleValues()[1].getCounterValue());
        Assert.assertEquals(0, summedCounter.getAllSampleValues()[2].getCounterValue());

    }

    @Test
    public void testNoops() {
        SampledCounterConfig configSec = new SampledCounterConfig(1, 10, true, 0);
        SampledCounterImpl counter1 = new SampledCounterImpl(configSec);
        SampledCounterImpl counter2 = new SampledCounterImpl(configSec);
        SampledCounterImpl counter3 = new SampledCounterImpl(configSec);

        SummedSampledCounter summedCounter = new SummedSampledCounter(configSec, counter1, counter2, counter3);
        counter1.setValue(10);
        counter2.setValue(10);
        counter3.setValue(10);

        summedCounter.decrement();
        summedCounter.decrement(10);
        summedCounter.increment();
        summedCounter.increment(10);
        summedCounter.setValue(100);
        summedCounter.getAndReset();
        Assert.assertEquals(30, summedCounter.getValue());
    }
}