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
package net.sf.ehcache.management.sampled;

import net.sf.ehcache.Cache;
import net.sf.ehcache.util.counter.sampled.SampledCounter;
import net.sf.ehcache.util.counter.sampled.SampledRateCounter;

/**
 * The default implementation of {@link ComprehensiveCacheSampler}.
 * 
 * @author <a href="mailto:byoukste@terracottatech.com">byoukste</a>
 */
public class ComprehensiveCacheSamplerImpl extends CacheSamplerImpl implements ComprehensiveCacheSampler {
    private Cache cache;

    /**
     * Constructor that takes a {@link Cache}
     *
     * @param cache the {@code Cache} that backs this sampler
     */
    public ComprehensiveCacheSamplerImpl(Cache cache) {
        super(cache);
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitSample() {
        return cache.getCacheStatisticsSampler().getCacheHitSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitInMemorySample() {
        return cache.getCacheStatisticsSampler().getCacheHitInMemorySample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitOffHeapSample() {
        return cache.getCacheStatisticsSampler().getCacheHitOffHeapSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitOnDiskSample() {
        return cache.getCacheStatisticsSampler().getCacheHitOnDiskSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissSample() {
        return cache.getCacheStatisticsSampler().getCacheMissSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissInMemorySample() {
        return cache.getCacheStatisticsSampler().getCacheMissInMemorySample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissOffHeapSample() {
        return cache.getCacheStatisticsSampler().getCacheMissOffHeapSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissOnDiskSample() {
        return cache.getCacheStatisticsSampler().getCacheMissOnDiskSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissExpiredSample() {
        return cache.getCacheStatisticsSampler().getCacheMissExpiredSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissNotFoundSample() {
        return cache.getCacheStatisticsSampler().getCacheMissNotFoundSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementEvictedSample() {
        return cache.getCacheStatisticsSampler().getCacheElementEvictedSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementRemovedSample() {
        return cache.getCacheStatisticsSampler().getCacheElementRemovedSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementExpiredSample() {
        return cache.getCacheStatisticsSampler().getCacheElementExpiredSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementPutSample() {
        return cache.getCacheStatisticsSampler().getCacheElementPutSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementUpdatedSample() {
        return cache.getCacheStatisticsSampler().getCacheElementUpdatedSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledRateCounter getAverageGetTimeSample() {
        return cache.getCacheStatisticsSampler().getAverageGetTimeSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledRateCounter getAverageSearchTimeSample() {
        return cache.getCacheStatisticsSampler().getAverageSearchTimeSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getSearchesPerSecondSample() {
        return cache.getCacheStatisticsSampler().getSearchesPerSecondSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheXaCommitsSample() {
        return cache.getCacheStatisticsSampler().getCacheXaCommitsSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheXaRollbacksSample() {
        return cache.getCacheStatisticsSampler().getCacheXaRollbacksSample();
    }
}
