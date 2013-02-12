package net.sf.ehcache.event;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.NotSerializableException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import junit.framework.Assert;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.disk.DiskStorageFactory;
import net.sf.ehcache.store.disk.DiskStoreHelper;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Alex Snaps
 */
public class EvictionListenerTest {

    private static CacheManager cacheManager;
    private static final String CACHE_NAME = "listening";
    private Cache cache;
    private static final int THREADS = 6;
    private static final int PER_THREAD = 15000;

    @BeforeClass
    public static void createCacheManager() {
        Configuration configuration = new Configuration();
        configuration.diskStore(new DiskStoreConfiguration().path("./target/tmp/"));
        cacheManager = new CacheManager(configuration);
        Logger.getLogger(DiskStorageFactory.class.getName()).setFilter(new Filter() {
            public boolean isLoggable(LogRecord lr) {
                return !(lr.getThrown() instanceof NotSerializableException);
            }
        });
    }

    @Before
    public void setup() {
        CacheConfiguration configuration = new CacheConfiguration(CACHE_NAME, 100).overflowToDisk(true)
            .maxBytesLocalDisk(1, MemoryUnit.MEGABYTES);
        cache = new Cache(configuration);
        cacheManager.addCache(cache);
    }

    @Test
    public void testEvictedOnlyOnce() throws InterruptedException, ExecutionException {
        CountingCacheEventListener countingCacheEventListener = new CountingCacheEventListener();
        cache.getCacheEventNotificationService().registerListener(countingCacheEventListener);
        int amountOfEntries = 10000;
        for (int i = 0; i < amountOfEntries; i++) {
            cache.get("key" + (1000 + (i % 10)));
            cache.put(new Element("key" + i, UUID.randomUUID().toString()));
        }
        DiskStoreHelper.flushAllEntriesToDisk(cache).get();
        assertThat(cache.getMemoryStoreSize(), is(100L));
        final int diskStoreSize = cache.getDiskStoreSize();
        Map<Object, AtomicInteger> cacheElementsEvicted = countingCacheEventListener.getCacheElementsEvicted(cache);
        System.out.println("\n\n ****");
        System.out.println("DiskStore store size : " + diskStoreSize);
        System.out.println(" ****\n\n");
        assertThat(cacheElementsEvicted.isEmpty(), is(false));
        for (Map.Entry<Object, AtomicInteger> entry : cacheElementsEvicted.entrySet()) {
            assertThat("Evicted multiple times: " + entry.getKey(), entry.getValue().get(), equalTo(1));
        }
        for (int i = 0; i < amountOfEntries; i++) {
            String key = "key" + i;
            if (!cache.isKeyInCache(key) && !cacheElementsEvicted.containsKey(key)) {
                final String message = "Key '" + key + "' isn't in cache & we didn't get notified about its eviction!";
                System.out.println(message);
                assertThat(cacheElementsEvicted.size(), is((amountOfEntries - diskStoreSize)));
                fail(message);
            }
        }
    }

