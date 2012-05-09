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

/**
 * An MBean for Cache exposing cache statistics.
 * Extends from both {@link CacheSampler}
 * <p/>
 * <p/>
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 */
public interface SampledCacheMBean extends CacheSampler {
    /**
     * CACHE_ENABLED
     */
    final String CACHE_ENABLED = "CacheEnabled";

    /**
     * CACHE_CHANGED
     */
    final String CACHE_CHANGED = "CacheChanged";

    /**
     * CACHE_FLUSHED
     */
    final String CACHE_FLUSHED = "CacheFlushed";

    /**
     * CACHE_CLEARED
     */
    final String CACHE_CLEARED = "CacheCleared";

    /**
     * CACHE_STATISTICS_ENABLED
     */
    final String CACHE_STATISTICS_ENABLED = "CacheStatisticsEnabled";

    /**
     * CACHE_STATISTICS_RESET
     */
    final String CACHE_STATISTICS_RESET = "CacheStatisticsReset";

    /**
     * Enabled/disable cache coherence mode for this node.
     *
     * @deprecated use {@link #setNodeBulkLoadEnabled(boolean)} instead
     */
    @Deprecated
    void setNodeCoherent(boolean coherent);

    /**
     * Is the cache coherent cluster-wide?
     *
     * @deprecated use {@link #isClusterBulkLoadEnabled()} instead
     */
    @Deprecated
    boolean isClusterCoherent();

    /**
     * Is the cache coherent locally?
     *
     * @deprecated use {@link #isNodeBulkLoadEnabled()} instead
     */
    @Deprecated
    boolean isNodeCoherent();

    /**
     * Configuration property accessor
     *
     * @return Max elements in memory config setting value
     * @deprecated use {@link #getMaxEntriesLocalHeap()} instead
     */
    @Deprecated
    int getMaxElementsInMemory();

    /**
     * setMaxElementsInMemory
     *
     * @param maxElements
     * @deprecated use {@link #setMaxEntriesLocalHeap()} instead
     */
    @Deprecated
    void setMaxElementsInMemory(int maxElements);
}