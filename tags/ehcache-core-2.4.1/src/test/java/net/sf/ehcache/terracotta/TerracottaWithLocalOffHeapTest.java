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

package net.sf.ehcache.terracotta;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.config.InvalidConfigurationException;

import org.junit.Assert;
import org.junit.Test;

public class TerracottaWithLocalOffHeapTest {

    @Test
    public void testInvalidRejoinWithoutNonstop() throws Exception {
        ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        TerracottaUnitTesting.setupTerracottaTesting(mockFactory);

        CacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        try {
            new CacheManager(CacheManager.class.getResourceAsStream("/terracotta/ehcache-terracotta-offheap.xml"));
            Assert.fail("Trying to use overflow-to-offheap with terracotta caches should fail");
        } catch (InvalidConfigurationException e) {
            System.out.println("Caught Expected : " + e);
            //expected
        }
    }
}
