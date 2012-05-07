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

import net.sf.ehcache.util.counter.sampled.SampledCounter;
import net.sf.ehcache.util.counter.sampled.SampledRateCounter;

/**
 * A {@link CacheSampler} interface that also exposes {@link SampledCounter} objects for all sampled statistics, rather than just the last
 * sampled value.
 *
 * @author <a href="mailto:byoukste@terracottatech.com">byoukste</a>
 */
public interface ComprehensiveCacheSampler extends CacheSampler {

    /**
     * Get the {@link SampledCounter} for cache hit
     *
     * @return the {@code SampledCounter} for cache hit count
     */
    public SampledCounter getCacheHitSample();

    /**
     * Get the {@link SampledCounter} for in-memory cache hit
     *
     * @return the {@code SampledCounter} for cache hit count in memory
     */
    public SampledCounter getCacheHitInMemorySample();

    /**
     * Get the {@link SampledCounter} for off-heap cache hit
     *
     * @return the {@code SampledCounter} for cache hit count in off-heap
     */
    public SampledCounter getCacheHitOffHeapSample();

    /**
     * Get the {@link SampledCounter} for on-disk cache hit
     *
     * @return the {@code SampledCounter} for cache hit count on disk
     */
    public SampledCounter getCacheHitOnDiskSample();

    /**
     * Get the {@link SampledCounter} for cache miss
     *
     * @return the {@code SampledCounter} for cache miss count
     */
    public SampledCounter getCacheMissSample();

    /**
     * Get the {@link SampledCounter} for in-memory cache miss
     *
     * @return the {@code SampledCounter} for cache miss count in memory
     */
    public SampledCounter getCacheMissInMemorySample();

    /**
     * Get the {@link SampledCounter} for off-heap cache miss
     *
     * @return the {@code SampledCounter} for cache miss count in off-heap
     */
    public SampledCounter getCacheMissOffHeapSample();

    /**
     * Get the {@link SampledCounter} for on-disk cache miss
     *
     * @return the {@code SampledCounter} for cache miss count on disk
     */
    public SampledCounter getCacheMissOnDiskSample();

    /**
     * Get the {@link SampledCounter} for cache miss as result of the element getting
     * expired
     *
     * @return the {@code SampledCounter} for cache miss count and the reason for miss
     *         being the element got expired
     */
    public SampledCounter getCacheMissExpiredSample();

    /**
     * Get the {@link SampledCounter} for cache miss as result of the element not found
     * in cache
     *
     * @return the {@code SampledCounter} for cache miss not found count
     */
    public SampledCounter getCacheMissNotFoundSample();

    /**
     * Get the {@link SampledCounter} element evicted from cache
     *
     * @return the {@code SampledCounter} for element evicted count
     */
    public SampledCounter getCacheElementEvictedSample();

    /**
     * Get the {@link SampledCounter} element removed from cache
     *
     * @return the {@code SampledCounter} for element removed count
     */
    public SampledCounter getCacheElementRemovedSample();

    /**
     * Get the {@link SampledCounter} element expired from cache
     *
     * @return Most recent value for element expired count
     */
    public SampledCounter getCacheElementExpiredSample();

    /**
     * Get the {@link SampledCounter} element puts in the cache
     *
     * @return the {@code SampledCounter} for number of element puts
     */
    public SampledCounter getCacheElementPutSample();

    /**
     * Get the {@link SampledCounter} element updates , i.e. put() on elements with
     * already existing keys in the cache
     *
     * @return the {@code SampledCounter}d value for element update count
     */
    public SampledCounter getCacheElementUpdatedSample();

    /**
     * Get the {@link SampledRateCounter} for average time taken for get() operation in the
     * cache
     *
     * @return the {@code SampledRateCounter} of average get time taken for a get operation
     */
    public SampledRateCounter getAverageGetTimeSample();

    /**
     * Get the {@link SampledRateCounter} for average search execution time for searches finishing within the last sample period
     *
     * @return the {@code SampledRateCounter} of average search time taken
     */
    public SampledRateCounter getAverageSearchTimeSample();

    /**
     * Get the {@link SampledCounter} for number of searches that have finished in the interval
     *
     * @return the {@code SampledCounter} for number of searches
     */
    public SampledCounter getSearchesPerSecondSample();

    /**
     * Get the {@link SampledCounter} for number of XA Transaction commits that have completed in the interval
     *
     * @return the {@code SampledCounter} for number XA Transaction commits
     */
    public SampledCounter getCacheXaCommitsSample();

    /**
     * Get the {@link SampledCounter} for number of XA Transaction rollbacks that have completed in the interval
     *
     * @return the {@code SampledCounter} for number XA Transaction rollbacks
     */
    public SampledCounter getCacheXaRollbacksSample();
}
