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

package net.sf.ehcache.statistics.extended;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

import org.terracotta.statistics.archive.Timestamped;

/**
 * The ExtendedStatistics interface.
 *
 * @author cschanck
 */
public interface ExtendedStatistics {

    /** The Constant ALL_CACHE_PUT_OUTCOMES. */
    static final Set<CacheOperationOutcomes.PutOutcome> ALL_CACHE_PUT_OUTCOMES = EnumSet.allOf(CacheOperationOutcomes.PutOutcome.class);

    /** The Constant ALL_CACHE_GET_OUTCOMES. */
    static final Set<CacheOperationOutcomes.GetOutcome> ALL_CACHE_GET_OUTCOMES = EnumSet.allOf(CacheOperationOutcomes.GetOutcome.class);

    /** The Constant ALL_CACHE_MISS_OUTCOMES. */
    static final Set<CacheOperationOutcomes.GetOutcome> ALL_CACHE_MISS_OUTCOMES = EnumSet.of(
            CacheOperationOutcomes.GetOutcome.MISS_EXPIRED, CacheOperationOutcomes.GetOutcome.MISS_NOT_FOUND);

    /** The Constant ALL_STORE_PUT_OUTCOMES. */
    static final Set<StoreOperationOutcomes.PutOutcome> ALL_STORE_PUT_OUTCOMES = EnumSet.allOf(StoreOperationOutcomes.PutOutcome.class);

    /**
     * Sets the time to disable.
     *
     * @param time the time
     * @param unit the unit
     */
    void setTimeToDisable(long time, TimeUnit unit);

    /**
     * Sets the always on.
     *
     * @param alwaysOn the new always on
     */
    void setAlwaysOn(boolean alwaysOn);

    /**
     * Gets the.
     *
     * @return the operation
     */
    Operation<CacheOperationOutcomes.GetOutcome> get();

    /**
     * Put.
     *
     * @return the operation
     */
    Operation<CacheOperationOutcomes.PutOutcome> put();

    /**
     * Removes the.
     *
     * @return the operation
     */
    Operation<CacheOperationOutcomes.RemoveOutcome> remove();

    /**
     * Heap get.
     *
     * @return the operation
     */
    Operation<StoreOperationOutcomes.GetOutcome> heapGet();

    /**
     * Offheap get.
     *
     * @return the operation
     */
    Operation<StoreOperationOutcomes.GetOutcome> offheapGet();

    /**
     * Disk get.
     *
     * @return the operation
     */
    Operation<StoreOperationOutcomes.GetOutcome> diskGet();

    /**
     * Heap put.
     *
     * @return the operation
     */
    Operation<StoreOperationOutcomes.PutOutcome> heapPut();

    /**
     * Offheap put.
     *
     * @return the operation
     */
    Operation<StoreOperationOutcomes.PutOutcome> offheapPut();

    /**
     * Disk put.
     *
     * @return the operation
     */
    Operation<StoreOperationOutcomes.PutOutcome> diskPut();

    /**
     * Heap remove.
     *
     * @return the operation
     */
    Operation<StoreOperationOutcomes.RemoveOutcome> heapRemove();

    /**
     * Offheap remove.
     *
     * @return the operation
     */
    Operation<StoreOperationOutcomes.RemoveOutcome> offheapRemove();

    /**
     * Disk remove.
     *
     * @return the operation
     */
    Operation<StoreOperationOutcomes.RemoveOutcome> diskRemove();

    /**
     * Search.
     *
     * @return the operation
     */
    Operation<CacheOperationOutcomes.SearchOutcome> search();

    /**
     * Xa commit.
     *
     * @return the operation
     */
    Operation<XaCommitOutcome> xaCommit();

    /**
     * Xa rollback.
     *
     * @return the operation
     */
    Operation<XaRollbackOutcome> xaRollback();

    /**
     * Xa recovery.
     *
     * @return the operation
     */
    Operation<XaRecoveryOutcome> xaRecovery();

    /**
     * Eviction.
     *
     * @return the operation
     */
    Operation<CacheOperationOutcomes.EvictionOutcome> eviction();

    /**
     * Expiry.
     *
     * @return the operation
     */
    Operation<CacheOperationOutcomes.ExpiredOutcome> expiry();

    /**
     * Cluster events
     * 
     * @return the operation
     */
    Operation<CacheOperationOutcomes.ClusterEventOutcomes> clusterEvent();

    /**
     * Nonstop events
     * 
     * @return the operation
     */
    Operation<CacheOperationOutcomes.NonStopOperationOutcomes> nonstop(); 

    /**
     * All get.
     *
     * @return the result
     */
    Result allGet();

    /**
     * All miss.
     *
     * @return the result
     */
    Result allMiss();

    /**
     * All put.
     *
     * @return the result
     */
    Result allPut();

    /**
     * Heap all put.
     *
     * @return the result
     */
    Result heapAllPut();

    /**
     * Off heap all put.
     *
     * @return the result
     */
    Result offHeapAllPut();

