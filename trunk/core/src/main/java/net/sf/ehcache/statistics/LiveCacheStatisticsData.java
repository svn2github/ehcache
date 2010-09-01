/**
 *  Copyright 2003-2010 Terracotta, Inc.
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
package net.sf.ehcache.statistics;

import net.sf.ehcache.event.CacheEventListener;

/**
 * Interface that classes storing usage statistics of a Cache will implement
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public interface LiveCacheStatisticsData extends CacheEventListener {

    /**
     * Enabled/Disabled statistics
     * 
     * @param enableStatistics
     */
    public void setStatisticsEnabled(final boolean enableStatistics);

    /**
     * Clear existing statistics
     */
    public void clearStatistics();

    /**
     * Called on a cache hit in the MemoryStore
     */
    public void cacheHitInMemory();

    /**
     * Called on a cache hit in the off-heap
     */
    public void cacheHitOffHeap();

    /**
     * Called on a cache hit in the DiskStore
     */
    public void cacheHitOnDisk();

    /**
     * Called when an element is not found in the cache
     */
    public void cacheMissNotFound();

    /**
     * Called when an element is found in the cache but already expired
     */
    public void cacheMissExpired();

    /**
     * Adds time taken for a get operation in the cache
     * 
     * @param millis
     */
    public void addGetTimeMillis(final long millis);

    /**
     * Sets the statistics accuracy.
     * 
     * @param statisticsAccuracy
     *            one of Statistics#STATISTICS_ACCURACY_BEST_EFFORT,
     *            Statistics#STATISTICS_ACCURACY_GUARANTEED,
     *            Statistics#STATISTICS_ACCURACY_NONE
     */
    public void setStatisticsAccuracy(int statisticsAccuracy);

    /**
     * Registers a {@link CacheUsageListener} which will be notified of the
     * cache
     * usage.
     * Implementations of {@link CacheUsageListener} should override the {@link Object#equals(Object)} and {@link Object#hashCode()} methods
     * as it is used for
     * equality check
     * 
     * @throws IllegalStateException
     * @since 1.7
     */
    void registerCacheUsageListener(CacheUsageListener cacheUsageListener) throws IllegalStateException;

    /**
     * Remove an already registered {@link CacheUsageListener}, if any.
     * Depends on the {@link Object#equals(Object)} method.
     * 
     * @throws IllegalStateException
     * @since 1.7
     */
    void removeCacheUsageListener(CacheUsageListener cacheUsageListener) throws IllegalStateException;

}
