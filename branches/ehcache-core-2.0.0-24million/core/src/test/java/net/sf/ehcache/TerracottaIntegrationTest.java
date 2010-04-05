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

package net.sf.ehcache;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * These tests require a Terracotta server running on localhost.
 *
 * If running this interactively, start terracotta with mvn tc:start.
 * To stop mvn tc:stop
 *
 * This test is set up in Maven as an integration test. Terracotta is set up to start and stop pre and post the
 * integration tests phase.
 *
 * @author Greg Luck
 */
public class TerracottaIntegrationTest {

    /**
     * the CacheManager instance
     */
    protected CacheManager manager;

    /**
     * setup test
     */
    @Before
    public void setUp() throws Exception {
        manager = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + File.separator
                + "terracotta" + File.separator + "ehcache-terracotta-localhost.xml");
    }

    /**
     * teardown
     */
    @After
    public void tearDown() throws Exception {
        if (manager != null) {
            manager.shutdown();
        }
    }

    /**
     * Tests that we can put something into a cache with Terracotta"
     */
    @Test
    public void testIntegration() {
        Cache cache1 = manager.getCache("clustered-1");
        manager.addCache("defaults");
        Cache defaults = manager.getCache("defaults");

        cache1.put(new Element("key1", "value1"));
        Assert.assertEquals("value1", cache1.get("key1"));
    }
}