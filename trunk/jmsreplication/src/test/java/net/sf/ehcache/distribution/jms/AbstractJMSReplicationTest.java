package net.sf.ehcache.distribution.jms;

import java.io.IOException;
import static net.sf.ehcache.distribution.jms.TestUtil.forceVMGrowth;
import static net.sf.ehcache.distribution.jms.RetryAssert.assertBy;
import static net.sf.ehcache.distribution.jms.RetryAssert.elementAt;
import static net.sf.ehcache.distribution.jms.RetryAssert.sizeOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.net.URL;
import java.rmi.server.RMISocketFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.ConfigurationFactory;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;

public abstract class AbstractJMSReplicationTest {

    protected static final String SAMPLE_CACHE_ASYNC = "sampleCacheAsync";
    protected static final String SAMPLE_CACHE_SYNC = "sampleCacheSync";
    protected static final String SAMPLE_CACHE_NOREP = "sampleCacheNorep";
    protected static final String SAMPLE_CACHE_JMS_REPLICATION_BOOTSTRAP = "sampleJMSReplicateRMIBootstrap";
  
    private static final Logger LOG = LoggerFactory.getLogger(AbstractJMSReplicationTest.class);
    private static final Collection<String> REPLICATED_CACHES = Arrays.asList(SAMPLE_CACHE_ASYNC, SAMPLE_CACHE_SYNC, SAMPLE_CACHE_JMS_REPLICATION_BOOTSTRAP);
  
    protected abstract URL getConfiguration();
  
    @BeforeClass
    public static void installRMISocketFactory() {
        RMISocketFactory current = RMISocketFactory.getSocketFactory();
        if (current == null) {
            current = RMISocketFactory.getDefaultSocketFactory();
        }
        assertNotNull(current);
        try {
            RMISocketFactory.setSocketFactory(new SocketReusingRMISocketFactory(current));
            LOG.info("Installed the SO_REUSEADDR setting socket factory");
        } catch (IOException e) {
            LOG.warn("Couldn't register the SO_REUSEADDR setting socket factory", e);
        }
    }

    public List<CacheManager> createCluster(String rootManagerName, int members) throws InterruptedException {
        LOG.info("Creating Cluster");
        List<CacheManager> managers = new ArrayList<CacheManager>(members);
        for (int i = 0; i < members; i++) {
            managers.add(new CacheManager(ConfigurationFactory.parseConfiguration(getConfiguration()).name(rootManagerName + i)));
        }
        LOG.info("Created Managers");
        try {
            waitForClusterMembership(120, TimeUnit.SECONDS, managers);
            LOG.info("Cluster Membership Complete");
            emptyCaches(120, TimeUnit.SECONDS, managers);
            LOG.info("Caches Emptied");
            return managers;
        } catch (RuntimeException e) {
            destroyCluster(managers);
            throw e;
        } catch (Error e) {
            destroyCluster(managers);
            throw e;
        }
    }
  
    public void destroyCluster(List<CacheManager> cluster) throws InterruptedException {
        for (CacheManager manager : cluster) {
            manager.shutdown();
        }
    }

    private void waitForClusterMembership(int time, TimeUnit unit, List<CacheManager> members) throws InterruptedException {
        Thread.sleep(200);
    }

