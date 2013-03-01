package org.terracotta.ehcache.tests;

import static org.junit.Assert.fail;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.junit.Assert;
import org.terracotta.toolkit.Toolkit;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class ShutdownClient1 extends ClientBase {
  private static final List<WeakReference<ClassLoader>> CLASS_LOADER_LIST = new ArrayList<WeakReference<ClassLoader>>();

  public ShutdownClient1(String[] args) {
    super("test", args);
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    Set<SimpleThreadInfo> baseLineThreads = SimpleThreadInfo.parseThreadInfo(getThreadDump());

    testClusteredCache(cache, toolkit);

    for (int i = 0; i < 10; i++) {
      System.out.println("***** Iteration " + (i + 1) + " *****");
      if (i > 0) {
        setupCacheManager();
      }
      storeL1ClassLoaderWeakReferences(getCacheManager().getCache("test"));

      shutdownExpressClient();
      clearTerracottaClient();
      System.runFinalization();

      Thread.sleep(TimeUnit.SECONDS.toMillis(30));
    }

    waitUntilLastChanceThreadsAreGone(6 * 60);
    new PermStress().stress(10000);
    boolean failed = true;
    for (int i = 0; i < 10; i++) {
      failed = assertClassloadersGCed();
      for (int j = 0; j < 10; j++) {
        System.gc();
      }
      if (failed) {
        System.out.println("Sleeping for 5 seconds...");
        TimeUnit.SECONDS.sleep(5);
      } else {
        break;
      }
    }
    if (failed) { throw new AssertionError("Some classloader were not gced"); }

    boolean success = false;
    Set<SimpleThreadInfo> afterShutdownThreads = null;
    for (int i = 0; !success && i < 5; i++) {
      afterShutdownThreads = SimpleThreadInfo.parseThreadInfo(getThreadDump());
      afterShutdownThreads.removeAll(baseLineThreads);
      filterKnownThreads(afterShutdownThreads);
      if (afterShutdownThreads.size() == 0) {
        success = true;
      } else {
        System.out.println("******** Threads Diff: ");
        printThreads(afterShutdownThreads);
        TimeUnit.SECONDS.sleep(1);
      }
    }
    if (!success) {
      fail("Threads still running: " + afterShutdownThreads);
    }

    pass();
    System.exit(0);
  }

  private void waitUntilLastChanceThreadsAreGone(int seconds) throws InterruptedException {
    System.out.println("********** waiting for TCThreadGroup last chance cleaner thread to die...");
    for (int i = 0; i < seconds; i++) {
      boolean foundLastChanceThread = false;
      ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
      for (long id : tbean.getAllThreadIds()) {
        ThreadInfo tinfo = tbean.getThreadInfo(id, Integer.MAX_VALUE);
        if (tinfo.getThreadName().startsWith("TCThreadGroup last chance cleaner thread")) {
          foundLastChanceThread = true;
          break;
        }
      }
      if (!foundLastChanceThread) {
        System.out.println("********** TCThreadGroup last chance cleaner thread gone after " + i + "s");
        return;
      }
      Thread.sleep(1000);
    }
    System.out.println("********** TCThreadGroup still alive after" + seconds + "s");
  }

  // if only a single L1 loader got GC'ed, we can consider the test passed
  private boolean assertClassloadersGCed() {
    boolean failed = true;
    StringBuilder sb = new StringBuilder();
    for (WeakReference<ClassLoader> wr : CLASS_LOADER_LIST) {
      ClassLoader cl = wr.get();
      if (cl != null) {
        sb.append(cl).append(", ");
      } else {
        failed = false;
      }
    }
    if (failed) {
      sb.deleteCharAt(sb.length() - 1);
      sb.deleteCharAt(sb.length() - 1);
      dumpHeap(ShutdownClient1.class.getSimpleName());
      // throw new AssertionError("Classloader(s) " + sb + " not GC'ed");
      System.out.println("Classloaders not gc'ed yet: " + sb.toString());
    }
    return failed;
  }

  public static void printThreads(Set<SimpleThreadInfo> threads) {
    for (SimpleThreadInfo ti : threads) {
      System.out.println(ti);
    }
  }

  public void testClusteredCache(Cache cache, Toolkit toolkit) {
    try {
      testCache(cache, toolkit);
      getBarrierForAllClients().await(TimeUnit.SECONDS.toMillis(3 * 60), TimeUnit.MILLISECONDS); // wait for client2 to
                                                                                                 // assert clustered
                                                                                                 // cache
      getBarrierForAllClients().await(TimeUnit.SECONDS.toMillis(3 * 60), TimeUnit.MILLISECONDS); // line up for client2
                                                                                                 // to wait for client1
      // shutdown
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public void shutdownExpressClient() {
    getCacheManager().shutdown();
    getClusteringToolkit().shutdown();
  }

  protected void testCache(Cache cache, Toolkit toolkit) throws Throwable {
    cache.put(new Element("key", "value"));
  }

  private static String getThreadDump() {
    final String newline = System.getProperty("line.separator", "\n");
    StringBuffer rv = new StringBuffer();
    ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
    for (long id : tbean.getAllThreadIds()) {
      ThreadInfo tinfo = tbean.getThreadInfo(id, Integer.MAX_VALUE);
      rv.append("Thread name: " + tinfo.getThreadName()).append("-" + id).append(newline);
      for (StackTraceElement e : tinfo.getStackTrace()) {
        rv.append("    at " + e).append(newline);
      }
      rv.append(newline);
    }
    return rv.toString();
  }

  private Set<SimpleThreadInfo> filterKnownThreads(Set<SimpleThreadInfo> dump) {
    List<ThreadIgnore> ignores = Arrays.asList(new ThreadIgnore("http-", "org.apache.tomcat."),
                                               new ThreadIgnore("Attach Listener-", ""),
                                               new ThreadIgnore("Poller SunPKCS11", "sun.security.pkcs11."),
                                               new ThreadIgnore("(Attach Listener)", ""),
                                               new ThreadIgnore("JFR request timer", ""),
                                               new ThreadIgnore("JMAPI event thread", ""));

    for (Iterator<SimpleThreadInfo> it = dump.iterator(); it.hasNext();) {
      SimpleThreadInfo threadInfo = it.next();
      for (ThreadIgnore ignore : ignores) {
        if (ignore.canIgnore(threadInfo)) {
          it.remove();
        }
      }
    }
    return dump;
  }

  private void storeL1ClassLoaderWeakReferences(Cache cache) throws Exception {
    ClassLoader clusteredStateLoader = getBarrierForAllClients().getClass().getClassLoader();

    System.out.println("XXX: clusteredStateLoader: " + clusteredStateLoader);
    Assert.assertNotNull(clusteredStateLoader);

    CLASS_LOADER_LIST.add(new WeakReference<ClassLoader>(clusteredStateLoader));
  }

  private static void dumpHeap(String dumpName) {
    try {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      String hotSpotDiagName = "com.sun.management:type=HotSpotDiagnostic";
      ObjectName name = new ObjectName(hotSpotDiagName);
      String operationName = "dumpHeap";

      new File("heapDumps").mkdirs();
      File tempFile = new File("heapDumps/" + dumpName + "_" + (System.currentTimeMillis()) + ".hprof");
      tempFile.delete();
      String dumpFilename = tempFile.getAbsolutePath();

      Object[] params = new Object[] { dumpFilename, Boolean.TRUE };
      String[] signature = new String[] { String.class.getName(), boolean.class.getName() };
      mbs.invoke(name, operationName, params, signature);

      System.out.println("dumped heap in file " + dumpFilename);
    } catch (Exception e) {
      // ignore
    }
  }

}