    @Test
    public void testEvictedForL2() throws InterruptedException {
        CacheConfiguration configuration = new CacheConfiguration().name("testEvictedForL2").maxBytesLocalHeap(1, MemoryUnit.KILOBYTES);
        Cache noDiskCache = new Cache(configuration);
        cacheManager.addCache(noDiskCache);

        CountingCacheEventListener countingCacheEventListener = new CountingCacheEventListener();
        noDiskCache.getCacheEventNotificationService().registerListener(countingCacheEventListener);
        int amountOfEntries = 10000;
        for (int i = 0; i < amountOfEntries; i++) {
            // cache.get("key" + (1000 + (i % 10)));
            Element element = new Element("key" + i, UUID.randomUUID().toString());
            noDiskCache.setPinned(element.getObjectKey(), true);
            element.setEternal(true);
            noDiskCache.put(element);
        }
        Thread.sleep(2000);
        System.out.println("\n\n ****");
        System.out.println("Memory store size before  : " + noDiskCache.getMemoryStoreSize());
        System.out.println(" ****\n\n");

        // Try putting an unpinned element and we should see an eviction
        Element element = new Element("key" + amountOfEntries, UUID.randomUUID().toString());
        noDiskCache.put(element);

        Element expectedElement = null;
        assertThat(noDiskCache.get("key" + amountOfEntries), is(expectedElement));

        Map<Object, AtomicInteger> cacheElementsEvicted = countingCacheEventListener.getCacheElementsEvicted(noDiskCache);

        System.out.println("\n\n ****");
        System.out.println("Memory store size after : " + noDiskCache.getMemoryStoreSize());
        System.out.println(" ****\n\n");

        assertThat(cacheElementsEvicted.isEmpty(), is(false));
        assertThat(cacheElementsEvicted.size(), is(1));
        for (Map.Entry<Object, AtomicInteger> entry : cacheElementsEvicted.entrySet()) {
            assertThat("Evicted multiple times: " + entry.getKey(), entry.getValue().get(), equalTo(1));
        }

        noDiskCache.unpinAll();
        for (int i = 0; i < amountOfEntries; i++) {
            Object key = "key"+i;
            Assert.assertFalse("key "+key+" still pinned", noDiskCache.isPinned(key));
        }

        // triggering eviction via this put
        element = new Element("key" + amountOfEntries, UUID.randomUUID().toString());
        noDiskCache.put(element);

        // now eviction should have happened for all keys which are unpinned just above
        cacheElementsEvicted = countingCacheEventListener.getCacheElementsEvicted(noDiskCache);
        System.out.println("after eviction, Memory store size : " + noDiskCache.getMemoryStoreSize()+" size "+noDiskCache.getSize()+" evicted "+cacheElementsEvicted.size());
        Assert.assertTrue(cacheElementsEvicted.size() > 1);
        Assert.assertTrue("cache size "+noDiskCache.getSize(), noDiskCache.getSize() < amountOfEntries);
        Assert.assertTrue("memoryStore size "+noDiskCache.getMemoryStoreSize(), noDiskCache.getMemoryStoreSize() < amountOfEntries);
    }

    @Test
    public void testGetsAllEvictedKeys() throws InterruptedException, ExecutionException {
        CountingCacheEventListener countingCacheEventListener = accessCache(cache);
        DiskStoreHelper.flushAllEntriesToDisk(cache).get();
        assertThat(cache.getMemoryStoreSize(), is(100L));
        Map<Object, AtomicInteger> cacheElementsEvicted = countingCacheEventListener.getCacheElementsEvicted(cache);
        for (Map.Entry<Object, AtomicInteger> entry : cacheElementsEvicted.entrySet()) {
            assertThat("Evicted multiple times: " + entry.getKey(), entry.getValue().get(), equalTo(1));
        }
        assertThat(cache.getSize(), not(is(0)));
        assertThat(cacheElementsEvicted.size() + cache.getSize(), is(THREADS * PER_THREAD));
    }

