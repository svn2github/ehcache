package org.terracotta.modules.ehcache;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.toolkit.internal.cache.BufferingToolkitCache;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Eugene Shelestovich
 */
public class WanAwareToolkitCacheTest {

  private static final String                  VALUE = "v1";
  private static final String                  KEY   = "k1";
  private BufferingToolkitCache<String, String> delegate;
  private ConcurrentMap<String, Serializable>  configMap;
  private WanAwareToolkitCache<String, String> wanAwareCache;
  private boolean                              waitHappened;

  @Before
  public void setUp() {
    this.waitHappened = false;
    delegate = mock(BufferingToolkitCache.class);
    configMap = new ConcurrentHashMap<String, Serializable>();

    wanAwareCache = getTestableWanAwareToolkitCache();
  }

  private WanAwareToolkitCache<String, String> getTestableWanAwareToolkitCache() {
    return new WanAwareToolkitCache<String, String>(delegate, configMap, null, null, null) {
      @Override
      void waitUntilActive() {
        waitHappened = true;
      }

      @Override
      void notifyClients() {
        // Do Nothing
      }
    };
  }


  @Test
  public void testCacheMustBeInactiveByDefault() {
    // After setup, a new wan-aware cache is inactive by default
    Assert.assertFalse(wanAwareCache.isActive());
  }

  @Test
  public void testClientShouldNotWaitWhenCacheInactive() {
    whenCacheIsActive().andClientPerformsPutOperation().assertWaitHappened(false).andAssertPutCallDelegatedToCache();
  }

  @Test
  public void testClientShouldWaitWhenCacheIsNotActive() {
    whenCacheIsNotActive().andClientPerformsPutOperation().assertWaitHappened(true).andAssertPutCallDelegatedToCache();
  }

  @Test
  public void testOrchestratorShouldNotWaitWhenCacheInactive() {
    whenCacheIsNotActive().andOrchestratorPerformsOperation().assertWaitHappened(false);
  }

  private WanAwareToolkitCacheTest andOrchestratorPerformsOperation() {
    wanAwareCache.putVersioned(KEY, VALUE, 0);
    wanAwareCache.removeVersioned(KEY, 0);
    wanAwareCache.clearVersioned();
    return this;
  }

  private void andAssertPutCallDelegatedToCache() {
    verify(delegate).put(KEY, VALUE);
  }

  private WanAwareToolkitCacheTest assertWaitHappened(boolean expected) {
    Assert.assertEquals(expected, waitHappened);
    return this;
  }

  private WanAwareToolkitCacheTest andClientPerformsPutOperation() {
    wanAwareCache.put(KEY, VALUE);
    return this;
  }

  private WanAwareToolkitCacheTest whenCacheIsActive() {
    wanAwareCache.activate();
    return this;
  }

  private WanAwareToolkitCacheTest whenCacheIsNotActive() {
    wanAwareCache.deactivate();
    return this;
  }

}
