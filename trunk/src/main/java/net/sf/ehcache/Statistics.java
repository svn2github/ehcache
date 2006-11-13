/**
 *  Copyright 2003-2006 Greg Luck
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

package net.sf.ehcache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;

/**
 * An immutable Cache statistics implementation}
 * <p/>
 * This is like a value object, with the added ability to clear cache statistics on the cache.
 * That ability does not survive any Serialization of this class. On deserialization the cache
 * can be considered disconnected.
 * <p/>
 * The accuracy of these statistics are determined by the value of {#getStatisticsAccuracy()}
 * at the time the statistic was computed. This can be changed by setting {@link Cache#setStatisticsAccuracy}.
 * <p/>
 * Because this class maintains a reference to an Ehcache, any references held to this class will precent the Ehcache
 * from getting garbage collected.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class Statistics implements Serializable {

    /**
     * Best efforts accuracy setting.
     */
    public static final int STATISTICS_ACCURACY_BEST_EFFORT = 0;

    /**
     * Guaranteed accuracy setting.
     */
    public static final int STATISTICS_ACCURACY_GUARANTEED = 1;

    /**
      * Fast but not accurate setting.
      */
     public static final int STATISTICS_ACCURACY_NONE = 2;


    private static final Log LOG = LogFactory.getLog(Statistics.class.getName());

    private static final long serialVersionUID = 3606940454221918725L;

    private transient Ehcache cache;

    private final int statisticsAccuracy;

    private final long cacheHits;

    private final long onDiskHits;

    private final long inMemoryHits;

    private final long misses;

    private final long size;


    /**
     * Creates a new statistics object, associated with a Cache
     *
     * @param cache The cache that {@link #clearStatistics()} will call, if not disconnected
     * @param statisticsAccuracy
     * @param cacheHits
     * @param onDiskHits
     * @param inMemoryHits
     * @param misses
     * @param size
     */
    public Statistics(Ehcache cache, int statisticsAccuracy, long cacheHits, long onDiskHits, long inMemoryHits,
                      long misses, long size) {
        this.statisticsAccuracy = statisticsAccuracy;
        this.cacheHits = cacheHits;
        this.onDiskHits = onDiskHits;
        this.inMemoryHits = inMemoryHits;
        this.misses = misses;
        this.cache = cache;
        this.size = size;
    }

    /**
     * Clears the statistic counters to 0 for the associated Cache.
     */

    public void clearStatistics() {
        if (cache == null) {
            throw new IllegalStateException("This statistics object no longer references a Cache.");
        }
        try {
            cache.clearStatistics();
        } catch (IllegalStateException e) {
            if (LOG.isDebugEnabled()) {
                LOG.info("Ignoring call because " + e.getMessage());
            }
        }
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
     * This result is returned when the statistics accuracy setting is {@link Statistics#STATISTICS_ACCURACY_BEST_EFFORT}.
     * <p/>
     * The size is the number of {@link Element}s in the {@link net.sf.ehcache.store.MemoryStore} plus
     * the number of {@link Element}s in the {@link net.sf.ehcache.store.DiskStore}.
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
     * This result is returned when the statistics accuracy setting is {@link Statistics#STATISTICS_ACCURACY_GUARANTEED}.
     * <p/>
     * This method accounts for elements which might be expired or duplicated between stores. It take approximately
     * 200ms per 1000 elements to execute.
     * <h3>Fast but non-accurate Size</h3>
     * This result is returned when the statistics accuracy setting is {@link #STATISTICS_ACCURACY_NONE}.
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
     * @return one of {@link #STATISTICS_ACCURACY_BEST_EFFORT}, {@link #STATISTICS_ACCURACY_GUARANTEED}, {@link #STATISTICS_ACCURACY_NONE}
     */
    public long getStatisticsAccuracy() {
        return statisticsAccuracy;
    }

    /**
     * @return the name of the Ehcache, or null is there no associated cache
     */
    public String getAssociatedCacheName() {
        if (cache != null) {
            return cache.getName();
        } else {
            return null;
        }
    }

    /**
     * Returns a {@link String} representation of the {@link Ehcache} statistics.
     */
    public final String toString() {
        StringBuffer dump = new StringBuffer();

        dump.append("[ ")
                .append(" name = ").append(getAssociatedCacheName())
                .append(" cacheHits = ").append(cacheHits)
                .append(" onDiskHits = ").append(onDiskHits)
                .append(" inMemoryHits = ").append(inMemoryHits)
                .append(" misses = ").append(misses)
                .append(" size = ").append(size)
                .append(" ]");

        return dump.toString();
    }

}
