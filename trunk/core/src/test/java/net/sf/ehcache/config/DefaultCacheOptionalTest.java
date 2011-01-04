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
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Abhishek Sanoujam
 */
public class DefaultCacheOptionalTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCacheOptionalTest.class);

    private CacheManager cacheManager;

    @Override
    public void tearDown() {
        if (cacheManager != null) {
            cacheManager.shutdown();
        }
    }

    public void testDefaultCacheIsOptional() {
        cacheManager = new CacheManager(this.getClass().getResourceAsStream("/no-default-cache.xml"));
        String[] cacheNames = cacheManager.getCacheNames();
        LOG.info("Cache names: " + Arrays.asList(cacheNames));
        Assert.assertEquals(1, cacheNames.length);
        Assert.assertEquals("sampleCache", cacheNames[0]);

        // adding caches by name should fail as no default cache config specified
        try {
            cacheManager.addCache("someNewCache");
            fail("Adding cache by name with no default config should fail");
        } catch (CacheException e) {
            LOG.info("Got expected exception - " + e.getMessage());
        }

        try {
            cacheManager.addCacheIfAbsent("someNewCache");
            fail("Adding cache by name with no default config should fail");
        } catch (CacheException e) {
            LOG.info("Got expected exception - " + e.getMessage());
        }

        // adding actual caches should work
        CacheConfiguration config = new CacheConfiguration("some-name", 92843);
        Cache cache = new Cache(config);
        cacheManager.addCache(cache);
        LOG.info("Added concrete cache successfully");

    }

}
