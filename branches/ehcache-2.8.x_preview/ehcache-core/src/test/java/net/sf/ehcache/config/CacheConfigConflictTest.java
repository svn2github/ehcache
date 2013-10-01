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

import java.util.Arrays;

import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;

import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.test.categories.CheckShorts;

@Category(CheckShorts.class)
public class CacheConfigConflictTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(CacheConfigConflictTest.class);

    public void testConflictingValuesFromConfig() {
        CacheManager cacheManager = new CacheManager(this.getClass().getResourceAsStream("/ehcache-conflict-eternal.xml"));
        LOG.info("Cache names: " + Arrays.asList(cacheManager.getCacheNames()));
    }

    public void testConflictingValuesProgrammatic() {
        CacheConfiguration cacheConfig = new CacheConfiguration("name", 10);

        // case 1: eternal=true, followed by non conflicting TTI&TTL
        cacheConfig.setEternal(true);
        cacheConfig.timeToIdleSeconds(0);
        cacheConfig.timeToLiveSeconds(0);
        assertEquals(0, cacheConfig.getTimeToIdleSeconds());
        assertEquals(0, cacheConfig.getTimeToIdleSeconds());

        // case 2: eternal=true, followed by conflicting TTI&TTL
        // eternal=true takes more precedance than tti/ttl, order doesn't matter
        cacheConfig.setEternal(true);
        cacheConfig.timeToIdleSeconds(10);
        assertEquals(0, cacheConfig.getTimeToIdleSeconds());

        cacheConfig.timeToLiveSeconds(20);
        assertEquals(0, cacheConfig.getTimeToIdleSeconds());

        // case 3: eternal=false, followed non-conflicting TTI&TTL
        // reset eternal
        cacheConfig.eternal(false);
        cacheConfig.timeToIdleSeconds(10);
        assertEquals(10, cacheConfig.getTimeToIdleSeconds());

        cacheConfig.timeToLiveSeconds(20);
        assertEquals(20, cacheConfig.getTimeToLiveSeconds());

        // case 4: setting eternal again resets tti/ttl
        cacheConfig.eternal(true);
        assertEquals(0, cacheConfig.getTimeToIdleSeconds());
        assertEquals(0, cacheConfig.getTimeToLiveSeconds());

        // case 5: after a reset, TTI&TTL are still 0
        cacheConfig.eternal(false);
        assertEquals(0, cacheConfig.getTimeToIdleSeconds());
        assertEquals(0, cacheConfig.getTimeToLiveSeconds());

    }

}
