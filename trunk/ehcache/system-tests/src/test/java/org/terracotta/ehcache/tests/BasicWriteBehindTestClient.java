/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.writebehind.WriteBehindManager;

import org.terracotta.toolkit.Toolkit;

public class BasicWriteBehindTestClient extends AbstractWriteBehindClient {

  public BasicWriteBehindTestClient(String[] args) {
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
    new BasicWriteBehindTestClient(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    cache.registerCacheWriter(new WriteBehindCacheWriter(this));
    for (int i = 0; i < 1000; i++) {
      cache.putWithWriter(new Element("key" + i % 200, "value" + i)); // 200 different keys, write operation
      if (0 == i % 10) {
        cache.removeWithWriter("key" + i % 200 / 10); // 10 different keys, delete operation
      }
    }

    WriteBehindManager wbManager = ((WriteBehindManager) cache.getWriterManager());
    System.out.println("write behind queue size " + wbManager.getQueueSize());

    // done with put now shutdown cache manager
    // this call should wait write behind queue get empty
    cache.getCacheManager().shutdown();
  }
}
