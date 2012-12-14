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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.util.counter.sampled.SampledCounter;
import net.sf.ehcache.util.counter.sampled.SampledRateCounter;

/**
 * The default implementation of {@link ComprehensiveCacheSampler}.
 *
 * @author <a href="mailto:byoukste@terracottatech.com">byoukste</a>
 */
public class ComprehensiveCacheSamplerImpl extends CacheSamplerImpl implements ComprehensiveCacheSampler {
    private final Ehcache cache;

    /**
     * Constructor that takes a {@link Ehcache}
     *
     * @param cache the {@code Cache} that backs this sampler
     */
    public ComprehensiveCacheSamplerImpl(Ehcache cache) {
        super(cache);
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitSample() {
        return new SampledCounterProxy(cache.getStatistics().cacheHitOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitRatioSample() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitInMemorySample() {
        return new SampledCounterProxy(cache.getStatistics().localHeapHitOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitOffHeapSample() {
        return new SampledCounterProxy(cache.getStatistics().localOffHeapHitOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitOnDiskSample() {
        return new SampledCounterProxy(cache.getStatistics().diskHeapHitOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissSample() {
        return new SampledCounterProxy(cache.getStatistics().cacheMissOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissInMemorySample() {
        return new SampledCounterProxy(cache.getStatistics().localHeapMissOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissOffHeapSample() {
        return new SampledCounterProxy(cache.getStatistics().localOffHeapMissOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissOnDiskSample() {
        return new SampledCounterProxy(cache.getStatistics().diskHeapMissOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissExpiredSample() {
        return new SampledCounterProxy(cache.getStatistics().cacheMissExpiredOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissNotFoundSample() {
        return new SampledCounterProxy(cache.getStatistics().cacheMissNotFoundOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementEvictedSample() {
        return new SampledCounterProxy(cache.getStatistics().cacheEvictionOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementRemovedSample() {
        return new SampledCounterProxy(cache.getStatistics().cacheRemoveOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementExpiredSample() {
        return new SampledCounterProxy(cache.getStatistics().cacheExpiredOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementPutSample() {
        return new SampledCounterProxy(cache.getStatistics().cachePutOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementUpdatedSample() {
        return new SampledCounterProxy(cache.getStatistics().cachePutReplacedOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledRateCounter getAverageGetTimeSample() {
        return new SampledRateCounterProxy(cache.getStatistics().cacheGetOperation().latency().average());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledRateCounter getAverageGetTimeNanosSample() {
        return new SampledRateCounterProxy(cache.getStatistics().cacheGetOperation().latency().average());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledRateCounter getAverageSearchTimeSample() {
        return new SampledRateCounterProxy(cache.getStatistics().cacheSearchOperation().latency().average());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getSearchesPerSecondSample() {
        return new SampledRateCounterProxy(cache.getStatistics().cacheSearchOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheXaCommitsSample() {
        return new SampledRateCounterProxy(cache.getStatistics().xaCommitSuccessOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheXaRollbacksSample() {
        return new SampledRateCounterProxy(cache.getStatistics().xaRollbackOperation().rate());
    }
}
