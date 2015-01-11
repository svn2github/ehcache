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
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.statistics.StatisticsGateway;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
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

  @Test
  public void testCacheSize() {
    CacheManager manager = new CacheManager();
    CacheConfiguration conf = new CacheConfiguration("test", 0);
    conf.setTransactionalMode("LOCAL");
    testSize(conf, manager);
    CacheConfiguration conf1 = new CacheConfiguration("test1", 0);
    conf1.setTransactionalMode("XA");
    testSize(conf1, manager);
    CacheConfiguration conf2 = new CacheConfiguration("test2", 0);
    conf2.setTransactionalMode("XA_STRICT");
    testSize(conf2, manager);
    CacheConfiguration conf3 = new CacheConfiguration("test3", 0);
    testSize(conf3, manager, false);

    manager.shutdown();

  }

  private void testSize(CacheConfiguration conf, CacheManager mgr) {
    testSize(conf, mgr, true);
  }

  private void testSize(CacheConfiguration conf,CacheManager mgr, boolean checkTransactional) {
    mgr.addCache(new Cache(conf));
    CacheSamplerImpl impl = new CacheSamplerImpl(mgr.getCache(conf.getName()));
    if(checkTransactional) {
      assertTrue(impl.getTransactional());
    }
    assertEquals(impl.getSize(), 0);
  }

}
