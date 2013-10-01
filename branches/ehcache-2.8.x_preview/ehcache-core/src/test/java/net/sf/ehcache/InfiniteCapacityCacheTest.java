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

package net.sf.ehcache;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class InfiniteCapacityCacheTest {

    private static final int INFINITY = 1000;

    /**
     * the CacheManager instance
     */
    protected CacheManager manager;

    /**
     * setup test
     */
    @Before
    public void setUp() throws Exception {
        manager = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-infinite-capacity.xml");
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

    @Test
    public void testDefaultStoreCapacities() {
        Cache defined = manager.getCache("defined");
        manager.addCache("defaults");
        Cache defaults = manager.getCache("defaults");

        Assert.assertEquals(0, defined.getCacheConfiguration().getMaxElementsInMemory());
        Assert.assertEquals(0, defaults.getCacheConfiguration().getMaxElementsInMemory());

        for (int i = 0; i < INFINITY; i++) {
            defined.put(new Element(Integer.valueOf(i), new Object()));
            defaults.put(new Element(Integer.valueOf(i), new Object()));
        }

        Assert.assertEquals(INFINITY, defined.getSize());
        Assert.assertEquals(INFINITY, defined.getStatistics().getLocalHeapSize());
        Assert.assertEquals(0, defined.getStatistics().getLocalDiskSize());

        Assert.assertEquals(INFINITY, defaults.getSize());
        Assert.assertEquals(INFINITY, defaults.getStatistics().getLocalHeapSize());
        Assert.assertEquals(0, defaults.getStatistics().getLocalDiskSize());
    }
}
