package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CacheStoreHelper;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.event.RegisteredEventListeners;
import org.junit.Before;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitProperties;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public abstract class AbstractClusteredStoreTest {
  protected Ehcache cache = mock(Ehcache.class);
  protected CacheConfiguration cacheConfiguration = new CacheConfiguration().terracotta(new TerracottaConfiguration().clustered(true).consistency(TerracottaConfiguration.Consistency.EVENTUAL));
  protected Configuration configuration = new Configuration().name("ClusteredStoreTest-cm").terracotta(new TerracottaClientConfiguration());
  protected ToolkitCacheInternal toolkitCacheInternal = mock(ToolkitCacheInternal.class);
  protected ClusteredStore clusteredStore;
  private ToolkitInstanceFactory toolkitInstanceFactory = mock(ToolkitInstanceFactory.class);
  protected CacheCluster cacheCluster = mockCacheCluster("abc");
  private CacheManager cacheManager = when(mock(CacheManager.class).getConfiguration()).thenReturn(configuration).getMock();
  protected ToolkitMap configMap = mock(ToolkitMap.class);
  private ToolkitInternal toolkitInternal = mock(ToolkitInternal.class);
  private ToolkitProperties toolkitProperties = mock(ToolkitProperties.class);
  private org.terracotta.toolkit.config.Configuration toolkitCacheConfiguration = mock(org.terracotta.toolkit.config.Configuration.class);
  private ToolkitNotifier toolkitNotifier = mock(ToolkitNotifier.class);
  private CacheStoreHelper cacheStoreHelper = mock(CacheStoreHelper.class);
  private ToolkitLock toolkitLock = mock(ToolkitLock.class);

  @Before
  public void setUpClusteredStore() {
    when(cache.getCacheConfiguration()).thenReturn(cacheConfiguration);
    when(cache.getCacheManager()).thenReturn(cacheManager);
    when(cache.getName()).thenReturn("ClusteredStoreTest-cache");
    when(cache.getStatus()).thenReturn(Status.STATUS_ALIVE);
    when(cache.getCacheEventNotificationService()).thenReturn(new RegisteredEventListeners(cache, cacheStoreHelper));
    when(cacheManager.getName()).thenReturn("ClusteredStoreTest-cm");
    when(toolkitInstanceFactory.getOrCreateClusteredStoreConfigMap(eq("ClusteredStoreTest-cm"), eq("ClusteredStoreTest-cache"))).thenReturn(configMap);
    when(toolkitInstanceFactory.getToolkit()).thenReturn(toolkitInternal);
    when(toolkitInstanceFactory.getLockForCache(any(Ehcache.class), anyString())).thenReturn(toolkitLock);
    when(toolkitInternal.getProperties()).thenReturn(toolkitProperties);
    when(toolkitProperties.getBoolean(anyString())).thenReturn(false);
    when(toolkitInstanceFactory.getOrCreateToolkitCache(cache)).thenReturn(toolkitCacheInternal);
    when(toolkitCacheInternal.getConfiguration()).thenReturn(toolkitCacheConfiguration);
    when(toolkitCacheConfiguration.getInt(anyString())).thenReturn(1);
    when(toolkitInstanceFactory.getOrCreateConfigChangeNotifier(eq(cache))).thenReturn(toolkitNotifier);
    clusteredStore = new ClusteredStore(toolkitInstanceFactory, cache, cacheCluster) {
      @Override
      void setUpWanConfig() {
        // Do Nothing
      }
    };
    when(cacheStoreHelper.getStore()).thenReturn(clusteredStore);
  }

  private static CacheCluster mockCacheCluster(String thisNode) {
    CacheCluster cacheCluster = mock(CacheCluster.class);
    ClusterNode node = when(mock(ClusterNode.class).getId()).thenReturn(thisNode).getMock();
    when(cacheCluster.getCurrentNode()).thenReturn(node);
    return cacheCluster;
  }
}
