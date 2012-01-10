/**
 * Copyright 2003-2010 Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

public class ConcurrentCacheMethodsTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {
    private final CyclicBarrier   barrier = new CyclicBarrier(getParticipantCount());

    private volatile CacheManager manager;
    private volatile Ehcache      cache;

    public App(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      int index = barrier.await();

      if (index == 0) {
        setup();
        try {
          testPutIfAbsent();
        } finally {
          clearup();
        }
        setup();
        try {
          testRemoveElement();
        } finally {
          clearup();
        }
        setup();
        try {
          testTwoArgReplace();
        } finally {
          clearup();
        }
        setup();
        try {
          testOneArgReplace();
        } finally {
          clearup();
        }
      }
      barrier.await();

      setup();
      try {
        testMultiThreadedPutIfAbsent();
      } finally {
        clearup();
      }
      setup();
      try {
        testMultiThreadedRemoveElement();
      } finally {
        clearup();
      }
      setup();
      try {
        testMultiThreadedTwoArgReplace();
      } finally {
        clearup();
      }
      setup();
      try {
        testMultiThreadedOneArgReplace();
      } finally {
        clearup();
      }
    }

    private void setup() {
      manager = new CacheManager(getClass().getResourceAsStream("/basic-cache-test.xml"));
      cache = manager.getEhcache("test");
    }

    private void clearup() {
      cache.removeAll();
      manager.removalAll();
      manager.shutdown();
    }

    private void testPutIfAbsent() {
      Element e = new Element("testPutIfAbsent", "value");
      assertNull(cache.putIfAbsent(e));
      assertEquals(e, cache.putIfAbsent(new Element("testPutIfAbsent", "value2")));

      try {
        cache.putIfAbsent(null);
        fail("putIfAbsent with null Element should throw NPE");
      } catch (NullPointerException npe) {
        // expected
      }

      try {
        cache.putIfAbsent(new Element(null, "value"));
        fail("putIfAbsent with null key should throw NPE");
      } catch (NullPointerException npe) {
        // expected
      }
    }

    private void testRemoveElement() {
      Element e = new Element("testRemoveElement", "value");
      cache.put(e);

      assertFalse(cache.removeElement(new Element("testRemoveElement", "value2")));
      assertFalse(cache.removeElement(new Element("testRemoveElement2", "value")));
      assertTrue(cache.removeElement(new Element("testRemoveElement", "value")));

      try {
        cache.removeElement(null);
        fail("removeElement with null Element should throw NPE");
      } catch (NullPointerException npe) {
        // expected
      }

      try {
        cache.removeElement(new Element(null, "value"));
        fail("removeElement with null key should throw NPE");
      } catch (NullPointerException npe) {
        // expected
      }
    }

    private void testTwoArgReplace() {
      assertFalse(cache.replace(new Element("testTwoArgReplace", "value1"), new Element("testTwoArgReplace", "value2")));
      cache.put(new Element("testTwoArgReplace", "value1"));
      assertTrue(cache.replace(new Element("testTwoArgReplace", "value1"), new Element("testTwoArgReplace", "value2")));
      assertFalse(cache.replace(new Element("testTwoArgReplace", "value1"), new Element("testTwoArgReplace", "value2")));

      try {
        cache.replace(null, new Element("testTwoArgReplace", "value2"));
        fail("replace with null key should throw NPE");
      } catch (NullPointerException npe) {
        // expected
      }

      try {
        cache.replace(new Element("testTwoArgReplace", "value1"), null);
        fail("replace with null key should throw NPE");
      } catch (NullPointerException npe) {
        // expected
      }

      try {
        cache.replace(null, null);
        fail("replace with null key should throw NPE");
      } catch (NullPointerException npe) {
        // expected
      }

      try {
        cache.replace(new Element(null, "value1"), new Element("testTwoArgReplace", "value2"));
        fail("replace with null key should throw NPE");
      } catch (NullPointerException npe) {
        // expected
      }

      try {
        cache.replace(new Element("testTwoArgReplace", "value1"), new Element(null, "value2"));
        fail("replace with null key should throw NPE");
      } catch (NullPointerException npe) {
        // expected
      }

      try {
        cache.replace(new Element(null, "value1"), new Element(null, "value2"));
        fail("replace with null keys should throw NPE");
      } catch (NullPointerException npe) {
        // expected
      }

      try {
        cache.replace(new Element("testTwoArgReplace", "value1"), new Element("different-testTwoArgReplace", "value2"));
        fail("replace with non-matching keys should throw IllegalArgumentException");
      } catch (IllegalArgumentException iae) {
        // expected
      }
    }

    private void testOneArgReplace() {
      assertNull(cache.replace(new Element("testOneArgReplace", "value")));
      assertNull(cache.replace(new Element("testOneArgReplace", "value2")));

      Element e = new Element("testOneArgReplace", "value");
      cache.put(e);

      Element e2 = new Element("testOneArgReplace", "value2");
      assertEquals(e, cache.replace(e2));

      assertEquals(cache.get("testOneArgReplace").getObjectValue(), e2.getObjectValue());

      try {
        cache.replace(null);
        fail("replace with null Element should throw NPE");
      } catch (NullPointerException npe) {
        // expected
      }

      try {
        cache.replace(new Element(null, "value1"));
        fail("replace with null keys should throw NPE");
      } catch (NullPointerException npe) {
        // expected
      }
    }

    public void testMultiThreadedPutIfAbsent() throws InterruptedException, ExecutionException {
      final Ehcache testCache = this.cache;

      Callable<Element> putIfAbsent = new Callable<Element>() {
        public Element call() throws Exception {
          return testCache.putIfAbsent(new Element("testMultiThreadedPutIfAbsent", Long.valueOf(Thread.currentThread()
              .getId())));
        }
      };

      ExecutorService executor = Executors.newFixedThreadPool(4 * Runtime.getRuntime().availableProcessors());
      try {
        List<Future<Element>> futures = executor.invokeAll(Collections.nCopies(100, putIfAbsent));

        boolean seenNull = false;
        Long threadId = null;
        for (Future<Element> f : futures) {
          Element e = f.get();
          if (e == null) {
            assertFalse(seenNull);
            seenNull = true;
          } else if (e.getValue() instanceof Long) {
            if (threadId == null) {
              threadId = (Long) e.getValue();
            } else {
              assertEquals(threadId, e.getValue());
            }
          } else {
            fail("Unexpected value : " + e.getValue());
          }
        }
        assertTrue(seenNull);
      } finally {
        executor.shutdownNow();
        executor.awaitTermination(60, TimeUnit.SECONDS);
      }
    }

    public void testMultiThreadedRemoveElement() throws InterruptedException, ExecutionException {
      final Ehcache testCache = this.cache;

      final AtomicBoolean finished = new AtomicBoolean(false);
      Callable<Boolean> removeElementCallable = new Callable<Boolean>() {
        private final AtomicInteger counter = new AtomicInteger();

        public Boolean call() throws Exception {
          while (!finished.get()) {
            int count = counter.getAndIncrement();
            while (!finished.get() && !testCache.removeElement(new Element("testMultiThreadedRemoveElement", "value"))) {
              System.err.println("Attempted remove for count: " + count);
              Thread.yield();
            }
            System.err.println(count + ": remove successful for copy hashcode " + this.hashCode());
          }
          return Boolean.TRUE;
        }
      };

      ExecutorService executor = Executors.newFixedThreadPool(4 * Runtime.getRuntime().availableProcessors());
      try {
        Future<Boolean> putFuture = executor.submit(new Callable<Boolean>() {
          public Boolean call() throws Exception {
            for (int i = 0; i < 100; i++) {
              testCache.put(new Element("testMultiThreadedRemoveElement", "value"));
              System.err.println(i + " put done, now waiting for remove...");
              while (testCache.get("testMultiThreadedRemoveElement") != null) {
                Thread.yield();
              }
              System.err.println(i + " get loop exit, remove successful\n");
            }
            return Boolean.TRUE;
          }
        });

        Future<Boolean> removeFuture = executor.submit(removeElementCallable);
        Assert.assertTrue(putFuture.get());
        finished.set(true);
        Assert.assertTrue(removeFuture.get());
        Element element = testCache.get("testMultiThreadedRemoveElement");
        assertNull("Expected null: " + element, element);
      } finally {
        executor.shutdownNow();
        executor.awaitTermination(60, TimeUnit.SECONDS);
      }
    }

    public void testMultiThreadedTwoArgReplace() throws InterruptedException, ExecutionException {
      final Ehcache testCache = this.cache;

      testCache.put(new Element("testMultiThreadedTwoArgReplace", Integer.valueOf(0)));

      Callable<Integer> twoArgReplace = new Callable<Integer>() {
        public Integer call() throws Exception {
          while (true) {
            Element old = testCache.get("testMultiThreadedTwoArgReplace");
            Element replace = new Element("testMultiThreadedTwoArgReplace", Integer.valueOf(((Integer) old
                .getObjectValue()).intValue() + 1));
            if (testCache.replace(old, replace)) { return (Integer) replace.getObjectValue(); }
          }
        }
      };

      ExecutorService executor = Executors.newFixedThreadPool(4 * Runtime.getRuntime().availableProcessors());
      try {
        List<Future<Integer>> futures = executor.invokeAll(Collections.nCopies(100, twoArgReplace));

        Set<Integer> values = new HashSet<Integer>();
        for (Future<Integer> f : futures) {
          values.add(f.get());
        }
        assertEquals(futures.size(), values.size());
        assertTrue(Integer.valueOf(futures.size()).equals(testCache.get("testMultiThreadedTwoArgReplace")
                                                              .getObjectValue()));
      } finally {
        executor.shutdownNow();
        executor.awaitTermination(60, TimeUnit.SECONDS);
      }
    }

    public void testMultiThreadedOneArgReplace() throws InterruptedException, ExecutionException {
      final Ehcache testCache = this.cache;

      testCache.put(new Element("testMultiThreadedOneArgReplace", null));

      Callable<Element> oneArgReplace = new Callable<Element>() {
        private final AtomicInteger index = new AtomicInteger();

        public Element call() throws Exception {
          return testCache.replace(new Element("testMultiThreadedOneArgReplace", Integer.valueOf(index
              .getAndIncrement())));
        }
      };

      ExecutorService executor = Executors.newFixedThreadPool(4 * Runtime.getRuntime().availableProcessors());
      try {
        List<Future<Element>> futures = executor.invokeAll(Collections.nCopies(100, oneArgReplace));

        boolean seenNull = false;
        Set<Integer> indices = new HashSet<Integer>();
        for (Future<Element> f : futures) {
          Element e = f.get();
          if (e.getValue() == null) {
            assertFalse(seenNull);
            seenNull = true;
          } else {
            indices.add((Integer) e.getObjectValue());
          }
        }
        assertTrue(seenNull);
        assertEquals(futures.size() - 1, indices.size());
        assertFalse(indices.contains(testCache.get("testMultiThreadedOneArgReplace").getObjectValue()));
      } finally {
        executor.shutdownNow();
        executor.awaitTermination(60, TimeUnit.SECONDS);
      }
    }

    public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {

      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }
}