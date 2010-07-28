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

package net.sf.ehcache.config;

import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;

public class CacheConfigConflictTest extends TestCase {

    public void testConflictingValuesFromConfig() {
        try {
            CacheManager cacheManager = new CacheManager(this.getClass().getResourceAsStream("/ehcache-conflict-eternal.xml"));
            System.out.println("Cache names: " + Arrays.asList(cacheManager.getCacheNames()));
            fail("Config with conflicting values should have thrown exception.");
        } catch (CacheException e) {
            // expected
            Assert.assertTrue(e.getMessage().contains("Conflicting values"));
        }
    }

    public void testConflictingValuesProgrammatic() {
        CacheConfiguration cacheConfig = new CacheConfiguration("name", 10);
        cacheConfig.setEternal(true);
        cacheConfig.timeToIdleSeconds(0);
        cacheConfig.timeToLiveSeconds(0);
        try {
            cacheConfig.timeToIdleSeconds(10);
            fail("Config with conflicting values should have thrown exception.");
        } catch (Exception e) {
            // expected
            Assert.assertTrue(e.getMessage().contains("Conflicting values"));
        }

        try {
            cacheConfig.timeToIdleSeconds(10);
            fail("Config with conflicting values should have thrown exception.");
        } catch (Exception e) {
            // expected
            Assert.assertTrue(e.getMessage().contains("Conflicting values"));
        }

        try {
            cacheConfig.timeToIdleSeconds(10);
            fail("Config with conflicting values should have thrown exception.");
        } catch (Exception e) {
            // expected
            Assert.assertTrue(e.getMessage().contains("Conflicting values"));
        }

        cacheConfig.eternal(false);
        cacheConfig.timeToIdleSeconds(10);
        cacheConfig.timeToLiveSeconds(10);
        cacheConfig.timeToIdleSeconds(0);
        cacheConfig.timeToLiveSeconds(0);

    }

}
