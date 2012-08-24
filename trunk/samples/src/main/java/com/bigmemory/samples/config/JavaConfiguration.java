package com.bigmemory.samples.config;
/**
 * Size-based config for a clustered cache using BigMemory, using a programmatic config
 *
 * Released to the public domain, as explained at  http://creativecommons.org/licenses/publicdomain
 */

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

public class JavaConfiguration {

  /**
   * defining the cache when configuring the CacheManager
   */
  public void configuringACache() {

    Configuration managerConfiguration = new Configuration();
    managerConfiguration.updateCheck(true)
        .monitoring(Configuration.Monitoring.AUTODETECT)
        .name("cacheManagerCompleteExample")
        .dynamicConfig(true).terracotta(new TerracottaClientConfiguration().url("localhost:9510"))
        .cache(new CacheConfiguration()
            .name("sample-offheap-cache")
            .maxBytesLocalHeap(1, MemoryUnit.GIGABYTES)
            .timeToLiveSeconds(60)
            .timeToIdleSeconds(30)
            .maxBytesLocalOffHeap(5, MemoryUnit.GIGABYTES)
            .terracotta(new TerracottaConfiguration().clustered(true)
                .consistency(TerracottaConfiguration.Consistency.STRONG)));

    CacheManager manager = CacheManager.create(managerConfiguration);

    manager.getCache("sample-offheap-cache");
    //your cache is now ready.


    manager.shutdown();
  }

  /**
   * dynamically adding a cache
   */
  public void dynamicallyAddingACache() {

    Configuration managerConfiguration = new Configuration();
    managerConfiguration.updateCheck(true)
        .monitoring(Configuration.Monitoring.AUTODETECT)
        .name("cacheManagerCompleteExample")
        .dynamicConfig(true).terracotta(new TerracottaClientConfiguration().url("localhost:9510"));

    CacheManager manager = CacheManager.create(managerConfiguration);

    Cache testCache = new Cache(
        new CacheConfiguration()
            .name("sample-offheap-cache")
            .maxBytesLocalHeap(1, MemoryUnit.GIGABYTES)
            .timeToLiveSeconds(60)
            .timeToIdleSeconds(30)
            .maxBytesLocalOffHeap(5, MemoryUnit.GIGABYTES)
            .terracotta(new TerracottaConfiguration().clustered(true)
                .consistency(TerracottaConfiguration.Consistency.STRONG)));
    manager.addCache(testCache);


    //your cache is now ready.


    manager.shutdown();
  }
}
