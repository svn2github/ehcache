package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.writebehind.WriteBehindManager;

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
    WriteBehindManager wbManager = ((WriteBehindManager) cache.getWriterManager());
    long size = wbManager.getQueueSize();
    System.out.println("sleeping 1 min write behind queue size " + size);
    TimeUnit.MINUTES.sleep(1L);
  }
}