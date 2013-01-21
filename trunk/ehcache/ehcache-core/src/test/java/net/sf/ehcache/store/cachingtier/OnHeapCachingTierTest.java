package net.sf.ehcache.store.cachingtier;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.CachingTier;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Alex Snaps
 */
public class OnHeapCachingTierTest {

    public static final String KEY = "value";

    @Test
    public void testOnlyPopulatesOnce() throws InterruptedException, BrokenBarrierException {
        final int threadCount = Runtime.getRuntime().availableProcessors() * 2;

        final CachingTier<String, String> cache = new OnHeapCachingTier<String, String>(new CountBasedBackEnd<String, Object>(threadCount * 2));
        final AtomicBoolean failure = new AtomicBoolean();
        final AtomicInteger invocationCounter = new AtomicInteger();
        final AtomicInteger valuesRead = new AtomicInteger();
        final CyclicBarrier barrier = new CyclicBarrier(threadCount + 1);
        final Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    try {
                        final int await = barrier.await();
                        cache.get(KEY, new Callable<String>() {
                            @Override
                            public String call() throws Exception {
                                invocationCounter.incrementAndGet();
                                return "0x" + Integer.toHexString(await);
                            }
                        }, false);
                        valuesRead.getAndIncrement();
                    } catch (Exception e) {
                        e.printStackTrace();
                        failure.set(true);
                    }
                }
            };
            threads[i].start();
        }

        barrier.await();
        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(failure.get(), is(false));
        assertThat(invocationCounter.get(), is(1));
        assertThat(valuesRead.get(), is(threadCount));
    }

    @Test
    public void testEvictsAtCapacityAnNotifies() {
        final Map<String, String> evictions = new HashMap<String, String>();
        CachingTier<String, String> cache = new OnHeapCachingTier<String, String>(new NoCapacityHeapCacheBackEnd<String>());
        cache.addListener(new CachingTier.Listener<String, String>() {
            @Override
            public void evicted(final String key, final String value) {
                evictions.put(key, value);
            }
        });
        final AtomicInteger createdValues = new AtomicInteger();
        final int loops = 100;

        for (int i = 0; i < loops; i++) {
            final String key = Integer.toString(i);
            final Callable<String> source = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    createdValues.incrementAndGet();
                    return key;
                }
            };
            cache.get(key, source, false);
        }
        assertThat(createdValues.get(), is(loops));
        final Set<String> keys = new HashSet<String>();

        for (int i = 0; i < loops; i++) {
            final String key = Integer.toString(i);
            if (cache.get(key, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return null;
                }
            }, false) != null) {
                keys.add(key);
            }
        }
        assertThat(keys.size(), is(0));
        assertThat(evictions.size(), is(loops));
    }

    @Test
    public void testDoesNotLeakFaults() throws InterruptedException {

        final CountBasedBackEnd<Integer, Object> backEnd = new CountBasedBackEnd<Integer, Object>(10);
        final OnHeapCachingTier<Integer, Element> cachingTier = new OnHeapCachingTier<Integer, Element>(
            backEnd);
        cachingTier.addListener(new CachingTier.Listener<Integer, Element>() {
            @Override
            public void evicted(final Integer key, final Element value) {
                throw new RuntimeException("Just to piss you off...");
            }
        });

        final Runnable runnable = new Runnable() {
            
            ThreadLocal<Integer> counter = new ThreadLocal<Integer>() {
                @Override
                protected Integer initialValue() {
                    return 0;
                }
            };
            
            @Override
            public void run() {
                while (counter.get() < 1000) {
                    try {
                        final Integer val = counter.get();
                        counter.set(val + 1);
                        final Element element = new Element(val, val);
                        cachingTier.get(val, new Callable<Element>() {
                            @Override
                            public Element call() throws Exception {
                                if (val % 100 == 0) {
                                    throw new OutOfMemoryError("Nope, I don't want to create this for you!") {
                                        @Override
                                        public synchronized Throwable fillInStackTrace() {
                                            return null;
                                        }
                                    };
                                } else if (val % 100 == 50) {
                                    return new Element(val, val);
                                }
                                return element;
                            }
                        }, false);
                    } catch (CacheException e) {
                        // happily keep on doing whatever you were doing
                    }
                }
                System.out.println(Thread.currentThread().getName() + " is done!");
            }
        };

        Thread[] threads = new Thread[2];
        for (int i = 0, threadsLength = threads.length; i < threadsLength; i++) {
            threads[i] = new Thread(null, runnable, "Accessor thread #" + (i + 1));
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join(TimeUnit.SECONDS.toMillis(10));
            final boolean alive = thread.isAlive();
            if(alive) {
                thread.getStackTrace();
            }
            assertThat(thread.getName() + " should be done by now!", alive, is(false));
        }

        assertThat(cachingTier.getInMemorySize(), is(10));
        for (Map.Entry<Integer, Object> entry : backEnd.entrySet()) {
            assertThat(entry.getKey() + " should point to an element", entry.getValue(), instanceOf(Element.class));
        }
    }

    /**
     * No need for V here, this can hold anything, as it holds... nothing!
     * @param <K> the key type
     */
    private static class NoCapacityHeapCacheBackEnd<K> implements HeapCacheBackEnd<K, Object> {

        private EvictionCallback<K, Object> callback;

        @Override
        public Object get(final K key) {
            return null;
        }

        @Override
        public Object putIfAbsent(final K key, final Object value) {
            callback.evicted(key, value);
            return null;
        }

        @Override
        public boolean remove(final K key, final Object value) {
            return false;
        }

        @Override
        public boolean replace(final K key, final Object oldValue, final Object newValue) {
            return false;
        }

        @Override
        public Object remove(final K key) {
            return null;
        }

        @Override
        public void clear() {
            // nothing to clear when you hold nothing!
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Set<Map.Entry<K, Object>> entrySet() {
            return Collections.EMPTY_SET;
        }

        @Override
        public void registerEvictionCallback(final EvictionCallback<K, Object> callback) {
            this.callback = callback;
        }

        @Override
        public void recalculateSize(final Object key) {
            // NO OP
        }
    }
}
