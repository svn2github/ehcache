package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;

import org.terracotta.api.ClusteringToolkit;

public class SerializationWriteBehindClient2 extends AbstractWriteBehindClient {
  public SerializationWriteBehindClient2(String[] args) {
    super(args);
  }

  @Override
  public long getSleepBetweenWrites() {
    return 10L;
  }

  @Override
  public long getSleepBetweenDeletes() {
    return 10L;
  }

  public static void main(String[] args) {
    new SerializationWriteBehindClient2(args).run();
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    cache.registerCacheWriter(new WriteBehindCacheWriter(this));
    Thread.sleep(60000);
  }
}