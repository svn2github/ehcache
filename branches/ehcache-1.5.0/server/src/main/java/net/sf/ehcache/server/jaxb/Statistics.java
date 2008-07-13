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

package net.sf.ehcache.server.jaxb;

/**
 * An immutable Cache statistics implementation.
 * <p/>
 * The accuracy of these statistics are determined by the value of {#getStatisticsAccuracy()}
 * at the time the statistic was computed. This can be changed by setting {@link net.sf.ehcache.Cache#setStatisticsAccuracy}.
 * <p/>
 * Because this class maintains a reference to an Ehcache, any references held to this class will precent the Ehcache
 * from getting garbage collected.
 * <p/>
 * The {@link net.sf.ehcache.server.jaxb.StatisticsAccuracy#STATISTICS_ACCURACY_BEST_EFFORT},
 * {@link net.sf.ehcache.server.jaxb.StatisticsAccuracy#STATISTICS_ACCURACY_GUARANTEED} and
 *  {@link net.sf.ehcache.server.jaxb.StatisticsAccuracy#STATISTICS_ACCURACY_NONE}
 * constants have the same values as in JSR107.
 * <p/>
 * @author Greg Luck
 * @version $Id$
 */
public class Statistics {


    private StatisticsAccuracy statisticsAccuracy;

    private long cacheHits;

    private long onDiskHits;

    private long inMemoryHits;

    private long misses;

    private float averageGetTime;

    private long evictionCount;

    private long size;


    /**
     * Empty constructor to create a new statistics object.
     */
    public Statistics() {
        //
    }


    /**
     * Creates a new statistics object.
     *
     * @param statistics the Ehcache core {@link net.sf.ehcache.Statistics} object.
     */
    public Statistics(net.sf.ehcache.Statistics statistics) {
        statisticsAccuracy = StatisticsAccuracy.fromCode(statistics.getStatisticsAccuracy());
        cacheHits = statistics.getCacheHits();
        onDiskHits = statistics.getOnDiskHits();
        inMemoryHits = statistics.getInMemoryHits();
        misses = statistics.getCacheMisses();
        averageGetTime = statistics.getAverageGetTime();
        evictionCount = statistics.getEvictionCount();
        size = statistics.getObjectCount();
    }

    /**
     * The number of times a requested item was found in the cache.
     *
     * @return the number of times a requested item was found in the cache
     */
    public long getCacheHits() {
        return cacheHits;
    }

    /**
     * Number of times a requested item was found in the Memory Store.
     *
     * @return the number of times a requested item was found in memory
     */
    public long getInMemoryHits() {
        return inMemoryHits;
    }

    /**
     * Number of times a requested item was found in the Disk Store.
     *
     * @return the number of times a requested item was found on Disk, or 0 if there is no disk storage configured.
     */
    public long getOnDiskHits() {
        return onDiskHits;
    }

    /**
     * @return the number of times a requested element was not found in the cache
     */
    public long getCacheMisses() {
        return misses;

    }

    /**
     * Gets the number of elements stored in the cache. Caclulating this can be expensive. Accordingly,
     * this method will return three different values, depending on the statistics accuracy setting.
     * <h3>Best Effort Size</h3>
     * This result is returned when the statistics accuracy setting is {@link StatisticsAccuracy#STATISTICS_ACCURACY_BEST_EFFORT}.
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
     * This result is returned when the statistics accuracy setting is {@link StatisticsAccuracy#STATISTICS_ACCURACY_GUARANTEED}.
     * <p/>
     * This method accounts for elements which might be expired or duplicated between stores. It take approximately
     * 200ms per 1000 elements to execute.
     * <h3>Fast but non-accurate Size</h3>
     * This result is returned when the statistics accuracy setting is {@link StatisticsAccuracy#STATISTICS_ACCURACY_NONE}.
     * <p/>
     * The number given may contain expired elements. In addition if the DiskStore is used it may contain some double
     * counting of elements. It takes 6ms for 1000 elements to execute. Time to execute is O(log n). 50,000 elements take
     * 36ms.
     *
     * @return the number of elements in the ehcache, with a varying degree of accuracy, depending on accuracy setting.
     */
    public long getObjectCount() {
        return size;
    }

    /**
     * Accurately measuring statistics can be expensive. Returns the current accuracy setting.
     *
     * @return one of {@link StatisticsAccuracy#STATISTICS_ACCURACY_BEST_EFFORT},
     * {@link StatisticsAccuracy#STATISTICS_ACCURACY_GUARANTEED}, {@link StatisticsAccuracy#STATISTICS_ACCURACY_NONE}
     */
    public StatisticsAccuracy getStatisticsAccuracy() {
        return statisticsAccuracy;
    }

    /**
     * The average get time. Because ehcache support JDK1.4.2, each get time uses
     * System.currentTimeMilis, rather than nanoseconds. The accuracy is thus limited.
     */
    public float getAverageGetTime() {
        return averageGetTime;
    }

    /**
     * Gets the number of cache evictions, since the cache was created, or statistics were cleared.
     */
    public long getEvictionCount() {
        return evictionCount;
    }

    /**
     *
     * @param statisticsAccuracy
     */
    public void setStatisticsAccuracy(StatisticsAccuracy statisticsAccuracy) {
        this.statisticsAccuracy = statisticsAccuracy;
    }

    /**
     *
     * @param cacheHits
     */
    public void setCacheHits(long cacheHits) {
        this.cacheHits = cacheHits;
    }

    /**
     *
     * @param onDiskHits
     */
    public void setOnDiskHits(long onDiskHits) {
        this.onDiskHits = onDiskHits;
    }

    /**
     *
     * @param inMemoryHits
     */
    public void setInMemoryHits(long inMemoryHits) {
        this.inMemoryHits = inMemoryHits;
    }

    /**
     *
     * @param misses
     */
    public void setMisses(long misses) {
        this.misses = misses;
    }

    /**
     *
     * @param averageGetTime
     */
    public void setAverageGetTime(float averageGetTime) {
        this.averageGetTime = averageGetTime;
    }

    /**
     *
     * @param evictionCount
     */
    public void setEvictionCount(long evictionCount) {
        this.evictionCount = evictionCount;
    }

    /**
     *
     * @param size
     */
    public void setSize(long size) {
        this.size = size;
    }
}
