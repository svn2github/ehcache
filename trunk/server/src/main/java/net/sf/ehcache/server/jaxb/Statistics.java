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

package net.sf.ehcache.server.jaxb;

import net.sf.ehcache.statistics.StatisticsGateway;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * An immutable Cache statistics implementation.
 * <p/>
 * Because this class maintains a reference to an Ehcache, any references held to this class will precent the Ehcache
 * from getting garbage collected.
 * <p/>
 * The {@link net.sf.ehcache.server.jaxb.StatisticsAccuracy#STATISTICS_ACCURACY_BEST_EFFORT},
 * {@link net.sf.ehcache.server.jaxb.StatisticsAccuracy#STATISTICS_ACCURACY_GUARANTEED} and
 * {@link net.sf.ehcache.server.jaxb.StatisticsAccuracy#STATISTICS_ACCURACY_NONE}
 * constants have the same values as in JSR107.
 * <p/>
 *
 * @author Greg Luck
 */
@XmlRootElement
public class Statistics {

    private long cacheHits;

    private long onDiskHits;

    private long inMemoryHits;

    private long misses;

    private long evictionCount;

    private long size;

    private long memoryStoreSize;

    private long diskStoreSize;


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
    public Statistics(StatisticsGateway statistics) {
        cacheHits = statistics.cacheHitCount();
        onDiskHits = statistics.localDiskHitCount();
        inMemoryHits = statistics.localHeapHitCount();
        misses = statistics.cacheMissCount();
        evictionCount = statistics.cacheEvictedCount();
        size = statistics.getSize();
        memoryStoreSize = statistics.getLocalHeapSize();
        diskStoreSize = statistics.getLocalDiskSize();
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
     * The size is the number of net.sf.ehcache.Elements in the net.sf.ehcache.store.MemoryStore plus
     * the number of net.sf.ehcache.Elements in the net.sf.ehcache.store.DiskStore.
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
     * @return Gets the number of cache evictions, since the cache was created, or statistics were cleared.
     */
    public long getEvictionCount() {
        return evictionCount;
    }

    /**
     * @param cacheHits setter
     */
    public void setCacheHits(long cacheHits) {
        this.cacheHits = cacheHits;
    }

    /**
     * @param onDiskHits setter
     */
    public void setOnDiskHits(long onDiskHits) {
        this.onDiskHits = onDiskHits;
    }

    /**
     * @param inMemoryHits setter
     */
    public void setInMemoryHits(long inMemoryHits) {
        this.inMemoryHits = inMemoryHits;
    }

    /**
     * @param misses setter
     */
    public void setMisses(long misses) {
        this.misses = misses;
    }

    /**
     * @param evictionCount setter
     */
    public void setEvictionCount(long evictionCount) {
        this.evictionCount = evictionCount;
    }

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
    public long getSize() {
        return size;
    }

    /**
     * Warning. This statistic is recorded as a long. If the statistic is large than Integer#MAX_VALUE
     * precision will be lost.
     *
     * @return the number of times a requested element was not found in the cache
     */
    public long getMisses() {
        return misses;
    }

    /**
     * @param size setter
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Gets the number of objects in the MemoryStore
     * @return the MemoryStore size which is always a count unadjusted for duplicates or expiries
     */
    public long getMemoryStoreSize() {
        return memoryStoreSize;
    }

    /**
     * @param memoryStoreSize setter
     */
    public void setMemoryStoreSize(long memoryStoreSize) {
        this.memoryStoreSize = memoryStoreSize;
    }

    /**
     * Gets the number of objects in the DiskStore
     * @return the DiskStore size which is always a count unadjusted for duplicates or expiries
     */
    public long getDiskStoreSize() {
        return diskStoreSize;
    }

    /**
     * @param diskStoreSize setter
     */
    public void setDiskStoreSize(long diskStoreSize) {
        this.diskStoreSize = diskStoreSize;
    }
}
