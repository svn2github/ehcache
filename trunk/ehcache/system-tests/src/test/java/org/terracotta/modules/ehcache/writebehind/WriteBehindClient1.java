package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.writebehind.WriteBehindManager;

import org.terracotta.ehcache.tests.AbstractWriteBehindClient;
import org.terracotta.ehcache.tests.WriteBehindCacheWriter;
import org.terracotta.toolkit.Toolkit;

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
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    cache.registerCacheWriter(new WriteBehindCacheWriter(this));
    for (int i = 0; i < 1000; i++) {
      cache.putWithWriter(new Element("key" + i % 200, "value" + i)); // 200 different keys, 1000 write operation
      if (0 == i % 10) {
        cache.removeWithWriter("key" + i % 200 / 10); // 20 different keys, 100 delete operation
      }
    }

    while (getWriteCount() < 100) {
      Thread.sleep(200);
    }
    WriteBehindManager wbManager = ((WriteBehindManager) cache.getWriterManager());
    System.out.println("write behind queue size " + wbManager.getQueueSize());
  }
}