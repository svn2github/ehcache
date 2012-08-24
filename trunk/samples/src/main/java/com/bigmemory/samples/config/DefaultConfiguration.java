package com.bigmemory.samples.config;
/**
 *
 *  Count-based config, defining a default cache (clustered, using offheap) and dynamically adding
 *  a cache instance
 *
 * Released to the public domain, as explained at  http://creativecommons.org/licenses/publicdomain
 */

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

public class DefaultConfiguration {

  public void ehcacheUsingDefaultCacheConfiguration() {

    Configuration managerConfiguration = new Configuration();
    managerConfiguration.updateCheck(true)
        .monitoring(Configuration.Monitoring.AUTODETECT)
        .name("cacheManagerCompleteExample")
        .dynamicConfig(true).terracotta(new TerracottaClientConfiguration().url("localhost:9510"))
        .addDefaultCache(
            new CacheConfiguration().maxEntriesLocalHeap(1000)
                .maxMemoryOffHeap("1G")
                .terracotta(new TerracottaConfiguration().consistency(TerracottaConfiguration.Consistency.STRONG)));

    CacheManager manager = CacheManager.create(managerConfiguration);

    manager.addCache("testCache");
    Cache testCache = manager.getCache("testCache");
    //your cache is now ready.


    manager.shutdown();
  }


}
