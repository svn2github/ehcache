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

import java.util.Map;

/**
 * An abstraction for sampled cache manager usage statistics.
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @author <a href="mailto:byoukste@terracottatech.com">byoukste</a>
 */
public interface CacheManagerSampler {
    /**
     * Gets the actual name of the cache manager.
     */
    String getName();

    /**
     * Gets the cluster uuid if applicable.
     *
     * @return the cluster uuid
     */
    String getClusterUUID();

    /**
     * Gets the status attribute of the Ehcache
     *
     * @return The status value, as a String from the Status enum class
     */
    String getStatus();

    /**
     * Enables/disables each cache contained by this CacheManager
     *
     * @param enabled
     */
    void setEnabled(boolean enabled);

    /**
     * Returns if each cache is enabled.
     *
     * @return boolean indicating that each cache is enabled
     */
    boolean isEnabled();

    /**
     * Shuts down the CacheManager.
     * <p/>
     * If the shutdown occurs on the singleton, then the singleton is removed, so that if a singleton access method is called, a new
     * singleton will be created.
     */
    void shutdown();

    /**
     * Clears the contents of all caches in the CacheManager, but without
     * removing any caches.
     * <p/>
     * This method is not synchronized. It only guarantees to clear those elements in a cache at the time that the
     * {@link net.sf.ehcache.Ehcache#removeAll()} mehod on each cache is called.
     */
    void clearAll();

    /**
     * Gets the cache names managed by the CacheManager
     */
    String[] getCacheNames() throws IllegalStateException;

    /**
     * Get a map of cache name to performance metrics (hits, misses).
     *
     * @return a map of cache metrics
     */
    Map<String, long[]> getCacheMetrics();

    /**
     * @return aggregate hit rate
     */
    long getCacheHitRate();

    /**
     * @return aggregate in-memory hit rate
     */
    long getCacheInMemoryHitRate();

    /**
     * @return aggregate off-heap hit rate
     */
    long getCacheOffHeapHitRate();

    /**
     * @return aggregate on-disk hit rate
     */
    long getCacheOnDiskHitRate();

    /**
     * @return aggregate miss rate
     */
    long getCacheMissRate();

    /**
     * @return aggregate in-memory miss rate
     */
    long getCacheInMemoryMissRate();

    /**
     * @return aggregate off-heap miss rate
     */
    long getCacheOffHeapMissRate();

    /**
     * @return aggregate on-disk miss rate
     */
    long getCacheOnDiskMissRate();

    /**
     * @return aggregate put rate
     */
    long getCachePutRate();

    /**
     * @return aggregate update rate
     */
    long getCacheUpdateRate();

    /**
     * @return aggregate remove rate
     */
    long getCacheRemoveRate();

    /**
     * @return aggregate eviction rate
     */
    long getCacheEvictionRate();

    /**
     * @return aggregate expiration rate
     */
    long getCacheExpirationRate();

    /**
     * @return aggregate average get time (ms.)
     */
    float getCacheAverageGetTime();

    /**
     * @return if any contained caches are configured for search
     */
    boolean getSearchable();

    /**
     * @return aggregate search rate
     */
    long getCacheSearchRate();

    /**
     * @return aggregate search time
     */
    long getCacheAverageSearchTime();

    /**
     * generateActiveConfigDeclaration
     *
     * @return CacheManager configuration as String
     */
    String generateActiveConfigDeclaration();

    /**
     * generateActiveConfigDeclaration
     *
     * @param cacheName
     * @return Cache configuration as String
     */
    String generateActiveConfigDeclaration(String cacheName);

    /**
     * Are any of the caches transactional
     *
     * @see net.sf.ehcache.config.CacheConfiguration.TransactionalMode
     */
    boolean getTransactional();

    /**
     * Get the committed transactions count
     *
     * @return the committed transactions count
     */
    long getTransactionCommittedCount();

    /**
     * @return aggregate Xa commit rate
     */
    long getTransactionCommitRate();

    /**
     * Get the rolled back transactions count
     *
     * @return the rolled back transactions count
     */
    long getTransactionRolledBackCount();

    /**
     * @return aggregate Xa rollback rate
     */
    long getTransactionRollbackRate();

    /**
     * Get the timed out transactions count. Note that only transactions which failed to
     * commit due to a timeout are taken into account
     *
     * @return the timed out transactions count
     */
    long getTransactionTimedOutCount();

    /**
     * Returns whether any caches are configured for write-behind
     */
    boolean getHasWriteBehindWriter();

    /**
     * Returns the total length of all write-behind queues across all caches
     *
     * @return aggregate writer-behind queue length
     */
    long getWriterQueueLength();

    /**
     * Maximum elements that can be queued for processing by the write-behind writer
     *
     * @return aggregate of the maximum elements that can be waiting to be processed
     *         by the write-behind writer across all caches
     */
    int getWriterMaxQueueSize();

    /**
     * Maximum number of bytes of entries in the disk stores of all caches that
     * did not declare their own max size.
     *
     * @return maximum number of bytes in the disk stores of all caches that
     *         did not declare their own max size.
     */
    long getMaxBytesLocalDisk();

    /**
     * @param maxBytes
     */
    void setMaxBytesLocalDisk(long maxBytes);

    /**
     * @param maxBytes
     */
    void setMaxBytesLocalDiskAsString(String maxBytes);

    /**
     * @return Original input for maxBytesLocalDisk
     */
    String getMaxBytesLocalDiskAsString();

    /**
     * Maximum number of bytes of entries in the heap memory stores of all caches that
     * did not declare their own max size.
     *
     * @return maximum number of bytes in the heap memory stores of all caches that
     *         did not declare their own max size.
     */
    long getMaxBytesLocalHeap();

    /**
     * @return Original input for maxBytesLocalHeap
     */
    String getMaxBytesLocalHeapAsString();

    /**
     * @param maxBytes
     */
    void setMaxBytesLocalHeap(long maxBytes);

    /**
     * @param maxBytes
     */
    void setMaxBytesLocalHeapAsString(String maxBytes);

    /**
     * Maximum number of bytes of entries in the off-heap stores of all caches that
     * did not declare their own max size.
     *
     * @return maximum number of bytes in the off-heap stores of all caches that
     *         did not declare their own max size.
     */
    long getMaxBytesLocalOffHeap();

    /**
     * @return Original input for maxBytesLocalOffHeap
     */
    String getMaxBytesLocalOffHeapAsString();
}