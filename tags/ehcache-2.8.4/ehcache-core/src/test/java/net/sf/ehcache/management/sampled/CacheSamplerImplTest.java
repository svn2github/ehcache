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
package net.sf.ehcache.management.sampled;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.statistics.StatisticsGateway;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test to check that quickStore() is called on clustered caches and getSize() on unclustered ones.
 *
 * @author Ludovic Orban
 */
public class CacheSamplerImplTest {

  private static final long EXACT_SIZE = 456L;
  private static final long QUICK_SIZE = 123L;

  private CacheConfiguration cacheConfiguration = new CacheConfiguration();
  private Ehcache cache = mock(Cache.class);
  private StatisticsGateway statisticsGateway = mock(StatisticsGateway.class);
  private ExtendedStatistics extendedStatistics = mock(ExtendedStatistics.class);
  private ExtendedStatistics.Statistic<Number> remoteSizeStat = mock(ExtendedStatistics.Statistic.class);

  @Before
  public void setUpCacheSamplerImpl() {
    when(cache.getCacheConfiguration()).thenReturn(cacheConfiguration);
    when(cache.getStatistics()).thenReturn(statisticsGateway);
    when(statisticsGateway.getExtended()).thenReturn(extendedStatistics);
    when(statisticsGateway.getSize()).thenReturn(EXACT_SIZE);
    when(extendedStatistics.remoteSize()).thenReturn(remoteSizeStat);
    when(remoteSizeStat.value()).thenReturn(QUICK_SIZE);
  }

  @Test
  public void given_clustered_cache__cacheSampler_getSize_returns_quick_size() throws Exception {
    cacheConfiguration.terracotta(new TerracottaConfiguration().clustered(true));
    CacheSamplerImpl cacheSampler = new CacheSamplerImpl(cache);

    assertThat(cacheSampler.getSize(), equalTo(QUICK_SIZE));
  }

  @Test
  public void given_unclustered_cache__cacheSampler_getSize_returns_exact_size() throws Exception {
    cacheConfiguration.terracotta(new TerracottaConfiguration().clustered(false));
    CacheSamplerImpl cacheSampler = new CacheSamplerImpl(cache);

    assertThat(cacheSampler.getSize(), equalTo(EXACT_SIZE));
  }

}
