package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;

import org.junit.Assert;
import org.terracotta.api.ClusteringToolkit;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
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
  protected void runTest(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    Set<SimpleThreadInfo> baseLineThreads = SimpleThreadInfo.parseThreadInfo(getThreadDump());

    testClusteredCache(cache, toolkit);

    for (int i = 0; i < 10; i++) {
      System.out.println("***** Iteration " + (i + 1) + " *****");
      if (i > 0) {
        setupCache();
      }
      storeL1ClassLoaderWeakReferences(getCacheManager().getCache("test"));

      shutdownExpressClient();
      clearTerracottaClient();
      System.runFinalization();

      Thread.sleep(TimeUnit.SECONDS.toMillis(30));
    }

    waitUntilLastChanceThreadsAreGone(6 * 60);
    new PermStress().stress(10000);
    assertClassloadersGCed();

    Set<SimpleThreadInfo> afterShutdownThreads = SimpleThreadInfo.parseThreadInfo(getThreadDump());
    afterShutdownThreads.removeAll(baseLineThreads);
    System.out.println("******** Threads Diff: ");
    printThreads(afterShutdownThreads);
    assertThreadShutdown(afterShutdownThreads);

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
  private void assertClassloadersGCed() {
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
      throw new AssertionError("Classloader(s) " + sb + " not GC'ed");
    }
  }

  public static void printThreads(Set<SimpleThreadInfo> threads) {
    for (SimpleThreadInfo ti : threads) {
      System.out.println(ti);
    }
  }

  public void testClusteredCache(Cache cache, ClusteringToolkit toolkit) {
    try {
      testCache(cache, toolkit);
      getBarrierForAllClients().await(TimeUnit.SECONDS.toMillis(3 * 60)); // wait for client2 to assert clustered cache
      getBarrierForAllClients().await(TimeUnit.SECONDS.toMillis(3 * 60)); // line up for client2 to wait for client1
                                                                          // shutdown
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public void shutdownExpressClient() {
    getCacheManager().shutdown();
    getTerracottaClient().shutdown();
  }

  protected void testCache(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    cache.put(new Element("key", "value"));

    Assert.assertEquals(StorageStrategy.DCV2, cache.getCacheConfiguration().getTerracottaConfiguration()
        .getStorageStrategy());
    System.out.println("Asserted default storageStrategy");
  }

  private void assertThreadShutdown(Set<SimpleThreadInfo> dump) throws Exception {
    filterKnownThreads(dump);
    if (dump.size() > 0) { throw new AssertionError("Threads still running: " + dump); }
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
    Method getStoreMethod = Cache.class.getDeclaredMethod("getStore", (Class[]) null);
    getStoreMethod.setAccessible(true);

    Object clusteredStore = getStoreMethod.invoke(cache, (Object[]) null);
    ClassLoader clusteredStateLoader = clusteredStore.getClass().getClassLoader();

    Class managerUtilClass = clusteredStateLoader.loadClass("com.tc.object.bytecode.ManagerUtil");
    ClassLoader bootjarLoader = managerUtilClass.getClassLoader();
    Method getManagerMethod = managerUtilClass.getDeclaredMethod("getManager", (Class[]) null);
    ClassLoader l1Loader = getManagerMethod.invoke(null, (Object[]) null).getClass().getClassLoader();

    System.out.println("XXX: clusteredStateLoader: " + clusteredStateLoader);
    System.out.println("XXX: bootjarLoader: " + bootjarLoader);
    System.out.println("XXX: l1Loader: " + l1Loader);
    Assert.assertNotNull(clusteredStateLoader);
    Assert.assertNotNull(bootjarLoader);
    Assert.assertNotNull(l1Loader);
    Assert.assertTrue(clusteredStateLoader != bootjarLoader);
    Assert.assertTrue(clusteredStateLoader != l1Loader);
    Assert.assertTrue(bootjarLoader != l1Loader);

    CLASS_LOADER_LIST.add(new WeakReference<ClassLoader>(clusteredStateLoader));
    CLASS_LOADER_LIST.add(new WeakReference<ClassLoader>(bootjarLoader));
    CLASS_LOADER_LIST.add(new WeakReference<ClassLoader>(l1Loader));
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
