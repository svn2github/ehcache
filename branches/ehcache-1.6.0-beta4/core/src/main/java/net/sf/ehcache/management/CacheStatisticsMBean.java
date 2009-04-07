/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

package net.sf.ehcache.management;

/**
 * @author Greg Luck
 * @version $Id$
 * @since 1.3
 */
public interface CacheStatisticsMBean {

    /**
     * Clears the statistic counters to 0 for the associated Cache.
     */
    public void clearStatistics();

    /**
     * The number of times a requested item was found in the cache.
     *
     * @return the number of times a requested item was found in the cache
     */
    public long getCacheHits();

    /**
     * Number of times a requested item was found in the Memory Store.
     *
     * @return the number of times a requested item was found in memory
     */
    public long getInMemoryHits();

    /**
     * Number of times a requested item was found in the Disk Store.
     *
     * @return the number of times a requested item was found on Disk, or 0 if there is no disk storage configured.
     */
    public long getOnDiskHits();

    /**
     * @return the number of times a requested element was not found in the cache
     */
    public long getCacheMisses();

    /**
     * Gets the number of elements stored in the cache. Caclulating this can be expensive. Accordingly,
     * this method will return three different values, depending on the statistics accuracy setting.
     * <h3>Best Effort Size</h3>
     * This result is returned when the statistics accuracy setting is {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_BEST_EFFORT}.
     * <p/>
     * The size is the number of {@link net.sf.ehcache.Element}s in the {@link net.sf.ehcache.store.MemoryStore} plus
     * the number of {@link net.sf.ehcache.Element}s in the {@link net.sf.ehcache.store.DiskStore}.
     * <p/>
     * This number is the actual number of elements, including expired elements that have
     * not been removed. Any duplicates between stores are accounted for.
     * <p/>
     * Expired elements are removed from the the memory store when
     * getting an expired element, or when attempting to spool an expired element to
     * disk.
     * <p/>
     * Expired elements are removed from the disk store when getting an expired element,
     * or when the expiry thread runs, which is once every five minutes.
     * <p/>
     * <h3>Guaranteed Accuracy Size</h3>
     * This result is returned when the statistics accuracy setting is {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_GUARANTEED}.
     * <p/>
     * This method accounts for elements which might be expired or duplicated between stores. It take approximately
     * 200ms per 1000 elements to execute.
     * <h3>Fast but non-accurate Size</h3>
     * This result is returned when the statistics accuracy setting is {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_NONE}.
     * <p/>
     * The number given may contain expired elements. In addition if the DiskStore is used it may contain some double
     * counting of elements. It takes 6ms for 1000 elements to execute. Time to execute is O(log n). 50,000 elements take
     * 36ms.
     *
     * @return the number of elements in the ehcache, with a varying degree of accuracy, depending on accuracy setting.
     */
    public long getObjectCount();


    /**
     * Gets the number of objects in the MemoryStore
     * @return the MemoryStore size which is always a count unadjusted for duplicates or expiries
     */
    public long getMemoryStoreObjectCount();

    /**
     * Gets the number of objects in the DiskStore
     * @return the DiskStore size which is always a count unadjusted for duplicates or expiries
     */
    public long getDiskStoreObjectCount();

    /**
     * Accurately measuring statistics can be expensive. Returns the current accuracy setting.
     *
     * @return one of {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_BEST_EFFORT},
     *         {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_GUARANTEED},
     *         {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_NONE}
     */
    public int getStatisticsAccuracy();


    /**
     * Accurately measuring statistics can be expensive. Returns the current accuracy setting.
     * @return a human readable description of the accuracy setting. One of "None", "Best Effort" or "Guaranteed".
     */
    public String getStatisticsAccuracyDescription();

    /**
     * @return the name of the Ehcache, or null is there no associated cache
     */
    public String getAssociatedCacheName();

}
