/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.writebehind.WriteBehindManager;

import org.terracotta.ehcache.tests.AbstractWriteBehindClient;
import org.terracotta.ehcache.tests.WriteBehindCacheWriter;
import org.terracotta.toolkit.Toolkit;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Assert;

public class BasicWriteBehindTestClient extends AbstractWriteBehindClient {
  public static int ELEMENT_COUNT = BasicWriteBehindTest.ELEMENT_COUNT;

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
  protected void runTest(final Cache cache, Toolkit toolkit) throws Throwable {
    cache.registerCacheWriter(new WriteBehindCacheWriter(this));
    for (int i = 0; i < ELEMENT_COUNT; i++) {
      cache.putWithWriter(new Element("key" + i % 200, "value" + i)); // 200 different keys, write operation
      if (0 == i % 10) {
        cache.removeWithWriter("key" + i % 200 / 10); // 10 different keys, delete operation
      }
    }

    final WriteBehindManager wbManager = ((WriteBehindManager) cache.getWriterManager());
    System.out.println("write behind queue size " + wbManager.getQueueSize());
    final AtomicLong counter = new AtomicLong();
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    executor.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        long count = counter.incrementAndGet();
        cache.putWithWriter(new Element("key-" + count, "value-" + count));
        System.out.println("executor write behind queue size " + wbManager.getQueueSize() + " counter " + count);
      }
    }, 500L, 1L, TimeUnit.MILLISECONDS);

    // done with put now shutdown cache manager
    // this call should wait write behind queue to get empty
    Thread.sleep(TimeUnit.SECONDS.toMillis(1L));
    System.out.println("calling cacheManager shutdown");
    cache.getCacheManager().shutdown();
    
    try {
      wbManager.getQueueSize();
      Assert.fail("should have failed because cacheManager.shutdown is called before");
    } catch (IllegalStateException e) {
      // expected exception
    }
  }
}
