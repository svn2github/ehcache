/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package org.terracotta.modules.ehcache;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFeatureType;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import java.io.Serializable;

/**
 * ToolkitInstanceFactoryImplTest
 */
public class ToolkitInstanceFactoryImplTest {

    @Test
    public void testMaxEntriesInCacheToMaxTotalCountTransformation() {
        verifyMapping(0, -1);
        verifyMapping(10, 10);
    }

    private void verifyMapping(int maxEntries, int maxCount) {
        Toolkit toolkit = mock(Toolkit.class);

        makeToolkitReturnNonStopConfigurationRegistry(toolkit);

        ToolkitInstanceFactoryImpl factory = new ToolkitInstanceFactoryImpl(toolkit);

        CacheConfiguration configuration = new CacheConfiguration().terracotta(new TerracottaConfiguration()).maxEntriesInCache(maxEntries);

        Ehcache ehcache = mock(Ehcache.class);
        configureEhcacheMockForToolkitUse(ehcache, configuration);

        factory.getOrCreateToolkitCache(ehcache);

        ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
        verify(toolkit).getCache(anyString(), captor.capture(), eq(Serializable.class));
        assertThat(captor.getValue().getInt(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME), is(maxCount));
    }

    private void configureEhcacheMockForToolkitUse(Ehcache ehcache, CacheConfiguration configuration) {CacheManager cacheManager = mock(CacheManager.class);
        when(ehcache.getCacheConfiguration()).thenReturn(configuration);
        when(ehcache.getCacheManager()).thenReturn(cacheManager);
    }

    private void makeToolkitReturnNonStopConfigurationRegistry(Toolkit toolkit) {NonStopFeature feature = mock(NonStopFeature.class);
        when(toolkit.getFeature(any(ToolkitFeatureType.class))).thenReturn(feature);
        when(feature.getNonStopConfigurationRegistry()).thenReturn(mock(NonStopConfigurationRegistry.class));
    }
}