    @Test
    public void testGetsAllEvictedKeysClockEviction() throws InterruptedException, ExecutionException {
        cacheManager.removeCache(CACHE_NAME);
        CacheConfiguration configuration = new CacheConfiguration(CACHE_NAME, 100)
            .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.CLOCK);
        cache = new Cache(configuration);
        cacheManager.addCache(cache);
        CountingCacheEventListener countingCacheEventListener = accessCache(cache);
        Map<Object, AtomicInteger> cacheElementsEvicted = countingCacheEventListener.getCacheElementsEvicted(cache);
        for (Map.Entry<Object, AtomicInteger> entry : cacheElementsEvicted.entrySet()) {
            assertThat("Evicted multiple times: " + entry.getKey(), entry.getValue().get(), equalTo(1));
        }
        assertThat(cache.getSize(), not(is(0)));
        assertThat(cacheElementsEvicted.size() + cache.getSize(), is(THREADS * PER_THREAD));
    }

    @Test
    public void testGetsAllEvictedKeysWithoutDiskSizeBased() throws InterruptedException {
        CacheConfiguration configuration = new CacheConfiguration().name("noDisk").maxBytesLocalHeap(100, MemoryUnit.KILOBYTES);
        final Cache noDiskCache = new Cache(configuration);
        cacheManager.addCache(noDiskCache);
        CountingCacheEventListener countingCacheEventListener = accessCache(noDiskCache);
        assertThat(noDiskCache.getMemoryStoreSize() <= noDiskCache.getCacheConfiguration().getMaxBytesLocalHeap(), is(true));
        Map<Object, AtomicInteger> cacheElementsEvicted = countingCacheEventListener.getCacheElementsEvicted(noDiskCache);
        for (Map.Entry<Object, AtomicInteger> entry : cacheElementsEvicted.entrySet()) {
            assertThat("Evicted multiple times: " + entry.getKey(), entry.getValue().get(), equalTo(1));
        }
        assertThat(noDiskCache.getSize(), not(is(0)));
        assertThat(cacheElementsEvicted.size(), not(is(0)));
        System.out.println(noDiskCache.getSize());
        assertThat(cacheElementsEvicted.size() + noDiskCache.getSize(), is(THREADS * PER_THREAD));
    }

    @Test
    public void testGetsAllEvictedKeysWithoutDiskEntryBased() throws InterruptedException {
        CacheConfiguration configuration = new CacheConfiguration().name("noDiskEntry").maxEntriesLocalHeap(100);
        final Cache noDiskCache = new Cache(configuration);
        cacheManager.addCache(noDiskCache);
        CountingCacheEventListener countingCacheEventListener = accessCache(noDiskCache);
        assertThat(noDiskCache.getMemoryStoreSize(), is(100L));
        Map<Object, AtomicInteger> cacheElementsEvicted = countingCacheEventListener.getCacheElementsEvicted(noDiskCache);
        for (Map.Entry<Object, AtomicInteger> entry : cacheElementsEvicted.entrySet()) {
            assertThat("Evicted multiple times: " + entry.getKey(), entry.getValue().get(), equalTo(1));
        }
        assertThat(noDiskCache.getSize(), not(is(0)));
        assertThat(cacheElementsEvicted.size(), not(is(0)));
        System.out.println(noDiskCache.getSize());
        assertThat(cacheElementsEvicted.size() + noDiskCache.getSize(), is(THREADS * PER_THREAD));
    }

    @Test
    public void testGetsAllEvictedKeysWithDiskEntryBased() throws InterruptedException, ExecutionException {
        CacheConfiguration configuration = new CacheConfiguration().name("diskEntry").maxEntriesLocalHeap(100)
            .overflowToDisk(true).maxEntriesLocalDisk(2000);
        final Cache diskCache = new Cache(configuration);
        cacheManager.addCache(diskCache);
        CountingCacheEventListener countingCacheEventListener = accessCache(diskCache);
        DiskStoreHelper.flushAllEntriesToDisk(diskCache).get();
        assertThat(diskCache.getMemoryStoreSize(), is(100L));
        Map<Object, AtomicInteger> cacheElementsEvicted = countingCacheEventListener.getCacheElementsEvicted(diskCache);
        for (Map.Entry<Object, AtomicInteger> entry : cacheElementsEvicted.entrySet()) {
            assertThat("Evicted multiple times: " + entry.getKey(), entry.getValue().get(), equalTo(1));
        }
        assertThat(diskCache.getSize(), not(is(0)));
        assertThat(cacheElementsEvicted.size(), not(is(0)));
        DiskStoreHelper.flushAllEntriesToDisk(diskCache).get();
        System.out.println(diskCache.getSize());
        assertThat(cacheElementsEvicted.size() + diskCache.getSize(), is(THREADS * PER_THREAD));
    }

    private CountingCacheEventListener accessCache(final Cache cacheUT) throws InterruptedException {
        CountingCacheEventListener countingCacheEventListener = new CountingCacheEventListener();
        cacheUT.getCacheEventNotificationService().registerListener(countingCacheEventListener);
        Thread[] threads = new Thread[THREADS];
        final AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread() {

                private final int index = counter.getAndIncrement();

                @Override
                public void run() {
                    for (int j = index * PER_THREAD; j < (index + 1) * PER_THREAD; j++) {
                        cacheUT.get("key" + (1000 + (j % 10)));
                        if (j % 125 == 0) {
                            cacheUT.put(new Element("key" + j, new Object()));
                        } else {
                            cacheUT.put(new Element("key" + j, UUID.randomUUID().toString()));
                        }
                    }
                }
            };
            threads[i].run();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        return countingCacheEventListener;
    }

    @Test
    public void testEvictionDuplicates() throws Exception {
        CacheConfiguration configuration = new CacheConfiguration().name("heapOnly").maxBytesLocalHeap(4, MemoryUnit.KILOBYTES).eternal(true).overflowToOffHeap(false);
        final Cache heapOnlyCache = new Cache(configuration);
        cacheManager.addCache(heapOnlyCache);

        final ConcurrentHashMap<Object, AtomicInteger> evicted = new ConcurrentHashMap<Object, AtomicInteger>();
        heapOnlyCache.getCacheEventNotificationService().registerListener(new CacheEventListenerAdapter(){
            @Override
            public void notifyElementEvicted(Ehcache cache, Element element) {
                AtomicInteger old = evicted.put(element.getObjectKey(), new AtomicInteger(1));
                if (old != null) {
                    fail("Got multiple evictions for " + element.getObjectKey() + "! Evicted " + old.incrementAndGet() + " times");
                }
            }
        });

        Putter[] putters = new Putter[2];
        for (int i = 0; i < 2; i++) {
            putters[i] = new Putter(i, heapOnlyCache);
        }
        for (Putter putter : putters) {
            putter.start();
        }
        for (Putter putter : putters) {
            putter.join();
            assertFalse(putter.failed);
        }
    }

    private static final class Putter extends Thread {
        private final int id;
        private final Cache c;
        private volatile boolean failed;


        private Putter(int id, Cache c) {
            this.id = id;
            this.c = c;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < 10000; i++) {
                    c.put(new Element(id + "-" + i, "" + i));
                }
            } catch (Throwable t) {
                t.printStackTrace();
                failed = true;
            }
        }
    }

    @After
    public void tearDown() {
        cacheManager.removalAll();
    }

    @AfterClass
    public static void cleanUp() {
        cacheManager.shutdown();
    }

    private static class CountingCacheEventListener implements CacheEventListener {

        private final ConcurrentMap<String, ConcurrentMap<Object, AtomicInteger>> evictions = createMap();

        public void notifyElementRemoved(final Ehcache cache, final Element element) throws CacheException {
            // To change body of implemented methods use File | Settings | File Templates.
        }

        public void notifyElementPut(final Ehcache cache, final Element element) throws CacheException {
            // To change body of implemented methods use File | Settings | File Templates.
        }

        public void notifyElementUpdated(final Ehcache cache, final Element element) throws CacheException {
            // To change body of implemented methods use File | Settings | File Templates.
        }

        public void notifyElementExpired(final Ehcache cache, final Element element) {
            // To change body of implemented methods use File | Settings | File Templates.
        }

        public void notifyElementEvicted(final Ehcache cache, final Element element) {
            getCounterFor(element, getEntriesFor(cache, evictions)).incrementAndGet();
        }

        public void notifyRemoveAll(final Ehcache cache) {
            // To change body of implemented methods use File | Settings | File Templates.
        }

        public void dispose() {
            // To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            super.clone();
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
