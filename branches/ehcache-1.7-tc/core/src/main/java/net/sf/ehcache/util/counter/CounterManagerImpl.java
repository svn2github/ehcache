/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.util.counter;

import java.util.Timer;

import net.sf.ehcache.util.counter.sampled.SampledCounter;
import net.sf.ehcache.util.counter.sampled.SampledCounterImpl;

/**
 * An implementation of a {@link CounterManager}.
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 * 
 */
public class CounterManagerImpl implements CounterManager {
    private final Timer timer = new Timer("SampledCounterManager Timer", true);
    private boolean shutdown;

    /**
     * Default Constructor
     */
    public CounterManagerImpl() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void shutdown() {
        if (shutdown) {
            return;
        }
        try {
            timer.cancel();
        } finally {
            shutdown = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Counter createCounter(CounterConfig config) {
        if (shutdown) {
            throw new IllegalStateException("counter manager is shutdown");
        }
        if (config == null) {
            throw new NullPointerException("config cannot be null");
        }

        Counter counter = config.createCounter();
        if (counter instanceof SampledCounterImpl) {
            SampledCounterImpl sampledCounter = (SampledCounterImpl) counter;
            timer.schedule(sampledCounter.getTimerTask(), sampledCounter
                    .getIntervalMillis(), sampledCounter.getIntervalMillis());
        }
        return counter;

    }

    /**
     * {@inheritDoc}
     */
    public void shutdownCounter(Counter counter) {
        if (counter instanceof SampledCounter) {
            SampledCounter sc = (SampledCounter) counter;
            sc.shutdown();
        }
    }

}
