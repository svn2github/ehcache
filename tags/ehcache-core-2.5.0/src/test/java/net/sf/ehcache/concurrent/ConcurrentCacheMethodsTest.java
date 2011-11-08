/**
 *  Copyright 2003-2010 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.concurrent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

public class ConcurrentCacheMethodsTest {

    private volatile CacheManager manager;
    private volatile Ehcache cache;

    @Before
    public void setUp() {
        manager = CacheManager.create();
        cache = new Cache(new CacheConfiguration("testCache", 0));
        manager.addCache(cache);
    }

    @After
    public void clearup() {
        manager.removalAll();
        manager.shutdown();
    }

    @Test
    public void testPutIfAbsent() {
        Element e = new Element("key", "value");
        Assert.assertNull(cache.putIfAbsent(e));
        Assert.assertEquals(e, cache.putIfAbsent(new Element("key", "value2")));

        try {
            cache.putIfAbsent(null);
            Assert.fail("putIfAbsent with null Element should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }

        try {
            cache.putIfAbsent(new Element(null, "value"));
            Assert.fail("putIfAbsent with null key should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    @Test
    public void testPutIfAbsentAffectsStats() {
        cache.removeAll();
        cache.setStatisticsEnabled(true);
        cache.getStatistics().clearStatistics();
        assertThat(cache.getStatistics().getCacheMisses(), is(0L));
        assertThat(cache.getStatistics().getCacheHits(), is(0L));

        assertThat(cache.get("someKey"), CoreMatchers.nullValue());
        assertThat(cache.getStatistics().getCacheMisses(), is(1L));
        assertThat(cache.getStatistics().getCacheHits(), is(0L));

        final Element element = new Element("someKey", "someValue");
        assertThat(cache.putIfAbsent(element), nullValue());
        assertThat(cache.getStatistics().getCacheMisses(), is(1L));
        assertThat(cache.getStatistics().getCacheHits(), is(0L));

        assertThat(cache.get("someKey"), sameInstance(element));
        assertThat(cache.getStatistics().getCacheMisses(), is(1L));
        assertThat(cache.getStatistics().getCacheHits(), is(1L));

        assertThat(cache.putIfAbsent(new Element("someKey", "someValue")), sameInstance(element));
        assertThat(cache.getStatistics().getCacheMisses(), is(1L));
        assertThat(cache.getStatistics().getCacheHits(), is(1L));
    }

    @Test
    public void testRemoveElement() {
        Element e = new Element("key", "value");
        cache.put(e);

        Assert.assertFalse(cache.removeElement(new Element("key", "value2")));
        Assert.assertFalse(cache.removeElement(new Element("key2", "value")));
        Assert.assertTrue(cache.removeElement(new Element("key", "value")));

        try {
            cache.removeElement(null);
            Assert.fail("removeElement with null Element should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }

        try {
            cache.removeElement(new Element(null, "value"));
            Assert.fail("removeElement with null key should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }
    }

    @Test
    public void testTwoArgReplace() {
        Assert.assertFalse(cache.replace(new Element("key", "value1"), new Element("key", "value2")));
        cache.put(new Element("key", "value1"));
        Assert.assertTrue(cache.replace(new Element("key", "value1"), new Element("key", "value2")));
        Assert.assertFalse(cache.replace(new Element("key", "value1"), new Element("key", "value2")));

        try {
            cache.replace(null, new Element("key", "value2"));
            Assert.fail("replace with null key should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }

        try {
            cache.replace(new Element("key", "value1"), null);
            Assert.fail("replace with null key should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }

        try {
            cache.replace(null, null);
            Assert.fail("replace with null key should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }

        try {
            cache.replace(new Element(null, "value1"), new Element("key", "value2"));
            Assert.fail("replace with null key should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }

        try {
            cache.replace(new Element("key", "value1"), new Element(null, "value2"));
            Assert.fail("replace with null key should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }

        try {
            cache.replace(new Element(null, "value1"), new Element(null, "value2"));
            Assert.fail("replace with null keys should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }

        try {
            cache.replace(new Element("key", "value1"), new Element("different", "value2"));
            Assert.fail("replace with non-matching keys should throw IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            //expected
        }
    }

    @Test
    public void testOneArgReplace() {

        Assert.assertNull(cache.replace(new Element("key", "value")));
        Assert.assertNull(cache.replace(new Element("key", "value2")));

        Element e = new Element("key", "value");
        cache.put(e);

        Element e2 = new Element("key", "value2");
        Assert.assertEquals(e, cache.replace(e2));

        Assert.assertEquals(cache.get("key").getObjectValue(), e2.getObjectValue());

        try {
            cache.replace(null);
            Assert.fail("replace with null Element should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }

        try {
            cache.replace(new Element(null, "value1"));
            Assert.fail("replace with null keys should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }
    }

    @Test
    public void testMultiThreadedPutIfAbsent() throws InterruptedException, ExecutionException {

        Callable<Element> putIfAbsent = new Callable<Element>() {
            public Element call() throws Exception {
                return cache.putIfAbsent(new Element("key", Long.valueOf(Thread.currentThread().getId())));
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
                    Assert.assertFalse(seenNull);
                    seenNull = true;
                } else if (threadId == null) {
                    threadId = (Long) e.getValue();
                } else {
                    Assert.assertEquals(threadId, e.getValue());
                }
            }
            Assert.assertTrue(seenNull);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(60, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testMultiThreadedRemoveElement() throws InterruptedException, ExecutionException, TimeoutException {

        Callable<Void> removeElement = new Callable<Void>() {
            public Void call() throws Exception {
                while (!cache.removeElement(new Element("key", "value"))) {
                    Thread.yield();
                }
                return null;
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(4 * Runtime.getRuntime().availableProcessors());
        try {
            executor.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    for (int i = 0; i < 100; i++) {
                        cache.put(new Element("key", "value"));
                        while (cache.get("key") != null) {
                            Thread.yield();
                        }
                    }
                    return null;
                }
            });

            List<Future<Void>> futures = executor.invokeAll(Collections.nCopies(100, removeElement));

            for (Future<Void> f : futures) {
                f.get();
            }
            Assert.assertNull(cache.get("key"));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(60, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testMultiThreadedTwoArgReplace() throws InterruptedException, ExecutionException {

        cache.put(new Element("key", Integer.valueOf(0)));

        Callable<Integer> twoArgReplace = new Callable<Integer>() {
            private final AtomicInteger index = new AtomicInteger();

            public Integer call() throws Exception {
                while (true) {
                    Element old = cache.get("key");
                    Element replace = new Element("key", Integer.valueOf(((Integer) old.getObjectValue()).intValue() + 1));
                    if (cache.replace(old, replace)) {
                        return (Integer) replace.getObjectValue();
                    }
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
            Assert.assertEquals(futures.size(), values.size());
            Assert.assertTrue(Integer.valueOf(futures.size()).equals(cache.get("key").getObjectValue()));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(60, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testMultiThreadedOneArgReplace() throws InterruptedException, ExecutionException {

        cache.put(new Element("key", null));

        Callable<Element> oneArgReplace = new Callable<Element>() {
            private final AtomicInteger index = new AtomicInteger();

            public Element call() throws Exception {
                return cache.replace(new Element("key", Integer.valueOf(index.getAndIncrement())));
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(4 * Runtime.getRuntime().availableProcessors());
        try {
            List<Future<Element>> futures = executor.invokeAll(Collections.nCopies(100, oneArgReplace));

            boolean seenNull = false;
            Long threadId = null;
            Set<Integer> indices = new HashSet<Integer>();
            for (Future<Element> f : futures) {
                Element e = f.get();
                if (e.getValue() == null) {
                    Assert.assertFalse(seenNull);
                    seenNull = true;
                } else {
                    indices.add((Integer) e.getObjectValue());
                }
            }
            Assert.assertTrue(seenNull);
            Assert.assertEquals(futures.size() - 1, indices.size());
            Assert.assertFalse(indices.contains(cache.get("key").getObjectValue()));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(60, TimeUnit.SECONDS);
        }
    }


}
