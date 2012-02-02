package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

public class SerializationWriteBehindClient1 extends AbstractWriteBehindClient {
  public SerializationWriteBehindClient1(String[] args) {
    super(args);
  }

  @Override
  public long getSleepBetweenWrites() {
    return 100L;
  }

  @Override
  public long getSleepBetweenDeletes() {
    return 100L;
  }

  public static void main(String[] args) {
    new SerializationWriteBehindClient1(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    cache.registerCacheWriter(new WriteBehindCacheWriter(this));
    for (int i = 0; i < 1000; i++) {
      cache.putWithWriter(new Element(new SerializationWriteBehindType("key" + i % 200), new SerializationWriteBehindType("value" + i)));
      if (0 == i % 10) {
        cache.removeWithWriter(new SerializationWriteBehindType("key" + i % 200 / 10));
      }
    }

    while (getWriteCount() <= 100) {
      Thread.sleep(200);
    }
  }
}