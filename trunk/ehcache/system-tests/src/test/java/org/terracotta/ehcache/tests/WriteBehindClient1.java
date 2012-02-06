package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

public class WriteBehindClient1 extends AbstractWriteBehindClient {
  public WriteBehindClient1(String[] args) {
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
    new WriteBehindClient1(args).run();
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    cache.registerCacheWriter(new WriteBehindCacheWriter(this));
    for (int i = 0; i < 1000; i++) {
      cache.putWithWriter(new Element("key" + i % 200, "value" + i));
      if (0 == i % 10) {
        cache.removeWithWriter("key" + i % 200 / 10);
      }
    }

    while (getWriteCount() <= 100) {
      Thread.sleep(200);
    }
  }
}