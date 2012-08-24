package com.bigmemory.samples.cache;
/*
 * Released to the public domain, as explained at  http://creativecommons.org/licenses/publicdomain
 */

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;

import java.io.IOException;

public class EhcacheCrudPlaying {
  public static void main(String[] args) throws IOException {
    Configuration configuration = new Configuration();
    CacheManager cacheManager = new CacheManager(configuration.maxBytesLocalHeap(10, MemoryUnit.MEGABYTES)
        .name("new-cacheManager"));

    CacheConfiguration cacheConfiguration = new CacheConfiguration("one", 0);
    cacheManager.addCache(new Cache(cacheConfiguration));

    Cache cache = cacheManager.getCache("one");

    //put value
    System.out.println("**** Put key1 / value1 into the cache ****");
    cache.put(new Element("key1", "value1"));
    read();

    //get value
    System.out.println("**** Retrieve key1 from cache. ****");
    final Element key1 = cache.get("key1");
    System.out.println("Value is " + key1.getValue());
    read();

    //update value
    System.out.println("**** Update value for key1 to value2 ****");
    cache.put(new Element("key1", "value2"));
    read();

    //get value
    System.out.println("Retrieve key1 from cache.");
    final Element key1Updated = cache.get("key1");
    System.out.println("Value is " + key1Updated.getValue() + ". key1 has been updated.");
    read();

    //delete value
    System.out.println("**** Delete key1 from cache. ****");
    cache.remove("key1");
    read();

    //get value
    System.out.println("Retrieve key1 from cache.");
    final Element key1Removed = cache.get("key1");
    System.out.println("Value is " + key1Removed + ". key1 has been deleted");

    cacheManager.shutdown();

  }

  private static void read() throws IOException {
    System.err.println("\nhit enter to continue");
    System.in.read();
  }

}
