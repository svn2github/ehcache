package net.sf.ehcache.event;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class EvictionListenerTest {

    private static CacheManager cacheManager;
    private static final String CACHE_NAME = "listening";
    private Cache cache;
    private static final int THREADS = 4;
    private static final int PER_THREAD = 1500;

    @BeforeClass
    public static void createCacheManager() {
        Configuration configuration = new Configuration();
        configuration.diskStore(new DiskStoreConfiguration().path("./target/tmp/"));
        cacheManager = new CacheManager(configuration);
    }

    @Before
    public void setup() {
        CacheConfiguration configuration = new CacheConfiguration(CACHE_NAME, 100)
            .overflowToDisk(true)
            .maxBytesLocalDisk(1, MemoryUnit.MEGABYTES);
        cache = new Cache(configuration);
        cacheManager.addCache(cache);
    }

    @Test
    public void testEvictedOnlyOnce() throws InterruptedException {
        CountingCacheEventListener countingCacheEventListener = new CountingCacheEventListener();
        cache.getCacheEventNotificationService().registerListener(countingCacheEventListener);
        int amountOfEntries = 10000;
        for (int i = 0; i < amountOfEntries; i++) {
            cache.get("key" + (1000 + (i % 10)));
            cache.put(new Element("key" + i, UUID.randomUUID().toString()));
        }
        Thread.sleep(2000);
        assertThat(cache.getMemoryStoreSize(), is(100L));
        System.out.println("\n\n ****");
        System.out.println("DiskStore store size : " + cache.getDiskStoreSize());
        System.out.println(" ****\n\n");
        Map<Object, AtomicInteger> cacheElementsEvicted = countingCacheEventListener.getCacheElementsEvicted(cache);
        assertThat(cacheElementsEvicted.isEmpty(), is(false));
        for (Map.Entry<Object, AtomicInteger> entry : cacheElementsEvicted.entrySet()) {
            assertThat("Evicted multiple times: " + entry.getKey(), entry.getValue().get(), equalTo(1));
        }
        assertThat(cacheElementsEvicted.size(), is((int) (amountOfEntries - cache.getDiskStoreSize())));
    }

    @Test
    public void testGetsAllEvictedKeys() throws InterruptedException {
        CountingCacheEventListener countingCacheEventListener = new CountingCacheEventListener();
        cache.getCacheEventNotificationService().registerListener(countingCacheEventListener);
        Thread[] threads = new Thread[THREADS];
        final AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread() {

                private final int index = counter.getAndIncrement();

                @Override
                public void run() {
                    for (int j = index * PER_THREAD; j < (index + 1) * PER_THREAD; j++) {
                        cache.get("key" + (1000 + (j % 10)));
                        cache.put(new Element("key" + j, UUID.randomUUID().toString()));
                    }
                }
            };
            threads[i].run();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        Thread.sleep(2000);
        assertThat(cache.getMemoryStoreSize(), is(100L));
        Map<Object, AtomicInteger> cacheElementsEvicted = countingCacheEventListener.getCacheElementsEvicted(cache);
        for (Map.Entry<Object, AtomicInteger> entry : cacheElementsEvicted.entrySet()) {
            assertThat("Evicted multiple times: " + entry.getKey(), entry.getValue().get(), equalTo(1));
        }
        assertThat(cache.getSize(), not(is(0)));
        assertThat(cacheElementsEvicted.size() + cache.getSize(), is(THREADS * PER_THREAD));
    }

    @After
    public void tearDown() {
        cacheManager.removeCache(CACHE_NAME);
    }

    @AfterClass
    public static void cleanUp() {
        cacheManager.shutdown();
    }

    private static class CountingCacheEventListener implements CacheEventListener {

        private final ConcurrentMap<String, ConcurrentMap<Object, AtomicInteger>> evictions =
            createMap();

        public void notifyElementRemoved(final Ehcache cache, final Element element) throws CacheException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void notifyElementPut(final Ehcache cache, final Element element) throws CacheException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void notifyElementUpdated(final Ehcache cache, final Element element) throws CacheException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void notifyElementExpired(final Ehcache cache, final Element element) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void notifyElementEvicted(final Ehcache cache, final Element element) {
            getCounterFor(element, getEntriesFor(cache, evictions)).incrementAndGet();
        }

        public void notifyRemoveAll(final Ehcache cache) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void dispose() {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            throw new UnsupportedOperationException("Don't clone me!");
        }

        private AtomicInteger getCounterFor(final Element element, final ConcurrentMap<Object, AtomicInteger> entries) {
            AtomicInteger counter = entries.get(element.getKey());
            if (counter == null) {
                counter = new AtomicInteger(0);
                AtomicInteger previous = entries.putIfAbsent(element.getKey(), counter);
                if (previous != null) {
                    counter = previous;
                }
            }
            return counter;
        }

        private ConcurrentMap<Object, AtomicInteger> getEntriesFor(final Ehcache cache,
                                                                   final ConcurrentMap<String, ConcurrentMap<Object, AtomicInteger>> map) {
            ConcurrentMap<Object, AtomicInteger> entries;
            entries = map.get(cache.getName());
            if (entries == null) {
                entries = new ConcurrentHashMap<Object, AtomicInteger>();
                ConcurrentMap<Object, AtomicInteger> previous = map.putIfAbsent(cache.getName(), entries);
                if (previous != null) {
                    entries = previous;
                }
            }
            return entries;
        }

        private <K, V> ConcurrentHashMap<K, V> createMap() {
            return new ConcurrentHashMap<K, V>();
        }

        public Map<Object, AtomicInteger> getCacheElementsEvicted(final Cache cache) {
            return getEntriesFor(cache, evictions);
        }
    }
}
