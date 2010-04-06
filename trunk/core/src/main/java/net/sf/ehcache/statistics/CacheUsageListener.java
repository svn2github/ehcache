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

/**
 * Interface for listeners to any change in usage statistics of an
 * Ehcache.
 * 
 * <p />
 * Implementations of this interface should implement the {@link Object#equals(Object)} and the {@link Object#hashCode()} as registering and
 * removing listeners depends on these
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public interface CacheUsageListener {

    /**
     * Called when statistics is enabled/disabled
     * 
     * @param enableStatistics
     */
    public void notifyStatisticsEnabledChanged(boolean enableStatistics);

    /**
     * Called when statistics is cleared
     */
    public void notifyStatisticsCleared();

    /**
     * Called on a cache hit in the MemoryStore
     */
    public void notifyCacheHitInMemory();

    /**
     * Called on a cache hit in the DiskStore
     */
    public void notifyCacheHitOnDisk();

    /**
     * Called when an element is inserted in the cache
     */
    public void notifyCacheElementPut();

    /**
     * Called when an element is updated in the cache, i.e. a put for an already
     * existing key
     */
    public void notifyCacheElementUpdated();

    /**
     * Called when an element is not found in the cache
     */
    public void notifyCacheMissedWithNotFound();

    /**
     * Called when an element is found in the cache but already expired
     */
    public void notifyCacheMissedWithExpired();

    /**
     * Notified with time taken for a get operation in the cache
     * 
     * @param millis
     */
    public void notifyTimeTakenForGet(final long millis);

    /**
     * Called when an element is expired in the cache
     */
    public void notifyCacheElementEvicted();

    /**
     * Called when an element in the cache expires
     */
    public void notifyCacheElementExpired();

    /**
     * Called when an element is removed from the cache
     */
    public void notifyCacheElementRemoved();

    /**
     * Called when Cache.removeAll() is called
     */
    public void notifyRemoveAll();

    /**
     * Notified when the statistics accuracy is changed.
     * 
     * @param statisticsAccuracy
     *            one of Statistics#STATISTICS_ACCURACY_BEST_EFFORT,
     *            Statistics#STATISTICS_ACCURACY_GUARANTEED,
     *            Statistics#STATISTICS_ACCURACY_NONE
     */
    public void notifyStatisticsAccuracyChanged(int statisticsAccuracy);

    /**
     * Called to dispose off the listener
     */
    public void dispose();
}
