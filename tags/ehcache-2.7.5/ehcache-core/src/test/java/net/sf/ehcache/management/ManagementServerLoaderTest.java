package net.sf.ehcache.management;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ludovic Orban
 */
public class ManagementServerLoaderTest {

  @Test
  public void testRegistrationLifecycle() throws Exception {
    boolean managementAvailable = ManagementServerLoader.isManagementAvailable();
    assertThat(managementAvailable, is(true));

    CacheManager cacheManager1 = createCacheManager("cm-one", "localhost:1234");
    CacheManager cacheManager2 = createCacheManager("cm-two", "localhost:1234");

    try {
      ManagementServerLoader.register(cacheManager1, null, cacheManager1.getConfiguration().getManagementRESTService());
      assertThat(ManagementServerImpl.status, is(ManagementServerImpl.Status.STARTED));
      ManagementServerLoader.register(cacheManager2, null, cacheManager2.getConfiguration().getManagementRESTService());
      assertThat(ManagementServerImpl.status, is(ManagementServerImpl.Status.STARTED));

      ManagementServerLoader.unregister(cacheManager2.getConfiguration()
          .getManagementRESTService()
          .getBind(), cacheManager2);
      assertThat(ManagementServerImpl.status, is(ManagementServerImpl.Status.STARTED));
      ManagementServerLoader.unregister(cacheManager1.getConfiguration()
          .getManagementRESTService()
          .getBind(), cacheManager1);
      assertThat(ManagementServerImpl.status, is(ManagementServerImpl.Status.STOPPED));
    } finally {
      cacheManager2.shutdown();
      cacheManager1.shutdown();
    }
  }

  private static net.sf.ehcache.CacheManager createCacheManager(String name, String bind) {
    Configuration cfg = new Configuration().name(name);
    ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
    managementRESTServiceConfiguration.setBind(bind);
    cfg.managementRESTService(managementRESTServiceConfiguration);
    return new net.sf.ehcache.CacheManager(cfg);
  }

}
