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

package net.sf.ehcache.management.sampled;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.statistics.SampledCacheUsageStatistics;

/**
 * An implementation of {@link SampledCacheMBean}
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class SampledCache implements SampledCacheMBean {

    private final Ehcache cache;
    private final SampledCacheUsageStatistics sampledCacheUsageStatistics;

    /**
     * Constructor accepting the backing {@link Ehcache}
     * 
     * @param cache
     */
    public SampledCache(Ehcache cache) {
        this.cache = cache;
        this.sampledCacheUsageStatistics = cache
                .getSampledCacheUsageStatistics();
    }

    /**
     * {@inheritDoc}
     */
    public void flush() {
        cache.flush();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return cache.getName();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatus() {
        return cache.getStatus().toString();
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() {
        cache.removeAll();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitCountMostRecentSample() {
        return sampledCacheUsageStatistics.getCacheHitMostRecentSample();
    }

}
