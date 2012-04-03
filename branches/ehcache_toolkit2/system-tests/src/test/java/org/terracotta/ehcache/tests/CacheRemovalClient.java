package org.terracotta.ehcache.tests;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import net.sf.ehcache.Cache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.cluster.ClusterInfo;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author Alex Snaps
 */
public class CacheRemovalClient extends ClientBase {

  public CacheRemovalClient(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new Client1(args).run();
  }

  @Override
  protected void runTest(final Cache cache, final ClusteringToolkit toolkit) throws Throwable {
    cacheManager.removeCache(cache.getName());
    for (int i = 0; i < 10; i++) {
      addAndRemove("test-" + i);
    }

    // CacheManager has seen 11 caches (including the 'test' one), ...
    final ClusterInfo clusterInfo = toolkit.getClusterInfo();
    List listeners = getValueOfDeclaredField("listeners", getValueOfDeclaredField("dsoCluster", clusterInfo));
    final int size = listeners.size();
    System.err.println("Found " + size + " listeners :");
    for (Object listener : listeners) {
      System.err.println(" - " + listener + " wrapping " + getValueOfDeclaredField("listener", listener));
    }

    // .. but there shouldn't be any listeners associated with these, but the "main" listener from the CacheManager
    cacheManager.shutdown();
    assertThat(size - 1, is(0));
    assertThat(getValueOfDeclaredField("listener", listeners.get(0)).getClass().getName(),
               equalTo("org.terracotta.modules.ehcache.event.ClusterListenerAdapter"));
  }

  private void addAndRemove(final String name) {
    cacheManager.addCache(new Cache(new CacheConfiguration(name, 100).terracotta(new TerracottaConfiguration()
        .clustered(true))));
    cacheManager.removeCache(name);
  }

  private static <T> T getValueOfDeclaredField(final String fieldName, final Object target) {
    try {
      final Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return (T) field.get(target);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
