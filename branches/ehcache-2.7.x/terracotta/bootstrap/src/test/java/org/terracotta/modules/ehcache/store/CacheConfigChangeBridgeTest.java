package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.config.CacheConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public class CacheConfigChangeBridgeTest {

  private ToolkitCacheInternal backend;
  private CacheConfiguration cacheConfiguration;
  private ToolkitNotifier notifier;
  private Configuration toolkitCacheConfig;

  @Before
  public void setUp() throws Exception {
    toolkitCacheConfig = mock(Configuration.class);
    backend = when(mock(ToolkitCacheInternal.class).getConfiguration())
        .thenReturn(toolkitCacheConfig).getMock();
    cacheConfiguration = spy(new CacheConfiguration());
    notifier = mock(ToolkitNotifier.class);
  }

  @Test
  public void testConnectConfigsSetsUpLocalCacheConfiguration() throws Exception {
    CacheConfigChangeBridge bridge = new CacheConfigChangeBridge("foo",
        backend, notifier, cacheConfiguration);

    when(toolkitCacheConfig.getInt(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME)).thenReturn(123);
    when(toolkitCacheConfig.getInt(ToolkitConfigFields.MAX_TTI_SECONDS_FIELD_NAME)).thenReturn(321);
    when(toolkitCacheConfig.getInt(ToolkitConfigFields.MAX_TTL_SECONDS_FIELD_NAME)).thenReturn(456);
    when(toolkitCacheConfig.getInt(ToolkitConfigFields.MAX_COUNT_LOCAL_HEAP_FIELD_NAME)).thenReturn(42);
    when(toolkitCacheConfig.getLong(ToolkitConfigFields.MAX_BYTES_LOCAL_HEAP_FIELD_NAME)).thenReturn(1L);
    when(toolkitCacheConfig.getLong(ToolkitConfigFields.MAX_BYTES_LOCAL_OFFHEAP_FIELD_NAME)).thenReturn(2L);
    when(toolkitCacheConfig.getBoolean(ToolkitConfigFields.OFFHEAP_ENABLED_FIELD_NAME)).thenReturn(true);

    bridge.connectConfigs();

    assertThat(cacheConfiguration.getMaxEntriesInCache(), is(123L));
    assertThat(cacheConfiguration.getTimeToLiveSeconds(), is(456L));
    assertThat(cacheConfiguration.getTimeToIdleSeconds(), is(321L));
    assertThat(cacheConfiguration.isEternal(), is(false));
    assertThat(cacheConfiguration.getMaxEntriesLocalHeap(), is(42L));
    assertThat(cacheConfiguration.getMaxBytesLocalHeap(), is(1L));
    assertThat(cacheConfiguration.getMaxBytesLocalOffHeap(), is(2L));
    assertThat(cacheConfiguration.isOverflowToOffHeap(), is(true));
  }

  @Test
  public void testOverrideLocallyConfiguredExpiry() throws Exception {
    CacheConfigChangeBridge bridge = new CacheConfigChangeBridge("foo",
        backend, notifier, cacheConfiguration);

    cacheConfiguration.setEternal(false);
    cacheConfiguration.setTimeToLiveSeconds(123);
    cacheConfiguration.setTimeToIdleSeconds(321);

    bridge.connectConfigs();

    assertThat(cacheConfiguration.isEternal(), is(true));
    assertThat(cacheConfiguration.getTimeToLiveSeconds(), is(0L));
    assertThat(cacheConfiguration.getTimeToIdleSeconds(), is(0L));
  }
}
