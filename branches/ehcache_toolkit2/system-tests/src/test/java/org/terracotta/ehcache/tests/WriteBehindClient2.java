package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;

import java.util.concurrent.TimeUnit;

public class WriteBehindClient2 extends AbstractWriteBehindClient {
  public WriteBehindClient2(String[] args) {
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
    new WriteBehindClient2(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    cache.registerCacheWriter(new WriteBehindCacheWriter(this));
    cache.putWithWriter(new Element("key", "value"));
    cache.removeWithWriter("key");
    TimeUnit.MINUTES.sleep(20);
  }
}