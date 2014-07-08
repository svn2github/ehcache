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

package net.sf.ehcache.util.counter;

/**
 * A Counter Manager that accepts a config to create counters. Creates counter's
 * based on {@link CounterConfig}. This manages the lifycycle of a counter
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 *
 */
public interface CounterManager {
    /**
     * Creates a Counter based on the passed config
     *
     * @param config
     * @return The counter created and managed by this CounterManager
     */
    Counter createCounter(CounterConfig config);

    /**
     * Adds a counter.
     */
    void addCounter(Counter counter);

    /**
     * Shuts down this counter manager
     */
    void shutdown();

    /**
     * Shuts down the counter
     *
     * @param counter
     */
    void shutdownCounter(Counter counter);

}
