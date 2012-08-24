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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract based class for derived sampled counters, i.e., counters whose values
 * are entirely calculated from other counters.
 *
 * @author cschanck
 *
 */
public abstract class DerivedSampledCounterImpl extends SampledCounterImpl {

    /**
     * Should we log mutation operations? Used for debugging.
     */
    protected static final boolean DERIVED_MUTATION_LOG = false;

    /**
     * Log
     */
    private static final Logger LOG = LoggerFactory.getLogger(DerivedSampledCounterImpl.class.getName());

    /**
     * Created a derived sampled counter based on a sampled config. No initial
     * sample is recorded yet, since subclasses of this class may not be prepared to be sampled
     * yet.
     *
     * @param config
     */
    public DerivedSampledCounterImpl(SampledCounterConfig config) {
        super(config.getIntervalSecs() , config.getHistorySize(), config.isResetOnSample(), config.getInitialValue(), false);
    }

    @Override
    public final void setValue(long newValue) {
        getAndSet(newValue);
    }

    @Override
    public final long increment(long amount) {
        if (DERIVED_MUTATION_LOG) {
            LOG.warn("Cannot increment value on a derived counter.");
        }
        return getValue();
    }

    @Override
    public final long increment() {
        return increment(1);
    }

    @Override
    public final long getAndSet(long newValue) {
        if (DERIVED_MUTATION_LOG) {
            LOG.warn("Cannot set/getAndSet value on a derived counter.");
        }
        return getValue();
    }

    @Override
    public final long decrement(long amount) {
        if (DERIVED_MUTATION_LOG) {
            LOG.warn("Cannot decrement value on a derived counter.");
        }
        return getValue();
    }

    @Override
    public final long decrement() {
        return decrement(1);
    }

    @Override
    public final long getAndReset() {
        if (DERIVED_MUTATION_LOG) {
            LOG.warn("Cannot reset value on a derived counter.");
        }
        return getValue();
    }

    @Override
    public abstract long getValue();

}
