package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;

import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.junit.Assert;
import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.container.hibernate.domain.Event;
import org.terracotta.ehcache.tests.container.hibernate.domain.EventManager;
import org.terracotta.ehcache.tests.container.hibernate.domain.Person;
import org.terracotta.ehcache.tests.container.hibernate.domain.PhoneNumber;
import org.terracotta.ehcache.tests.container.hibernate.nontransactional.HibernateUtil;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class HibernateShutdownClient1 extends ClientBase {
  private static final List<WeakReference<ClassLoader>> CLASS_LOADER_LIST = new ArrayList<WeakReference<ClassLoader>>();

  public HibernateShutdownClient1(String[] args) {
    super("test", args);
  }

  @Override
  public void doTest() throws Throwable {
    Set<SimpleThreadInfo> baseLineThreads = SimpleThreadInfo.parseThreadInfo(getThreadDump());

    testClusteredCache();

    for (int i = 0; i < 5; i++) {
      System.out.println("***** Iteration " + (i + 1) + " *****");
      if (i > 0) {
        HibernateUtil.configure("/hibernate-config/shutdowntest/hibernate.cfg.xml");
        HibernateUtil.closeSessionFactory();
      }

      storeL1ClassLoaderWeakReferences();

      shutdownExpressClient();
      clearTerracottaClient();
      System.runFinalization();

      Thread.sleep(TimeUnit.SECONDS.toMillis(30));
    }

    waitUntilLastChanceThreadsAreGone();
    new PermStress().stress(10000);
    assertClassloadersGCed();

    Set<SimpleThreadInfo> afterShutdownThreads = SimpleThreadInfo.parseThreadInfo(getThreadDump());
    afterShutdownThreads.removeAll(baseLineThreads);
    System.out.println("******** Threads Diff: ");
    printThreads(afterShutdownThreads);
    assertThreadShutdown(afterShutdownThreads);
  }

  private void waitUntilLastChanceThreadsAreGone() throws InterruptedException {
    for (int i = 0; i < (5 * 60); i++) {
      boolean foundLastChanceThread = false;
      ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
      for (long id : tbean.getAllThreadIds()) {
        ThreadInfo tinfo = tbean.getThreadInfo(id, Integer.MAX_VALUE);
        if (tinfo.getThreadName().startsWith("TCThreadGroup last chance cleaner thread")) {
          foundLastChanceThread = true;
          break;
        }
      }
      if (!foundLastChanceThread) { return; }
      Thread.sleep(1000);
    }
  }

  @Override
  protected void setupCacheManager() {
    // Avoid setting up a cache manager
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
      dumpHeap(HibernateShutdownClient1.class.getSimpleName());
      throw new AssertionError("Classloader(s) " + sb + " not GC'ed");
    }
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

  public static void printThreads(Set<SimpleThreadInfo> threads) {
    for (SimpleThreadInfo ti : threads) {
      System.out.println(ti);
    }
  }

  public void testClusteredCache() {
    try {
      runTest(null, getClusteringToolkit());
      getBarrierForAllClients().await(TimeUnit.SECONDS.toMillis(3 * 60)); // wait for client2 to assert clustered cache
      getBarrierForAllClients().await(TimeUnit.SECONDS.toMillis(3 * 60)); // line up for client2 to wait for client1
                                                                          // shutdown
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public void shutdownExpressClient() {
    getTerracottaClient().shutdown();
  }

  private void assertThreadShutdown(Set<SimpleThreadInfo> dump) throws Exception {
    filterKnownThreads(dump);
    if (dump.size() > 0) { throw new AssertionError("Threads still running: " + dump); }
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    HibernateUtil.configure("/hibernate-config/shutdowntest/hibernate.cfg.xml");
    HibernateUtil.dropAndCreateDatabaseSchema();

    EventManager mgr = new EventManager(HibernateUtil.getSessionFactory());
    Statistics stats = HibernateUtil.getSessionFactory().getStatistics();
    stats.setStatisticsEnabled(true);

    // create 3 persons Steve, Orion, Tim
    Person stevePerson = new Person();
    stevePerson.setFirstname("Steve");
    stevePerson.setLastname("Harris");
    Long steveId = mgr.createAndStorePerson(stevePerson);
    mgr.addEmailToPerson(steveId, "steve@tc.com");
    mgr.addEmailToPerson(steveId, "sharrif@tc.com");
    mgr.addTalismanToPerson(steveId, "rabbit foot");
    mgr.addTalismanToPerson(steveId, "john de conqueroo");

    PhoneNumber p1 = new PhoneNumber();
    p1.setNumberType("Office");
    p1.setPhone(111111);
    mgr.addPhoneNumberToPerson(steveId, p1);

    PhoneNumber p2 = new PhoneNumber();
    p2.setNumberType("Home");
    p2.setPhone(222222);
    mgr.addPhoneNumberToPerson(steveId, p2);

    Person orionPerson = new Person();
    orionPerson.setFirstname("Orion");
    orionPerson.setLastname("Letizi");
    Long orionId = mgr.createAndStorePerson(orionPerson);
    mgr.addEmailToPerson(orionId, "orion@tc.com");
    mgr.addTalismanToPerson(orionId, "voodoo doll");

    Long timId = mgr.createAndStorePerson("Tim", "Teck");
    mgr.addEmailToPerson(timId, "teck@tc.com");
    mgr.addTalismanToPerson(timId, "magic decoder ring");

    Long engMeetingId = mgr.createAndStoreEvent("Eng Meeting", stevePerson, new Date());
    mgr.addPersonToEvent(steveId, engMeetingId);
    mgr.addPersonToEvent(orionId, engMeetingId);
    mgr.addPersonToEvent(timId, engMeetingId);

    Long docMeetingId = mgr.createAndStoreEvent("Doc Meeting", orionPerson, new Date());
    mgr.addPersonToEvent(steveId, docMeetingId);
    mgr.addPersonToEvent(orionId, docMeetingId);

    for (Event event : (List<Event>) mgr.listEvents()) {
      mgr.listEmailsOfEvent(event.getId());
    }

    HibernateUtil.closeSessionFactory();

    System.err.println("Second Level Cache Regions");
    for (String region : stats.getSecondLevelCacheRegionNames()) {
      System.err.println("Region : " + region);
      SecondLevelCacheStatistics l2Stats = stats.getSecondLevelCacheStatistics(region);
      System.err.println("\tCache Miss Count " + l2Stats.getMissCount());
      System.err.println("\tCache Hit Count " + l2Stats.getHitCount());
      System.err.println("\tCache Put Count " + l2Stats.getPutCount());
    }

    QueryStatistics queryStats = stats.getQueryStatistics("from Event");
    Assert.assertEquals("Cache Miss Count", 1L, queryStats.getCacheMissCount());
    Assert.assertEquals("Cache Hit Count", 0L, queryStats.getCacheHitCount());
    Assert.assertEquals("Cache Put Count", 1L, queryStats.getCachePutCount());
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
                                               new ThreadIgnore("(Attach Listener)-", ""),
                                               new ThreadIgnore("JFR request timer-", ""),
                                               new ThreadIgnore("JMAPI event thread-", ""));

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

  public void storeL1ClassLoaderWeakReferences() throws Exception {
    ClassLoader clusteredStateLoader = getBarrierForAllClients().getClass().getClassLoader();

    System.out.println("XXX: clusteredStateLoader: " + clusteredStateLoader);
    Assert.assertNotNull(clusteredStateLoader);

    CLASS_LOADER_LIST.add(new WeakReference<ClassLoader>(clusteredStateLoader));
  }
}
