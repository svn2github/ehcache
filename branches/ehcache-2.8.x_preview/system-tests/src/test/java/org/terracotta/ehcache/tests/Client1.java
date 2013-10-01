package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;

import org.terracotta.toolkit.Toolkit;

import junit.framework.Assert;

public class Client1 extends ClientBase {

  public Client1(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new Client1(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    cache.put(new Element("key", "value"));

    testIsKeyInCache(true, cache.getCacheManager());
  }

  public static void testIsKeyInCache(final boolean populateCache, CacheManager cacheManager) {
    Cache eventualCache = getCache("eventualCache", TerracottaConfiguration.Consistency.EVENTUAL);
    cacheManager.addCache(eventualCache);
    Cache strongCache = getCache("strongCache", TerracottaConfiguration.Consistency.STRONG);
    cacheManager.addCache(strongCache);

    System.out.println("Testing eventual cache");
    checkIsKeyInCache(populateCache, eventualCache);
    System.out.println("Testing strong cache");
    checkIsKeyInCache(populateCache, strongCache);
  }

  private static void checkIsKeyInCache(final boolean populateCache, Cache cache) {
    final int total = 10;
    for (int i = 0; i < total; i++) {
      System.out.println("Testing isKeyInCache for: " + i + "/" + total);
      final String key = "some-key-" + i;
      final String value = "some-value-" + i;
      if (populateCache) {
        cache.put(new Element(key, value));
      }
      final long end = System.currentTimeMillis() + 3000;
      while (System.currentTimeMillis() < end) {
        Assert.assertTrue("isKeyInCache() should return true for key: " + key, cache.isKeyInCache(key));
        Element element = cache.get(key);
        Assert.assertNotNull("element should not be null", element);
        Assert.assertEquals(key, element.getKey());
        Assert.assertEquals(value, element.getValue());
      }
    }
  }

  private static Cache getCache(String name, Consistency consistency) {
    return new Cache(new CacheConfiguration().name(name).maxEntriesLocalHeap(100)
        .terracotta(new TerracottaConfiguration().clustered(true).consistency(consistency)));
  }
}