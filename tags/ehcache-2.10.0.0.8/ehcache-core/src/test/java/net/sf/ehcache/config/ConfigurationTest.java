/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.ehcache.config;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.ObjectExistsException;
import net.sf.ehcache.Status;

import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ConfigurationTest
 */
public class ConfigurationTest {

    @Test
    public void testConfigurationCannotBeSharedAcrossRunningCacheManagers() {
        Configuration configuration = new Configuration();
        configuration.setupFor(mock(CacheManager.class), "one");

        try {
            configuration.setupFor(mock(CacheManager.class), "two");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("share a Configuration"));
        }
    }

    @Test
    public void testNullingTerracottaConfiguration() {
        Configuration config = new Configuration();
        config.addTerracottaConfig(new TerracottaClientConfiguration());

        try {
            config.addTerracottaConfig(new TerracottaClientConfiguration());
            throw new AssertionError();
        } catch (ObjectExistsException oee) {
            // expected
        }

        config.addTerracottaConfig(null);
        config.addTerracottaConfig(new TerracottaClientConfiguration());
    }

    @Test
    public void testConfigurationCanBeReusedAfterCacheManagerShutdown() {
        Configuration configuration = new Configuration();
        CacheManager mock = mock(CacheManager.class);
        when(mock.getStatus()).thenReturn(Status.STATUS_SHUTDOWN);
        configuration.setupFor(mock, "one");

        configuration.setupFor(mock(CacheManager.class), "two");
    }

    @Test
    public void testCleanup() throws Exception {
        Configuration configuration =  new Configuration();
        CacheManager cacheManager = mock(CacheManager.class);
        Configuration.RuntimeCfg runtimeCfg1 = configuration.setupFor(cacheManager, "initial CacheManager");
        configuration.cleanup();
        Configuration.RuntimeCfg runtimeCfg2 = configuration.setupFor(cacheManager, "expecting CacheManager name in RuntimeCfg to be this value");
        assertTrue(runtimeCfg2.getCacheManagerName().equals("expecting CacheManager name in RuntimeCfg to be this value"));
    }
}
