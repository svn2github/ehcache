package net.sf.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolParticipant;
import net.sf.ehcache.store.cachingtier.HeapCacheBackEnd;
import net.sf.ehcache.store.cachingtier.PooledBasedBackEnd;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class PooledBasedBackEndTest {

    @Test
    public void testPoolIsMaintained() {
        PooledBasedBackEnd<String, String> pooledBasedBackEnd = new PooledBasedBackEnd<String, String>(new LruPolicy());
        final TestPoolAccessor poolAccessor = new TestPoolAccessor();
        final TestEvictionCallback evictionCallback = new TestEvictionCallback();
        pooledBasedBackEnd.registerEvictionCallback(evictionCallback);
        pooledBasedBackEnd.registerAccessor(poolAccessor);

        poolAccessor.size = 1;
        assertThat(poolAccessor.sum.get(), is(0L));
        pooledBasedBackEnd.putIfAbsent("key", "value");
        assertThat(poolAccessor.sum.get(), is(1L));
        poolAccessor.size = 3;
        pooledBasedBackEnd.remove("key");
        assertThat(poolAccessor.sum.get(), is(0L));
        pooledBasedBackEnd.putIfAbsent("key", "value");
        assertThat(poolAccessor.sum.get(), is(3L));
        poolAccessor.size = 4;
        pooledBasedBackEnd.replace("key", "value", "newValue");
        assertThat(poolAccessor.sum.get(), is(4L));
        pooledBasedBackEnd.remove("key");
        assertThat(poolAccessor.sum.get(), is(0L));

        pooledBasedBackEnd.putIfAbsent("key", "value");
        assertThat(poolAccessor.sum.get(), is(4L));
        poolAccessor.fail = true;
        assertThat(pooledBasedBackEnd.putIfAbsent("key2", "value"), nullValue());
        assertThat(pooledBasedBackEnd.get("key2"), nullValue());
        assertThat(poolAccessor.sum.get(), is(4L));
        assertThat(evictionCallback.counter.get(), is(3L));

        assertThat(pooledBasedBackEnd.replace("key", "value2", "value3"), is(false));
        assertThat(poolAccessor.sum.get(), is(4L));
        assertThat(evictionCallback.counter.get(), is(3L));

        assertThat(pooledBasedBackEnd.replace("key", "value", "value3"), is(true)); // this is forced, won't fail!
        assertThat(poolAccessor.sum.get(), is(4L));
        assertThat(evictionCallback.counter.get(), is(3L));

        poolAccessor.size = 12;
        pooledBasedBackEnd.recalculateSize("key2");
        assertThat(poolAccessor.sum.get(), is(4L));
        assertThat(evictionCallback.counter.get(), is(3L));

        pooledBasedBackEnd.recalculateSize("key");
        assertThat(poolAccessor.sum.get(), is(60L));
        assertThat(evictionCallback.counter.get(), is(3L));

        poolAccessor.size = 8;
        assertThat(pooledBasedBackEnd.remove("key", "value"), is(false));
        assertThat(poolAccessor.sum.get(), is(60L));
        assertThat(evictionCallback.counter.get(), is(3L));

        assertThat(pooledBasedBackEnd.remove("key", "value3"), is(true));
        assertThat(poolAccessor.sum.get(), is(0L));
        assertThat(evictionCallback.counter.get(), is(3L));

        assertThat(pooledBasedBackEnd.keySet().isEmpty(), is(true));
    }

    @Test
    public void testPoolIsMaintainedMultiThreaded() throws InterruptedException {
        final Random random = new Random();
        final PooledBasedBackEnd<String, String> pooledBasedBackEnd = new PooledBasedBackEnd<String, String>(new LruPolicy());
        final TestPoolAccessor poolAccessor = new TestPoolAccessor() {

            ThreadLocal<Integer> localCounter = new ThreadLocal<Integer>() {
                @Override
                protected Integer initialValue() {
                    return 0;
                }
            };

            @Override
            public long add(final Object key, final Object value, final Object container, final boolean force) {
                final int size = localCounter.get() % 20;
                localCounter.set(size + 1);
                super.sum.addAndGet(size);
                return size;
            }
        };
        final TestEvictionCallback evictionCallback = new TestEvictionCallback();
        pooledBasedBackEnd.registerEvictionCallback(evictionCallback);
        pooledBasedBackEnd.registerAccessor(poolAccessor);
        final long seed = random.nextLong();

        for(int i = 0; i < 10000; i++) {
            String kv = Integer.toString(i);
            pooledBasedBackEnd.putIfAbsent(kv, kv);
        }

        Runnable randomAccessor = new Runnable() {

            Random r = new Random(seed);

            @Override
            public void run() {
                for (int i = 0; i < 500000; i++) {
                    final String kv;
                    final List<String> rv;
                    switch(r.nextInt(7)) {
                        case 0:
//                          SEE EHC-1006
                            rv = pooledBasedBackEnd.getRandomValues(1);
                            if (!rv.isEmpty() && rv.get(0) != null) {
                                pooledBasedBackEnd.recalculateSize(rv.get(0));
                            }
                            break;
                        case 1 :
                            rv = pooledBasedBackEnd.getRandomValues(1);
                            if (!rv.isEmpty() && rv.get(0) != null) {
                                pooledBasedBackEnd.remove(rv.get(0));
                            }
                            break;
                        case 2 :
                            rv = pooledBasedBackEnd.getRandomValues(1);
                            if (!rv.isEmpty() && rv.get(0) != null) {
                                pooledBasedBackEnd.remove(rv.get(0), rv.get(0));
                            }
                            break;
                        case 3 :
                            rv = pooledBasedBackEnd.getRandomValues(2);
                            if (rv.size() > 1 && rv.get(0) != null && rv.get(1) != null ) {
                                pooledBasedBackEnd.replace(rv.get(0), rv.get(0), new String(rv.get(1)));
                            }
                            break;
                        default:
                            kv = Integer.toString(r.nextInt(20000));
                            pooledBasedBackEnd.putIfAbsent(kv, new String(kv));
                    }
                }
            }
        };

//        Thread[] threads = new Thread[1];
        Thread[] threads = new Thread[Runtime.getRuntime().availableProcessors() * 2];
        for (int i = 0, threadsLength = threads.length; i < threadsLength; i++) {
            threads[i] = new Thread(randomAccessor);
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        for (String s : pooledBasedBackEnd.keySet()) {
            pooledBasedBackEnd.remove(s);
        }

        assertThat(pooledBasedBackEnd.size(), is(0));
        assertThat(poolAccessor.sum.get(), is(0L));
    }

    @Test
    public void testRemoveNotifiesEvictionCallback() {
        PooledBasedBackEnd backEnd = new PooledBasedBackEnd(new LruPolicy());
        backEnd.registerAccessor(new TestPoolAccessor());
        final Map evicted = new HashMap();
        backEnd.registerEvictionCallback(new HeapCacheBackEnd.EvictionCallback() {
            @Override
            public void evicted(final Object key, final Object value) {
                evicted.put(key, value);
            }
        });
        backEnd.remove("foo");
        assertThat(evicted.size(), is(0));
        final Element theElement = new Element("foo", "bar");
        backEnd.putIfAbsent("foo", theElement);
        assertThat(evicted.size(), is(0));
        backEnd.remove("foo");
        assertThat(evicted.size(), is(1));
        assertThat(evicted.containsKey("foo"), is(true));
        assertThat((Element) evicted.get("foo"), sameInstance(theElement));
        assertThat(backEnd.get("foo"), nullValue());
    }

    @Test
    public void testRemoveSupportsNoEvictionCallbackBeingRegistered() {
        PooledBasedBackEnd backEnd = new PooledBasedBackEnd(new LruPolicy());
        backEnd.registerAccessor(new TestPoolAccessor());
        backEnd.remove("foo");
        backEnd.putIfAbsent("foo", new Element("foo", "bar"));
        assertThat(backEnd.remove("foo"), notNullValue());
        assertThat(backEnd.get("foo"), nullValue());
    }

    private static class TestPoolAccessor implements PoolAccessor {

        volatile long size = 8;
        volatile boolean fail = false;
        private final AtomicLong sum = new AtomicLong();
        private long replaceNewSize = 60;

        @Override
        public long add(final Object key, final Object value, final Object container, final boolean force) {
            if(!force && fail) {
                return -1;
            }
            sum.addAndGet(size);
            return size;
        }

        @Override
        public boolean canAddWithoutEvicting(final Object key, final Object value, final Object container) {
            throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
        }

        @Override
        public long delete(final long size) {
            if(size < 0) {
                throw new IllegalArgumentException("WTF?! " + size);
            }
            sum.addAndGet(-size);
            return size;
        }

        @Override
        public long replace(final long currentSize, final Object key, final Object value, final Object container, final boolean force) {
            if (!force && fail) {
                return -1;
            }
            final long newSize = replaceNewSize - currentSize;
            sum.addAndGet(newSize);
            return newSize;
        }

        @Override
        public long getSize() {
            throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
        }

        @Override
        public void unlink() {
            throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
        }

        @Override
        public PoolParticipant getParticipant() {
            throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
        }

        @Override
        public void setMaxSize(final long newValue) {
            throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
        }

        @Override
        public boolean hasAbortedSizeOf() {
            throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
        }

        @Override
        public long getPoolOccupancy() {
            throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
        }

        @Override
        public long getPoolSize() {
            throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
        }
    }

    private static class TestEvictionCallback implements HeapCacheBackEnd.EvictionCallback<String, String> {

        AtomicLong counter = new AtomicLong();
        ConcurrentMap<String, String> evicted = new ConcurrentHashMap<String, String>();

        @Override
        public void evicted(final String key, final String value) {
            evicted.put(key, value);
            counter.incrementAndGet();
        }
    }
}
