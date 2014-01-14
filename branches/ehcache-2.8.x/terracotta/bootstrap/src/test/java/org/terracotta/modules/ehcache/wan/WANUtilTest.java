/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.wan;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WANUtilTest {
  private static final String                 CACHE_MANAGER_NAME = "CACHE_MANAGER_NAME";
  private static final String                 CACHE_NAME         = "CACHE_NAME";

  private WANUtil                             wanUtil;
  private ConcurrentMap<String, Serializable> cacheConfigMap;
  private ConcurrentMap<String, Serializable> cacheManagerConfigMap;
  private boolean                             testResult;

  @Before
  public void setUp() {
    cacheConfigMap = new ConcurrentHashMap<String, Serializable>();
    cacheManagerConfigMap = new ConcurrentHashMap<String, Serializable>();

    wanUtil = getTestableWANUtil();
  }


  @Test
  public void testIsWANReadyWhenOrchestratorWasUp() throws Exception {
    whenWANReady().callIsWANReady().assertResultIs(true);
  }

  @Test
  public void testIsWANReadyWhenOrchestratorWasDown() throws Exception {
    callIsWANReady().assertResultIs(false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIsWanEnabledCacheForNullParameters() throws Exception {
    callIsWanEnabledCache(CACHE_NAME, null).assertResultIs(false);
    callIsWanEnabledCache(null, CACHE_MANAGER_NAME).assertResultIs(false);
  }

  @Test
  public void testIsWanEnabledCacheWhenCacheMarkedWanEnabled() {
    whenCacheMarkedWanEnabled().callIsWanEnabledCache(CACHE_MANAGER_NAME, CACHE_NAME).assertResultIs(true);
  }

  @Test
  public void testIsWanEnabledCacheWhenCacheMarkedWanDisabled() throws Exception {
    callIsWanEnabledCache(CACHE_MANAGER_NAME, CACHE_NAME).assertResultIs(false);
  }

  private WANUtilTest whenCacheMarkedWanEnabled() {
    wanUtil.markCacheWanEnabled(CACHE_MANAGER_NAME, CACHE_NAME);
    return this;
  }

  private WANUtilTest callIsWanEnabledCache(String cacheManagerName, String cacheName) {
    testResult = wanUtil.isWanEnabledCache(cacheManagerName, cacheName);
    return this;
  }

  private WANUtilTest callIsWANReady() {
    testResult = wanUtil.isWANReady(CACHE_MANAGER_NAME);
    return this;
  }

  private WANUtilTest whenWANReady() {
    wanUtil.markWANReady(CACHE_MANAGER_NAME);
    return this;
  }

  private void assertResultIs(boolean expectedResult) {
    Assert.assertEquals(expectedResult, testResult);
  }

  private WANUtil getTestableWANUtil() {
    return new WANUtil(null) {
      @Override
      ConcurrentMap<String, Serializable> getCacheConfigMap(String cacheManagerName, String cacheName) {
        return cacheConfigMap;
      }

      @Override
      ConcurrentMap<String, Serializable> getCacheManagerConfigMap(String cacheManagerName) {
        return cacheManagerConfigMap;
      }

      @Override
      void notifyClients(String cacheManagerName) {
        // Do Nothing
      }
    };
  }

}
