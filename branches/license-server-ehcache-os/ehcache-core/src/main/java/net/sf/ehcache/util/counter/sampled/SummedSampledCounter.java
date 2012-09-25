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


/**
 * Derived sample counter that sums two counters.
 *
 * @author cschanck
 *
 */
public class SummedSampledCounter extends DerivedSampledCounterImpl {

    private final SampledCounter[] delegates;

    /**
     * Create a derived counter that functions as as sum of two counters.
     *
     * @param sc1
     * @param sc2
     */
    public SummedSampledCounter(SampledCounterConfig config, SampledCounter... delegates) {
        super(config);
        this.delegates = delegates;
        recordSample();
    }

    @Override
    public long getValue() {
        long l = 0;
        for (SampledCounter sc : delegates) {
            l = l + sc.getValue();
        }
        return l;
    }

}