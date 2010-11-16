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

package net.sf.ehcache.config.nonstop;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

public class NonStopConfigTest extends TestCase {

    public void testInvalidConfig() {
        try {
            CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/nonstop/nonstop-invalid-config-test.xml"));
            fail("Invalid config should have failed");
        } catch (CacheException e) {
            // make sure its the expected exception
            assertTrue(e.getMessage().contains("does not allow attribute \"one\""));
        }
    }

    public void testNonStopCacheConfig() {
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/nonstop/nonstop-config-test.xml"));
        assertNonstopConfig(cacheManager.getCache("defaultConfig"), NonstopConfiguration.DEFAULT_ENABLED,
                NonstopConfiguration.DEFAULT_IMMEDIATE_TIMEOUT, NonstopConfiguration.DEFAULT_TIMEOUT_MILLIS,
                NonstopConfiguration.DEFAULT_TIMEOUT_BEHAVIOR.getType());

        assertNonstopConfig(cacheManager.getCache("one"), false, NonstopConfiguration.DEFAULT_IMMEDIATE_TIMEOUT,
                NonstopConfiguration.DEFAULT_TIMEOUT_MILLIS, NonstopConfiguration.DEFAULT_TIMEOUT_BEHAVIOR.getType());

        assertNonstopConfig(cacheManager.getCache("two"), false, false, NonstopConfiguration.DEFAULT_TIMEOUT_MILLIS,
                NonstopConfiguration.DEFAULT_TIMEOUT_BEHAVIOR.getType());
        assertNonstopConfig(cacheManager.getCache("three"), false, false, 12345, NonstopConfiguration.DEFAULT_TIMEOUT_BEHAVIOR
                .getType());
        assertNonstopConfig(cacheManager.getCache("four"), false, false, 12345, "localReads");
        cacheManager.shutdown();
    }

    private void assertNonstopConfig(Cache cache, boolean nonstop, boolean immediateTimeout, int timeoutMillis, String timeoutBehavior) {
        System.out.println("Checking for cache: " + cache.getName());
        CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
        assertNotNull(cacheConfiguration);
        TerracottaConfiguration terracottaConfiguration = cacheConfiguration.getTerracottaConfiguration();
        assertNotNull(terracottaConfiguration);
        NonstopConfiguration nonstopConfiguration = terracottaConfiguration.getNonstopConfiguration();
        assertNotNull(nonstopConfiguration);

        assertEquals(nonstop, nonstopConfiguration.isEnabled());
        assertEquals(immediateTimeout, nonstopConfiguration.isImmediateTimeout());
        assertEquals(timeoutMillis, nonstopConfiguration.getTimeoutMillis());
        assertEquals(timeoutBehavior, nonstopConfiguration.getTimeoutBehavior().getType());
    }

}
