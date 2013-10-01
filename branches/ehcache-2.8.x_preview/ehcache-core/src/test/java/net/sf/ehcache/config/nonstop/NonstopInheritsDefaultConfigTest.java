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

package net.sf.ehcache.config.nonstop;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.terracotta.TerracottaUnitTesting;

import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.test.categories.CheckShorts;

@Category(CheckShorts.class)
public class NonstopInheritsDefaultConfigTest extends TestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonstopInheritsDefaultConfigTest.class);

    public void testNonstopConfigInheritsDefaultConfig() throws Exception {
        if (Boolean.valueOf("true")) {
            LOGGER.warn("THIS TEST IS CURRENTLY DISABLED");
            return;
        }
        ClusteredInstanceFactory mockFactory = Mockito.mock(ClusteredInstanceFactory.class);
        TerracottaUnitTesting.setupTerracottaTesting(mockFactory);

        CacheManager cacheManager = new CacheManager(NonstopInheritsDefaultConfigTest.class
                .getResourceAsStream("/nonstop/nonstop-inherits-default-config-test.xml"));

        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        LOGGER.info(cacheNames.toString());
        Assert.assertEquals(2, cacheNames.size());
        Assert.assertTrue(cacheNames.contains("defaultConfigCache"));
        Assert.assertTrue(cacheNames.contains("overridesDefaultConfigCache"));

        Ehcache defaultConfigCache = cacheManager.getEhcache("defaultConfigCache");
        Ehcache overridesDefaultConfigCache = cacheManager.getEhcache("overridesDefaultConfigCache");

        TerracottaConfiguration defaultTC = defaultConfigCache.getCacheConfiguration().getTerracottaConfiguration();
        Assert.assertNotNull(defaultTC);
        Assert.assertEquals(false, defaultTC.isClustered());
        Assert.assertNotNull(defaultTC.getNonstopConfiguration());
        Assert.assertEquals(12345, defaultTC.getNonstopConfiguration().getTimeoutMillis());

    }

}