    private void emptyCaches(final int time, final TimeUnit unit, final List<CacheManager> members) {
        List<Callable<Void>> cacheEmptyTasks = new ArrayList<Callable<Void>>();
        for (String cache : getAllReplicatedCacheNames(members.get(0))) {
            final String cacheName = cache;
            cacheEmptyTasks.add(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    for (CacheManager manager : members) {
                        manager.getCache(cacheName).put(new Element("setup", "setup"), true);
                    }

                    members.get(0).getCache(cacheName).remove("setup");
                    for (CacheManager manager : members.subList(1, members.size())) {
                        RetryAssert.assertBy(time, unit, RetryAssert.sizeOf(manager.getCache(cacheName)), is(0));
                    }
                    return null;
                }
            });
        }

        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            for (Future<Void> result : executor.invokeAll(cacheEmptyTasks)) {
                result.get();
            }
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        } catch (ExecutionException e) {
            throw new AssertionError(e);
        } finally {
            executor.shutdown();
        }
    }

    private static Collection<String> getAllReplicatedCacheNames(CacheManager manager) {
      return REPLICATED_CACHES;
    }

    @Test
    public void testBasicReplicationAsynchronous() throws Exception {
        basicReplicationTest(SAMPLE_CACHE_ASYNC);
    }
  
    @Test
    public void testBasicReplicationSynchronous() throws Exception {
        basicReplicationTest(SAMPLE_CACHE_SYNC);
    }
  
    @Test
    public void testStartupAndShutdown() throws InterruptedException {
        destroyCluster(createCluster("testStartupAndShutdown", 4));
    }
  
    @Test
    public void testCASOperationsNotSupported() throws Exception {
        final CacheManager manager = new CacheManager(ConfigurationFactory.parseConfiguration(getConfiguration()).name("testCASOperationsNotSupported"));
        try {
            final Ehcache cache = manager.getEhcache(SAMPLE_CACHE_ASYNC);

            try {
                cache.putIfAbsent(new Element("foo", "poo"));
                throw new AssertionError("CAS operation should have failed.");
            } catch (CacheException ce) {
                assertTrue(ce.getMessage().contains("CAS"));
            }

            try {
                cache.removeElement(new Element("foo", "poo"));
                throw new AssertionError("CAS operation should have failed.");
            } catch (CacheException ce) {
                assertTrue(ce.getMessage().contains("CAS"));
            }

            try {
                cache.replace(new Element("foo", "poo"));
                throw new AssertionError("CAS operation should have failed.");
            } catch (CacheException ce) {
                assertTrue(ce.getMessage().contains("CAS"));
            }

            try {
                cache.replace(new Element("foo", "poo"), new Element("foo", "poo2"));
                throw new AssertionError("CAS operation should have failed.");
            } catch (CacheException ce) {
                assertTrue(ce.getMessage().contains("CAS"));
            }
        } finally {
            manager.shutdown();
        }
    }
    
    public void basicReplicationTest(String cacheName) throws Exception {
        final int NBR_ELEMENTS = 100;
  
        List<CacheManager> cluster = createCluster("basicReplicationTest," + cacheName, 4);
        try {
            //put
            for (int i = 0; i < NBR_ELEMENTS; i++) {
                cluster.get(0).getCache(cacheName).put(new Element(i, "testdat"));
            }

            for (CacheManager manager : cluster) {
                assertBy(3, TimeUnit.SECONDS, sizeOf(manager.getCache(cacheName)), is(NBR_ELEMENTS));
            }

            //update via copy
            for (int i = 0; i < NBR_ELEMENTS; i++) {
                cluster.get(0).getCache(cacheName).put(new Element(i, "testdat"));
            }
            Thread.sleep(1000);

            for (CacheManager manager : cluster) {
                assertThat(manager.getName() + "." + cacheName, manager.getCache(cacheName).getKeys().size(), is(NBR_ELEMENTS));
            }

            //remove
            cluster.get(0).getCache(cacheName).remove(0);

            for (CacheManager manager : cluster) {
                assertBy(3, TimeUnit.SECONDS, sizeOf(manager.getCache(cacheName)), is(NBR_ELEMENTS - 1));
            }

            //removeall
            cluster.get(0).getCache(cacheName).removeAll();

            for (CacheManager manager : cluster) {
                assertBy(3, TimeUnit.SECONDS, sizeOf(manager.getCache(cacheName)), is(0));
            }
        } finally {
            destroyCluster(cluster);
        }
    }
  
  
  //  @Test
  //  public void testShutdownManager() throws Exception {
  //      cacheName = SAMPLE_CACHE_ASYNC;
  //      manager1.getCache(cacheName).removeAll();
  //      Thread.currentThread().sleep(1000);
  //
  //      CacheManagerPeerProvider provider = manager1.getCachePeerProvider();
  //      JGroupManager jg = (JGroupManager) provider;
  //      assertEquals(Status.STATUS_ALIVE, jg.getStatus());
  //      manager1.shutdown();
  //      assertEquals(Status.STATUS_SHUTDOWN, jg.getStatus());
  //      //Lets see if the other still replicate
  //      manager2.getCache(cacheName).put(new Element(new Integer(1), new Date()));
  //      Thread.currentThread().sleep(2000);
  //
  //
  //      assertTrue(manager2.getCache(cacheName).getKeys().size() == manager3.getCache(cacheName).getKeys().size() &&
  //              manager2.getCache(cacheName).getKeys().size() == manager4.getCache(cacheName).getKeys().size() &&
  //              manager2.getCache(cacheName).getKeys().size() == 1);
  //
  //
  //  }
  
    @Test
    public void testContinuous() throws Exception {
        List<CacheManager> cluster = createCluster("testContinuous", 4);
        try {
            for (int i = 0; i < 10; i++) {
                assertThat(cluster.get(0).getStatus(), is(Status.STATUS_ALIVE));
                cluster.remove(0).shutdown();

                Thread.sleep(2000);

                cluster.add(new CacheManager(ConfigurationFactory.parseConfiguration(getConfiguration()).name("testContinuous" + (i+4))));

                Thread.sleep(5000);
                cluster.get(0).getCache(SAMPLE_CACHE_ASYNC).put(new Element(2, new Date()));
                cluster.get(1).getCache(SAMPLE_CACHE_ASYNC).put(new Element(3, new Date()));

                for (CacheManager manager : cluster) {
                    assertBy(2, TimeUnit.SECONDS, sizeOf(manager.getCache(SAMPLE_CACHE_ASYNC)), is(2));
                }
            }
        } finally {
            destroyCluster(cluster);
        }
    }
  
  
    /**
     * Occasionally failig because the publish session is closed when it tries to send. 
     * SEVERE: The Session is closed
  javax.jms.IllegalStateException: The Session is closed
    at org.apache.activemq.ActiveMQSession.checkClosed(ActiveMQSession.java:616)
    at org.apache.activemq.ActiveMQSession.configureMessage(ActiveMQSession.java:604)
    at org.apache.activemq.ActiveMQSession.createObjectMessage(ActiveMQSession.java:316)
    at org.apache.activemq.ActiveMQTopicSession.createObjectMessage(ActiveMQTopicSession.java:192)
    at net.sf.ehcache.distribution.jms.JMSCachePeer.send(JMSCachePeer.java:228)
    at net.sf.ehcache.distribution.jms.JMSCacheReplicator.flushReplicationQueue(JMSCacheReplicator.java:554)
    at net.sf.ehcache.distribution.jms.JMSCacheReplicator.dispose(JMSCacheReplicator.java:159)
    at net.sf.ehcache.event.RegisteredEventListeners.dispose(RegisteredEventListeners.java:275)
    at net.sf.ehcache.Cache.dispose(Cache.java:2088)
    at net.sf.ehcache.CacheManager.shutdown(CacheManager.java:1119)
    at net.sf.ehcache.distribution.jms.ActiveMQJMSReplicationTest.tearDown(ActiveMQJMSReplicationTest.java:91)
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
    at java.lang.reflect.Method.invoke(Method.java:597)
    at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:44)
    at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:15)
    at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:41)
    at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:37)
    at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:76)
    at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:50)
    at org.junit.runners.ParentRunner$3.run(ParentRunner.java:193)
    at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:52)
    at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:191)
    at org.junit.runners.ParentRunner.access$000(ParentRunner.java:42)
    at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:184)
    at org.junit.runners.ParentRunner.run(ParentRunner.java:236)
    at org.junit.runner.JUnitCore.run(JUnitCore.java:157)
    at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:94)
    at com.intellij.rt.execution.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:192)
    at com.intellij.rt.execution.junit.JUnitStarter.main(JUnitStarter.java:64)
     */
    @Ignore
    @Test
    public void testAddManager() throws Exception {
        List<CacheManager> cluster = createCluster("testAddManager", 4);
        try {
            assertThat(cluster.get(0).getStatus(), is(Status.STATUS_ALIVE));
            cluster.remove(0).shutdown();


            Thread.sleep(2000);

            cluster.add(new CacheManager(ConfigurationFactory.parseConfiguration(getConfiguration()).name("testAddManager4")));

            Thread.sleep(5000);
            cluster.get(0).getCache(SAMPLE_CACHE_ASYNC).put(new Element(2, new Date()));
            cluster.get(1).getCache(SAMPLE_CACHE_ASYNC).put(new Element(3, new Date()));

            Thread.sleep(2000);

            for (CacheManager manager : cluster) {
                assertBy(2, TimeUnit.SECONDS, sizeOf(manager.getCache(SAMPLE_CACHE_ASYNC)), is(2));
            }
        } finally {
            destroyCluster(cluster);
        }
    }
  
  
    @Test
    public void testNoReplication() throws Exception {
        List<CacheManager> cluster = createCluster("testNoReplication", 2);
        try {
            Ehcache cache1 = cluster.get(0).getCache(SAMPLE_CACHE_NOREP);
            Ehcache cache2 = cluster.get(1).getCache(SAMPLE_CACHE_NOREP);
            Element element = new Element(1, new Date());

            //put
            cache2.put(element);
            Thread.sleep(1000);
            assertThat(cache1.getKeys().size(), is(0));
            assertThat(cache2.getKeys().size(), is(1));

            //update
            cache2.put(element);
            Thread.sleep(1000);
            assertThat(cache1.getKeys().size(), is(0));
            assertThat(cache2.getKeys().size(), is(1));

            //remove
            cache1.put(element);
            cache1.remove(1);
            Thread.sleep(1000);
            assertThat(cache1.getKeys().size(), is(0));
            assertThat(cache2.getKeys().size(), is(1));

            //removeAll
            cache1.removeAll();
            Thread.sleep(1000);
            assertThat(cache1.getKeys().size(), is(0));
            assertThat(cache2.getKeys().size(), is(1));
        } finally {
            destroyCluster(cluster);
        }
    }
  
    /**
     * What happens when two cache instances replicate to each other and a change is initiated
     *
     * @throws InterruptedException -
     */
    @Test
    public void testVariousPuts() throws Exception {
        List<CacheManager> cluster = createCluster("testVariousPuts", 2);
        try {
            Ehcache cache1 = cluster.get(0).getCache(SAMPLE_CACHE_ASYNC);
            Ehcache cache2 = cluster.get(1).getCache(SAMPLE_CACHE_ASYNC);

            Serializable key = "1";
            Serializable value = new Date();
            Element element = new Element(key, value);

            //Put
            cache1.put(element);
            Thread.sleep(1000);

            //Should have been replicated to cache2.
            assertBy(1, TimeUnit.SECONDS, elementAt(cache2, key), is(element));

            //Remove
            cache1.remove(key);
            assertThat(cache1.get(key), nullValue());

            //Should have been replicated to cache2.
            assertBy(1, TimeUnit.SECONDS, elementAt(cache2, key), nullValue());

            //Put into 2
            Element element3 = new Element("3", "ddsfds");
            cache2.put(element3);
            assertBy(1, TimeUnit.SECONDS, elementAt(cache1, "3"), is(element3));

            cluster.get(0).clearAll();
        } finally {
            destroyCluster(cluster);
        }
    }
  
  
    /**
     * What happens when two cache instances replicate to each other and a change is initiated
     *
     * @throws InterruptedException -
     */
    @Test
    public void testPutAndRemove() throws Exception {
        List<CacheManager> cluster = createCluster("testPutAndRemove", 2);
        try {
            Ehcache cache1 = cluster.get(0).getCache(SAMPLE_CACHE_SYNC);
            Ehcache cache2 = cluster.get(1).getCache(SAMPLE_CACHE_SYNC);

            Serializable key = "1";
            Serializable value = new Date();
            Element element = new Element(key, value);

            //Put
            cache1.put(element);
            long updateTime = element.getLastUpdateTime();
            //Should have been replicated to cache2.
            assertBy(1, TimeUnit.SECONDS, elementAt(cache2, key), is(element));
            //make sure we are not getting our own circular update back
            assertThat(cache1.get(key).getLastUpdateTime(), is(updateTime));

            //Remove
            cache1.remove(key);
            assertThat(cache1.get(key), nullValue());

            //Should have been replicated to cache2.
            assertBy(1, TimeUnit.SECONDS, elementAt(cache2, key), nullValue());
        } finally {
            destroyCluster(cluster);
        }
    }
  
  
    /**
     * Same as testPutandRemove but this one does:
     */
    @Test
    public void testPutAndRemoveStability() throws InterruptedException {
        List<CacheManager> cluster = createCluster("testPutAndRemoveStability", 2);
        try {
            for (int i = 0; i < 120; i++) {
                Ehcache cache1 = cluster.get(0).getCache(SAMPLE_CACHE_SYNC);
                Ehcache cache2 = cluster.get(1).getCache(SAMPLE_CACHE_SYNC);

                Serializable key = "1";
                Serializable value = new Date();
                Element element = new Element(key, value);

                //Put
                cache1.put(element);
                long updateTime = element.getLastUpdateTime();
                //Should have been replicated to cache2.
                assertBy(1, TimeUnit.SECONDS, elementAt(cache2, key), is(element));
                //make sure we are not getting our own circular update back
                assertThat(cache1.get(key).getLastUpdateTime(), is(updateTime));

                //Remove
                cache1.remove(key);
                assertThat(cache1.get(key), nullValue());

                //Should have been replicated to cache2.
                Thread.sleep(100);
                assertBy(1, TimeUnit.SECONDS, elementAt(cache2, key), nullValue());
            }
        } finally {
            destroyCluster(cluster);
        }
    }
  
  
    /**
     * This is a manual test.
     * Start the test running and observe the output. You should see no exceptions.
     * Then kill the message queue.
     * <p/>
     * You will see errors like these if using Open MQ.
     * <p/>
     * Oct 11, 2008 10:11:10 PM com.sun.messaging.jmq.jmsclient.ExceptionHandler logCaughtException
     * WARNING: [I500]: Caught JVM Exception: java.io.EOFException
     * Oct 11, 2008 10:11:10 PM com.sun.messaging.jmq.jmsclient.ExceptionHandler logCaughtException
     * WARNING: [I500]: Caught JVM Exception: java.io.EOFException
     * Oct 11, 2008 10:11:10 PM com.sun.messaging.jmq.jmsclient.ConnectionRecover logRecoverState
     * INFO: [I107]: Connection recover state: RECOVER_INACTIVE, broker: localhost:7676(49990)
     * Oct 11, 2008 10:11:10 PM com.sun.messaging.jmq.jmsclient.ConnectionRecover logRecoverState
     * INFO: [I107]: Connection recover state: RECOVER_INACTIVE, broker: localhost:7676(49990)
     * Oct 11, 2008 10:11:10 PM com.sun.messaging.jmq.jmsclient.ExceptionHandler logCaughtException
     * WARNING: [I500]: Caught JVM Exception: java.io.EOFException
     * Oct 11, 2008 10:11:10 PM com.sun.messaging.jmq.jmsclient.ConnectionRecover logRecoverState
     * INFO: [I107]: Connection recover state: RECOVER_INACTIVE, broker: localhost:7676(49990)
     * Oct 11, 2008 10:11:10 PM com.sun.messaging.jmq.jmsclient.ConnectionRecover logRecoverState
     * INFO: [I107]: Connection recover state: RECOVER_INACTIVE, broker: localhost:7676(49990)
     * Oct 11, 2008 10:11:10 PM com.sun.messaging.jmq.jmsclient.ConnectionRecover logRecoverState
     * INFO: [I107]: Connection recover state: RECOVER_INACTIVE, broker: localhost:7676(49990)
     * Oct 11, 2008 10:11:10 PM com.sun.messaging.jmq.jmsclient.ConnectionRecover logRecoverState
     * INFO: [I107]: Connection recover state: RECOVER_INACTIVE, broker: localhost:7676(49990)
     * Oct 11, 2008 10:11:13 PM com.sun.messaging.jmq.jmsclient.ExceptionHandler throwConnectionException
     * WARNING: [C4003]: Error occurred on connection creation [localhost:7676]. - cause: java.net.ConnectException: Connection refused
     * <p/>
     * Then restart the message queue.
     * <p/>
     * You will see recover messages such as:
     * <p/>
     * INFO: [I107]: Connection recover state: RECOVER_INACTIVE, broker: localhost:7676(50206)
     * Oct 11, 2008 10:13:46 PM com.sun.messaging.jmq.jmsclient.ConnectionRecover logRecoverState
     * INFO: [I107]: Connection recover state: RECOVER_SUCCEEDED, broker: localhost:7676(50206)
     * Oct 11, 2008 10:13:46 PM com.sun.messaging.jmq.jmsclient.ConnectionRecover logRecoverState
     * INFO: [I107]: Connection recover state: RECOVER_INACTIVE, broker: localhost:7676(50206)
     * Oct 11, 2008 10:13:46 PM com.sun.messaging.jmq.jmsclient.ConnectionRecover logRecoverState
     * INFO: [I107]: Connection recover state: RECOVER_INACTIVE, broker: localhost:7676(50206)
     * Oct 11, 2008 10:13:46 PM com.sun.messaging.jmq.jmsclient.ConsumerReader run
     * WARNING: [C4001]: Write packet failed., packet type = ACKNOWLEDGE(24)
     * com.sun.messaging.jms.JMSException: [C4001]: Write packet failed., packet type = ACKNOWLEDGE(24)
     * at com.sun.messaging.jmq.jmsclient.ConnectionImpl.checkPacketType(ConnectionImpl.java:654)
     * at com.sun.messaging.jmq.jmsclient.ConnectionImpl.checkReconnecting(ConnectionImpl.java:641)
     * at com.sun.messaging.jmq.jmsclient.ProtocolHandler.checkConnectionState(ProtocolHandler.java:766)
     * at com.sun.messaging.jmq.jmsclient.ProtocolHandler.writePacketNoAck(ProtocolHandler.java:360)
     * at com.sun.messaging.jmq.jmsclient.ProtocolHandler.acknowledge(ProtocolHandler.java:2608)
     * at com.sun.messaging.jmq.jmsclient.SessionImpl.doAcknowledge(SessionImpl.java:1382)
     * at com.sun.messaging.jmq.jmsclient.SessionImpl.dupsOkCommitAcknowledge(SessionImpl.java:1427)
     * at com.sun.messaging.jmq.jmsclient.SessionImpl.syncedDupsOkCommitAcknowledge(SessionImpl.java:1450)
     * at com.sun.messaging.jmq.jmsclient.SessionReader.deliver(SessionReader.java:141)
     * at com.sun.messaging.jmq.jmsclient.ConsumerReader.run(ConsumerReader.java:190)
     * at java.lang.Thread.run(Thread.java:613)
     * Oct 11, 2008 10:13:46 PM com.sun.messaging.jmq.jmsclient.ConnectionRecover logRecoverState
     * INFO: [I107]: Connection recover state: RECOVER_INACTIVE, broker: localhost:7676(50206)
     * Oct 11, 2008 10:13:46 PM com.sun.messaging.jmq.jmsclient.ConsumerReader run
     * WARNING: [C4001]: Write packet failed., packet type = ACKNOWLEDGE(24)
     * com.sun.messaging.jms.JMSException: [C4001]: Write packet failed., packet type = ACKNOWLEDGE(24)
     * at com.sun.messaging.jmq.jmsclient.ConnectionImpl.checkPacketType(ConnectionImpl.java:654)
     * at com.sun.messaging.jmq.jmsclient.ConnectionImpl.checkReconnecting(ConnectionImpl.java:641)
     * at com.sun.messaging.jmq.jmsclient.ProtocolHandler.checkConnectionState(ProtocolHandler.java:766)
     * at com.sun.messaging.jmq.jmsclient.ProtocolHandler.writePacketNoAck(ProtocolHandler.java:360)
     * at com.sun.messaging.jmq.jmsclient.ProtocolHandler.acknowledge(ProtocolHandler.java:2608)
     * at com.sun.messaging.jmq.jmsclient.SessionImpl.doAcknowledge(SessionImpl.java:1382)
     * at com.sun.messaging.jmq.jmsclient.SessionImpl.dupsOkCommitAcknowledge(SessionImpl.java:1427)
     * at com.sun.messaging.jmq.jmsclient.SessionImpl.syncedDupsOkCommitAcknowledge(SessionImpl.java:1450)
     * at com.sun.messaging.jmq.jmsclient.SessionReader.deliver(SessionReader.java:141)
     * at com.sun.messaging.jmq.jmsclient.ConsumerReader.run(ConsumerReader.java:190)
     * at java.lang.Thread.run(Thread.java:613)
     * Oct 11, 2008 10:13:46 PM com.sun.messaging.jmq.jmsclient.ConnectionRecover logRecoverState
     * <p/>
     * Normal processing will then resume. i.e. the ehcache cluster reforms once the message queue is back.
     * <p/>
     * To enable this behaviour the following must be set on the connection factory configuration.
     * <p/>
     * Open MQ
     * imqReconnect='true' - without this reconnect will not happen
     * imqPingInterval='5' - Consumers will not reconnect until they notice the connection is down. The ping interval
     * does this. The default is 30. Set it lower if you want the ehcache cluster to reform more quickly.
     * Finally, unlimited retry attempts are recommended. This is the default.
     */
    //@Test
    public void testPutAndRemoveMessageQueueFailure() throws InterruptedException {
        List<CacheManager> cluster = createCluster("testPutAndRemoveMessageQueueFailure", 2);
        try {
            for (int i = 0; i < 1000; i++) {
                try {
                    Ehcache cache1 = cluster.get(0).getCache(SAMPLE_CACHE_SYNC);
                    Ehcache cache2 = cluster.get(1).getCache(SAMPLE_CACHE_SYNC);

                    Serializable key = "1";
                    Serializable value = new Date();
                    Element element = new Element(key, value);

                    //Put
                    cache1.put(element);
                    long updateTime = element.getLastUpdateTime();
                    Thread.sleep(100);
                    //Should have been replicated to cache2.
                    assertBy(1, TimeUnit.SECONDS, elementAt(cache2, key), is(element));
                    //make sure we are not getting our own circular update back
                    assertThat(cache1.get(key).getLastUpdateTime(), is(updateTime));

                    //Remove
                    cache1.remove(key);
                    assertThat(cache1.get(key), nullValue());

                    //Should have been replicated to cache2.
                    assertBy(1, TimeUnit.SECONDS, elementAt(cache2, key), nullValue());
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        } finally {
            destroyCluster(cluster);
        }
    }
  
    @Test
    public void testSimultaneousPutRemove() throws InterruptedException {
        List<CacheManager> cluster = createCluster("testSimultaneousPutRemove", 2);
        try {
            Ehcache cache1 = cluster.get(0).getCache(SAMPLE_CACHE_SYNC);
            Ehcache cache2 = cluster.get(1).getCache(SAMPLE_CACHE_SYNC);

            Serializable key = "1";
            Serializable value = new Date();
            Element element = new Element(key, value);

            //Put
            cache1.put(element);
            Thread.sleep(1000);
            cache2.remove(element.getKey());
            Thread.sleep(1000);


            assertThat(cache1.get(element.getKey()), nullValue());
            cluster.get(0).clearAll();
            Thread.sleep(1000);

            cache2.put(element);
            cache2.remove(element.getKey());
            Thread.sleep(1000);
            cache1.put(element);
            Thread.sleep(1000);
            assertThat(cache2.get(element.getKey()), notNullValue());

            cluster.get(0).clearAll();
            Thread.sleep(1000);
        } finally {
            destroyCluster(cluster);
        }
    }
  
  
    /**
     * Tests the JMSCacheLoader.
     * <p/>
     * We put an item in cache1, which does not replicate.
     * <p/>
     * We then do a get on cache2, which has a JMSCacheLoader which should ask the cluster for the answer.
     * If a cache does not have an element it should leave the message on the queue for the next node to process.
     */
    @Test
    public void testGet() throws InterruptedException {
        List<CacheManager> cluster = createCluster("testGet", 2);
        try {
            Ehcache cache1 = cluster.get(0).getCache(SAMPLE_CACHE_NOREP);
            Ehcache cache2 = cluster.get(1).getCache(SAMPLE_CACHE_NOREP);

            Serializable key = "1";
            Serializable value = new Date();
            Element element = new Element(key, value);

            //Put
            cache1.put(element);
            Thread.sleep(2050);


            //Should not have been replicated to cache2.
            assertThat(cache2.get(key), nullValue());

            //Should load from cache1
            for (int i = 0; i < 120; i++) {
                assertThat(cache2.getWithLoader(key, null, null).getValue(), is(value));
                cache2.remove(key);
            }
        } finally {
            destroyCluster(cluster);
        }
    }
  
  
    /**
     * Tests the JMSCacheLoader.
     * <p/>
     * We put an item in cache1, which does not replicate.
     * <p/>
     * We then do a get on cache2, which has a JMSCacheLoader which should ask the cluster for the answer.
     * If a cache does not have an element it should leave the message on the queue for the next node to process.
     */
    @Test
    public void testGetAll() throws InterruptedException {
        List<CacheManager> cluster = createCluster("testGetAll", 2);
        try {
            Ehcache cache1 = cluster.get(0).getCache(SAMPLE_CACHE_NOREP);
            Ehcache cache2 = cluster.get(1).getCache(SAMPLE_CACHE_NOREP);

            Serializable key = "1";
            Serializable value = new Date();
            Element element = new Element(key, value);
            Element element2 = new Element(2, "dog");
            Element element3 = new Element(3, "cat");

            ArrayList keys = new ArrayList();
            keys.add("1");
            keys.add(2);

            //Put
            cache1.put(element);
            cache1.put(element2);
            cache1.put(element3);
            Thread.sleep(2050);


            //Should not have been replicated to cache2.
            assertThat(cache2.get(key), nullValue());

            //Should load from cache2
            for (int i = 0; i < 120; i++) {
                Map<Object, Object> received = cache2.getAllWithLoader(keys, null);
                assertThat(received.size(), is(2));
                assertThat((Serializable) received.get("1"), is(value));
                assertThat((String) received.get(2), is("dog"));
                cache2.remove(key);
                cache2.remove(2);
            }
        } finally {
            destroyCluster(cluster);
        }
    }
  
  
    /**
     * Tests the JMSCacheLoader.
     * <p/>
     * We put an item in cache1, which does not replicate.
     * <p/>
     * We then do a get on cache2, which has a JMSCacheLoader which should ask the cluster for the answer.
     * If a cache does not have an element it should leave the message on the queue for the next node to process.
     *
     * @throws InterruptedException -
     */
    @Test
    public void testGetTimeout() throws InterruptedException {
        List<CacheManager> cluster = createCluster("testGetTimeout", 2);
        try {
            Ehcache cache1 = cluster.get(0).getCache(SAMPLE_CACHE_NOREP);
            Ehcache cache2 = cluster.get(1).getCache(SAMPLE_CACHE_NOREP);

            Serializable key = "net.sf.ehcache.distribution.jms.Delay";
            Serializable value = new Date();
            Element element = new Element(key, value);

            //Put
            cache1.put(element);
            Thread.sleep(1050);

            //Should not have been replicated to cache2.
            assertThat(cache2.get(key), nullValue());

            //Should timeout loading from cache2
            assertThat(cache2.getWithLoader(key, null, null), nullValue());
            cache2.remove(key);
        } finally {
            destroyCluster(cluster);
        }
    }
  
    /**
     * Tests loading from bootstrap for a cache which is configured to load using RMI and replicate using JMS
     */
    @Test
    public void testBootstrapFromClusterWithAsyncLoader() throws CacheException, InterruptedException {
        List<CacheManager> cluster = createCluster("testBootstrapFromClusterWithAsyncLoader", 2);
        try {
            Ehcache cache1 = cluster.get(0).getCache(SAMPLE_CACHE_JMS_REPLICATION_BOOTSTRAP);
            Ehcache cache2 = cluster.get(1).getCache(SAMPLE_CACHE_JMS_REPLICATION_BOOTSTRAP);

            for (int j = 0; j < 1000; j++) {
                cache1.put(new Element(Integer.valueOf(j),
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            }

            //verify was replicated as usually using JMS
            assertBy(3, TimeUnit.SECONDS, sizeOf(cache2), is(1000));

            forceVMGrowth();

            //Now fire up a new CacheManager and see if bootstrapping using RMI works
            CacheManager manager5 = new CacheManager(ConfigurationFactory.parseConfiguration(getConfiguration()).name("testBootstrapFromClusterWithAsyncLoader5"));
            try {
                assertBy(60, TimeUnit.SECONDS, sizeOf(manager5.getCache(SAMPLE_CACHE_JMS_REPLICATION_BOOTSTRAP)), is(1000));
            } finally {
                manager5.shutdown();
            }
        } finally {
            destroyCluster(cluster);
        }
    }
}
