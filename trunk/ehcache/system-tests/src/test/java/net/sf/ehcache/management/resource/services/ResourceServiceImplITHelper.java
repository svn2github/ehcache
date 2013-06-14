package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;

/**
 * @author: Anthony Dahanne
 */
public abstract class ResourceServiceImplITHelper {

  CacheManager getCacheManagerProgramatically() {
    Configuration configuration = new Configuration();
    configuration.setName("testCacheManagerProgrammatic");
    configuration.setMaxBytesLocalDisk("10M");
    configuration.setMaxBytesLocalHeap("5M");

    CacheConfiguration myCache = new CacheConfiguration()
            .eternal(false).name("testCache2");
    configuration.addCache(myCache);
    ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
    managementRESTServiceConfiguration.setBind("0.0.0.0:12121");
    managementRESTServiceConfiguration.setEnabled(true);
    configuration.addManagementRESTService(managementRESTServiceConfiguration);

    CacheManager mgr = new CacheManager(configuration);
    Cache exampleCache = mgr.getCache("testCache2");
    assert (exampleCache != null);
    return mgr;
  }


  public static void main(String[] args) {
    ResourceServiceImplITHelper resourceServiceImplITHelper = new ResourceServiceImplITHelper() {
      @Override
      CacheManager getCacheManagerProgramatically() {
        return super.getCacheManagerProgramatically();
      }
    };
    resourceServiceImplITHelper.getCacheManagerProgramatically();

  }

}