    /**
     * Disk all put.
     *
     * @return the result
     */
    Result diskAllPut();

    /**
     * Cache hit ratio.
     * @return
     */
    Statistic<Double> cacheHitRatio();

    /**
     * Nonstop timeout ratio
     * @return
     */
    Statistic<Double> nonstopTimeoutRatio();

    /**
     * Operations.
     *
     * @param <T> the generic type
     * @param outcome the outcome
     * @param name the name
     * @param tags the tags
     * @return the sets the
     */
    <T extends Enum<T>> Set<Operation<T>> operations(Class<T> outcome, String name, String... tags);

    /**
     * Get the set of cache specific pass thru statistics for a nam/tags pair. Used for
     * custom pass thru statistics, as opposed to well known standard ones.
     * @param name name
     * @param tags set of tags
     * @return
     */
    Set<Statistic<Number>> passthru(String name, Set<String> tags);

    /**
     * The Interface Operation.
     *
     * @param <T> the generic type
     */
    public interface Operation<T extends Enum<T>> {

        /**
         * Type.
         *
         * @return the class
         */
        Class<T> type();

        /**
         * Component.
         *
         * @param result the result
         * @return the result
         */
        Result component(T result);

        /**
         * Compound.
         *
         * @param results the results
         * @return the result
         */
        Result compound(Set<T> results);

        /**
         * Ratio of.
         *
         * @param numerator the numerator
         * @param denomiator the denomiator
         * @return the statistic
         */
        Statistic<Double> ratioOf(Set<T> numerator, Set<T> denomiator);

        /**
         * Sets the always on.
         *
         * @param enable the new always on
         */
        void setAlwaysOn(boolean enable);

        /**
         * Checks if is always on.
         *
         * @return true, if is always on
         */
        boolean isAlwaysOn();

        /**
         * Sets the window.
         *
         * @param time the time
         * @param unit the unit
         */
        void setWindow(long time, TimeUnit unit);

        /**
         * Sets the history.
         *
         * @param samples the samples
         * @param time the time
         * @param unit the unit
         */
        void setHistory(int samples, long time, TimeUnit unit);

        /**
         * Gets the window size.
         *
         * @param unit the unit
         * @return the window size
         */
        long getWindowSize(TimeUnit unit);

        /**
         * Gets the history sample size.
         *
         * @return the history sample size
         */
        int getHistorySampleSize();

        /**
         * Gets the history sample time.
         *
         * @param unit the unit
         * @return the history sample time
         */
        long getHistorySampleTime(TimeUnit unit);

    }

    /**
     * The Interface Result.
     */
    public interface Result {

        /**
         * Count.
         *
         * @return the statistic
         */
        Statistic<Long> count();

        /**
         * Rate.
         *
         * @return the statistic
         */
        Statistic<Double> rate();

        /**
         * Latency.
         *
         * @return the latency
         */
        Latency latency();
    }

    /**
     * The Interface Latency.
     */
    public interface Latency {

        /**
         * Minimum.
         *
         * @return the statistic
         */
        Statistic<Long> minimum();

        /**
         * Maximum.
         *
         * @return the statistic
         */
        Statistic<Long> maximum();

        /**
         * Average.
         *
         * @return the statistic
         */
        Statistic<Double> average();
    }

    /**
     * The Interface Statistic.
     *
     * @param <T> the generic type
     */
    public interface Statistic<T extends Number> {

        /**
         * Active.
         *
         * @return true, if successful
         */
        boolean active();

        /**
         * Value.
         *
         * @return the t
         */
        T value();

        /**
         * History.
         *
         * @return the list
         */
        List<Timestamped<T>> history();
    }

    /**
     * Gets the size.
     *
     * @return the size
     */
    Statistic<Number> size();

    /**
     * Gets the local heap size.
     *
     * @return the local heap size
     */
    Statistic<Number> localHeapSize();

    /**
     * Gets the local heap size in bytes.
     *
     * @return the local heap size in bytes
     */
    Statistic<Number> localHeapSizeInBytes();

    /**
     * Gets the local off heap size.
     *
     * @return the local off heap size
     */
    Statistic<Number> localOffHeapSize();

    /**
     * Gets the local off heap size in bytes.
     *
     * @return the local off heap size in bytes
     */
    Statistic<Number> localOffHeapSizeInBytes();

    /**
     * Gets the local disk size.
     *
     * @return the local disk size
     */
    Statistic<Number> localDiskSize();

    /**
     * Gets the local disk size in bytes.
     *
     * @return the local disk size in bytes
     */
    Statistic<Number> localDiskSizeInBytes();

    /**
     * Gets the remote size.
     *
     * @return the remote size
     */
    Statistic<Number> remoteSize();

    /**
     * Gets the writer queue length.
     *
     * @return the writer queue length
     */
    Statistic<Number> writerQueueLength();
    
    /**
     * Get the timestamp (millis) of the last cluster rejoin event
     * 
     * @return statistic for cluster rejoin timestamp
     */
    Statistic<Number> mostRecentRejoinTimeStampMillis();

}