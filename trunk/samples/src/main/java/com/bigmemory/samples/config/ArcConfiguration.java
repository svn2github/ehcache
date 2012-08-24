package com.bigmemory.samples.config;


import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

/**
 *
 * Size based config using ARC, defining a clustered cache in the XML file and dynamically adding one
 * so they share the memory allocated by ARC
 *
 * The two caches share 1go of local heap memory and 5go of offheap memory
 */
public class ArcConfiguration {

  public void ehcacheArc() {
    CacheManager manager = CacheManager.newInstance(getClass().getResource("/arc/ehcache.xml"));

    Cache testCache = manager.getCache("sample-offheap-cache");   // ttl = 40 and tti 20
    //your cache is now ready.

    manager.addCache("sample-cache");
    Cache testCacheFromDefaultCache = manager.getCache("sample-cache");   // ttl = 60 and tti 30


    manager.shutdown();

  }

}
