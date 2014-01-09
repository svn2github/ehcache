package net.sf.ehcache.management.sampled;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.query.QueryManagerBuilder;
import net.sf.ehcache.search.query.TestQueryManagerBuilder;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anthony Dahanne
 */
public class CacheManagerSamplerImplTest {
  @Test(expected = SearchException.class)
  public void testExecuteQuery__no_caches() throws Exception {

    CacheManager cacheManager =  mock(CacheManager.class);
    String[] emptyCacheNamesArray = new String[0];
    when(cacheManager.getCacheNames()).thenReturn(emptyCacheNamesArray);
    CacheManagerSamplerImpl cacheManagerSampler = new CacheManagerSamplerImpl(cacheManager);
    QueryManagerBuilder qmb = TestQueryManagerBuilder.getQueryManagerBuilder();
    cacheManagerSampler.executeQuery("bogus query", qmb);
  }

  @Test(expected = SearchException.class)
  public void testExecuteQuery__no_searchable_caches() throws Exception {

    CacheManager cacheManager =  mock(CacheManager.class);
    String[] emptyCacheNamesArray = new String[]{"pif"};
    when(cacheManager.getCacheNames()).thenReturn(emptyCacheNamesArray);
    CacheConfiguration cacheConfiguration = new CacheConfiguration().name("pif");
    Ehcache cache =  new Cache(cacheConfiguration);
    when(cacheManager.getEhcache("pif")).thenReturn(cache);

    CacheManagerSamplerImpl cacheManagerSampler = new CacheManagerSamplerImpl(cacheManager);
    QueryManagerBuilder qmb = TestQueryManagerBuilder.getQueryManagerBuilder();
    cacheManagerSampler.executeQuery("bogus query", qmb);
  }
}
