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

package net.sf.ehcache.config;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Abhishek Sanoujam
 */
public class StorageStrategyConfigTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(StorageStrategyConfigTest.class);

    @Test
    public void testStorageStrategyConfig() {
        CacheManager cacheManager = new CacheManager(this.getClass().getResourceAsStream("/ehcache-storage-strategy.xml"));
        Cache cache = cacheManager.getCache("defaultStorageStrategy");
        StorageStrategy storageStrategy = cache.getCacheConfiguration().getTerracottaConfiguration().getStorageStrategy();
        LOG.info("default storageStrategy: " + storageStrategy);
        StorageStrategy defaultStorageStrategy = StorageStrategy.DCV2;
        assertEquals("Default storageStrategy should be: " + defaultStorageStrategy, defaultStorageStrategy, storageStrategy);

        cache = cacheManager.getCache("classicStorageStrategy");
        storageStrategy = cache.getCacheConfiguration().getTerracottaConfiguration().getStorageStrategy();
        LOG.info("classic storageStrategy: " + storageStrategy);
        assertEquals(StorageStrategy.DCV2, storageStrategy);

        cache = cacheManager.getCache("DCV2StorageStrategy");
        storageStrategy = cache.getCacheConfiguration().getTerracottaConfiguration().getStorageStrategy();
        LOG.info("DCV2 storageStrategy: " + storageStrategy);
        assertEquals(StorageStrategy.DCV2, storageStrategy);

        TerracottaConfiguration config = cache.getCacheConfiguration().getTerracottaConfiguration();
        config.setStorageStrategy("classic");
        assertEquals(StorageStrategy.DCV2, config.getStorageStrategy());

        config.setStorageStrategy("DCV2");
        assertEquals(StorageStrategy.DCV2, config.getStorageStrategy());

        config.storageStrategy("classic");
        assertEquals(StorageStrategy.DCV2, config.getStorageStrategy());

        config.storageStrategy("DCV2");
        assertEquals(StorageStrategy.DCV2, config.getStorageStrategy());

        config.storageStrategy(StorageStrategy.CLASSIC);
        assertEquals(StorageStrategy.DCV2, config.getStorageStrategy());

        config.storageStrategy(StorageStrategy.DCV2);
        assertEquals(StorageStrategy.DCV2, config.getStorageStrategy());
    }

}
