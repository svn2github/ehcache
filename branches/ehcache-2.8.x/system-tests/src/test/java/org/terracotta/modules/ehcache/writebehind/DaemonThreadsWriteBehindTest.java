/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.writebehind.WriteBehindManager;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.AbstractWriteBehindClient;
import org.terracotta.ehcache.tests.WriteBehindCacheWriter;
import org.terracotta.modules.ehcache.async.AsyncCoordinatorImpl;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;

import com.tc.l2.L2DebugLogging.LogLevel;
import com.tc.test.config.model.TestConfig;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import junit.framework.Assert;

public class DaemonThreadsWriteBehindTest extends AbstractCacheTestBase {

  public DaemonThreadsWriteBehindTest(TestConfig testConfig) {
    super("basic-writebehind-test.xml", testConfig, DaemonThreadsWriteBehindTestClient.class);
    configureTCLogging(AsyncCoordinatorImpl.class.getName(), LogLevel.DEBUG);
  }

  public static class DaemonThreadsWriteBehindTestClient extends AbstractWriteBehindClient {

    public DaemonThreadsWriteBehindTestClient(String[] args) {
      super(args);

    }

    @Override
    public void doTest() throws Throwable {
      final ThreadMXBean tbean;
      tbean = ManagementFactory.getThreadMXBean();

      final int nonDaemonThreadCountA = tbean.getThreadCount() - tbean.getDaemonThreadCount();
      final int daemonThreadCountA = tbean.getDaemonThreadCount();
      final long[] listA = tbean.getAllThreadIds();
      for (int loopNumber = 0; loopNumber < 4; loopNumber++) {
        cacheManager = new CacheManager(
                                        DaemonThreadsWriteBehindTestClient.class
                                            .getResourceAsStream("/ehcache-config.xml"));
        int daemonThreadCountB = tbean.getDaemonThreadCount();
        Assert.assertTrue(daemonThreadCountA < daemonThreadCountB);
        Cache cache = cacheManager.getCache("test");
        cache.registerCacheWriter(new WriteBehindCacheWriter(this));
        Assert.assertNotNull(cache.getWriterManager());
        Assert.assertTrue(cache.getWriterManager() instanceof WriteBehindManager);
        for (int i = 0; i < 10; i++) {
          cache.putWithWriter(new Element(i, i));
        }
        while (getWriteCount() < 10) {
          Thread.sleep(200);
        }
        resetWriteCount();
        cacheManager.shutdown();
        System.out.println("done with iteration " + loopNumber);
      }

      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          return anyDaemonThreadLeft(tbean, listA, daemonThreadCountA, nonDaemonThreadCountA);
        }
      });

    }

    private boolean anyDaemonThreadLeft(ThreadMXBean tbean, long[] listA, int daemonThreadCountA, int nonDaemonThreadCountA) {
      long[] listC = tbean.getAllThreadIds();
      int daemonThreadCountC = tbean.getDaemonThreadCount();
      int nonDaemonThreadCountC = tbean.getThreadCount() - tbean.getDaemonThreadCount();
      List<Long> listIntA = new ArrayList<Long>();
      for (long threadId : listA) {
        listIntA.add(threadId);
      }
      List<Long> listIntC = new ArrayList<Long>();
      for (long threadId : listC) {
        listIntC.add(threadId);
      }
      listIntC.removeAll(listIntA);

      Set<String> knownThreads = getKnownThreads();
      int skipThreadCount = 0;
      StringBuffer threadsInfo = new StringBuffer();
      System.out.println("\n\n" + listIntC.size() + " Start Printing Stack Trace\n--------------------");
      for (Long threadId : listIntC) {
        ThreadInfo tinfo = tbean.getThreadInfo(threadId);
        if (knownThreads.contains(tinfo.getThreadName().trim())) {
          ++skipThreadCount;
          continue;
        }
        String info = "Thread name: " + tinfo.getThreadName() + " | " + tinfo.getThreadId() + "\n";
        threadsInfo.append(info);
        if (tinfo.getStackTrace().length == 0) {
          ++skipThreadCount;
        }
        for (StackTraceElement e : tinfo.getStackTrace()) {
          threadsInfo.append(e).append("\n\n");
        }
      }
      System.out.println(threadsInfo + "\n\n-----------------------\n\n");
      return ((daemonThreadCountA == (daemonThreadCountC - skipThreadCount)) && (nonDaemonThreadCountA == nonDaemonThreadCountC));
    }

    private static Set<String> getKnownThreads() {
      Set<String> skipThreads = new HashSet<String>();
      skipThreads.add("Attach Listener");
      skipThreads.add("Poller SunPKCS11-Darwin");
      skipThreads.add("Finalizer");
      skipThreads.add("AWT-AppKit");
      return skipThreads;
    }

    @Override
    protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
      // NO OP

    }

    @Override
    public long getSleepBetweenWrites() {
      return 0;
    }

    @Override
    public long getSleepBetweenDeletes() {
      return 0;
    }
  }

}
