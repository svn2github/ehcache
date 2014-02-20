package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.config.CacheConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.toolkit.config.AbstractConfiguration;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public class CacheConfigChangeBridgeTest {

  private ToolkitCacheInternal backend;
  private CacheConfiguration cacheConfiguration;
  private ToolkitNotifier notifier;
  private TestConfiguration toolkitCacheConfig;

  @Before
  public void setUp() throws Exception {
    toolkitCacheConfig = new TestConfiguration();
    backend = when(mock(ToolkitCacheInternal.class).getConfiguration())
        .thenReturn(toolkitCacheConfig).getMock();
    cacheConfiguration = spy(new CacheConfiguration());
    notifier = mock(ToolkitNotifier.class);
  }

  @Test
  public void testConnectConfigsSetsUpLocalCacheConfiguration() throws Exception {
    CacheConfigChangeBridge bridge = new CacheConfigChangeBridge("foo",
        backend, notifier, cacheConfiguration);

    toolkitCacheConfig.internalSetConfigMapping(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME, 123);
    toolkitCacheConfig.internalSetConfigMapping(ToolkitConfigFields.MAX_TTI_SECONDS_FIELD_NAME, 321);
    toolkitCacheConfig.internalSetConfigMapping(ToolkitConfigFields.MAX_TTL_SECONDS_FIELD_NAME, 456);
    toolkitCacheConfig.internalSetConfigMapping(ToolkitConfigFields.MAX_COUNT_LOCAL_HEAP_FIELD_NAME, 42);
    toolkitCacheConfig.internalSetConfigMapping(ToolkitConfigFields.MAX_BYTES_LOCAL_HEAP_FIELD_NAME, 1L);
    toolkitCacheConfig.internalSetConfigMapping(ToolkitConfigFields.MAX_BYTES_LOCAL_OFFHEAP_FIELD_NAME, 2L);
    toolkitCacheConfig.internalSetConfigMapping(ToolkitConfigFields.OFFHEAP_ENABLED_FIELD_NAME, true);

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
  public void testEmptyConfigFromToolkitCache() throws Exception {
    CacheConfigChangeBridge bridge = new CacheConfigChangeBridge("foo",
        backend, notifier, cacheConfiguration);

    bridge.connectConfigs();

    // Empty config so only check for registration of the listener and nothing else.
    verify(cacheConfiguration).addConfigurationListener(bridge);
    verifyNoMoreInteractions(cacheConfiguration);
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

  private static class TestConfiguration extends AbstractConfiguration {
    private final Map<String, Serializable> map = new HashMap<String, Serializable>();

    @Override
    protected void internalSetConfigMapping(final String key, final Serializable value) {
      map.put(key, value);
    }

    @Override
    public Set<String> getKeys() {
      return map.keySet();
    }

    @Override
    public Serializable getObjectOrNull(final String name) {
      return map.get(name);
    }
  }
}
