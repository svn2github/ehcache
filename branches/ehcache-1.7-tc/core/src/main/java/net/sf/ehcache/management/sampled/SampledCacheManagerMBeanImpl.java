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

import net.sf.ehcache.CacheManager;

/**
 * An implementation of {@link SampledCacheManagerMBean}
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class SampledCacheManagerMBeanImpl implements SampledCacheManagerMBean {

    private final CacheManager cacheManager;

    /**
     * Constructor taking the backing {@link CacheManager}
     * 
     * @param cacheManager
     */
    public SampledCacheManagerMBeanImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * {@inheritDoc}
     */
    public void clearAll() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public String[] getCacheNames() throws IllegalStateException {
        return cacheManager.getCacheNames();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatus() {
        return cacheManager.getStatus().toString();
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        //no-op
    }

}
