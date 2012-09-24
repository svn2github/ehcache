/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;

import com.tc.test.config.model.TestConfig;

import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

public class CacheSeparationWriteBehindTest extends AbstractCacheTestBase {

  private static final int NODE_COUNT = 2;

  public CacheSeparationWriteBehindTest(TestConfig testConfig) {
    super("cache-separation-writebehind-test.xml", testConfig, App.class, App.class);
  }

  public static class App extends ClientBase {

    private final ToolkitBarrier     barrier;

    final ToolkitAtomicLong totalWriteCount1;
    final ToolkitAtomicLong totalDeleteCount1;
    final ToolkitAtomicLong totalWriteCount2;
    final ToolkitAtomicLong totalDeleteCount2;

    public App(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("barrier", NODE_COUNT);
      this.totalWriteCount1 = getClusteringToolkit().getAtomicLong("long1");
      this.totalDeleteCount1 = getClusteringToolkit().getAtomicLong("long2");
      this.totalWriteCount2 = getClusteringToolkit().getAtomicLong("long3");
      this.totalDeleteCount2 = getClusteringToolkit().getAtomicLong("long3");
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      final int index = barrier.await();

      Cache cache1 = cacheManager.getCache("test1");
      Cache cache2 = cacheManager.getCache("test2");

      WriteBehindCacheWriter writer1;
      WriteBehindCacheWriter writer2;

      if (0 == index) {
        writer1 = new WriteBehindCacheWriter("WriteBehindCacheWriter1", index, 20L);
        cache1.registerCacheWriter(writer1);
        writer2 = new WriteBehindCacheWriter("WriteBehindCacheWriter2", index, 20L);
        cache2.registerCacheWriter(writer2);

        for (int i = 0; i < 1000; i++) {
          cache1.putWithWriter(new Element("key" + i % 200, "value" + i));
          if (0 == i % 10) {
            cache1.removeWithWriter("key" + i % 200 / 10);
          }
        }

        for (int i = 0; i < 100; i++) {
          cache2.putWithWriter(new Element("key" + i % 200, "value" + i));
          if (0 == i % 10) {
            cache2.removeWithWriter("key" + i % 200 / 10);
          }
        }
      } else {
        writer1 = new WriteBehindCacheWriter("WriteBehindCacheWriter1", index, 10L);
        cache1.registerCacheWriter(writer1);
        writer2 = new WriteBehindCacheWriter("WriteBehindCacheWriter2", index, 10L);
        cache2.registerCacheWriter(writer2);

        cache1.putWithWriter(new Element("key", "value"));
        cache1.removeWithWriter("key");
        cache2.putWithWriter(new Element("key", "value"));
        cache2.removeWithWriter("key");
      }

      TimeUnit.MINUTES.sleep(20);
      barrier.await();

      System.out.println("[Client " + index + " processed " + writer1.getWriteCount() + " writes for writer 1]");
      System.out.println("[Client " + index + " processed " + writer2.getWriteCount() + " writes for writer 2]");
      System.out.println("[Client " + index + " processed " + writer1.getDeleteCount() + " deletes for writer 1]");
      System.out.println("[Client " + index + " processed " + writer2.getDeleteCount() + " deletes for writer 2]");

      totalWriteCount1.addAndGet(writer1.getWriteCount());
      totalDeleteCount1.addAndGet(writer1.getDeleteCount());
      totalWriteCount2.addAndGet(writer2.getWriteCount());
      totalDeleteCount2.addAndGet(writer2.getDeleteCount());

      barrier.await();

      if (0 == index) {
        System.out.println("[Clients processed a total of " + totalWriteCount1.get() + " writes for writer 1]");
        System.out.println("[Clients processed a total of " + totalWriteCount2.get() + " writes for writer 2]");
        System.out.println("[Clients processed a total of " + totalDeleteCount1.get() + " deletes for writer 1]");
        System.out.println("[Clients processed a total of " + totalDeleteCount2.get() + " deletes for writer 2]");

        Assert.assertEquals(1001, totalWriteCount1.get());
        Assert.assertEquals(112, totalWriteCount2.get());
        Assert.assertEquals(101, totalDeleteCount1.get());
        Assert.assertEquals(112, totalDeleteCount2.get());
      }

      barrier.await();
    }

  }
}
