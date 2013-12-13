package net.sf.ehcache.event;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.store.disk.DiskStoreHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Snaps
 */
public class DiskPersistentEviction {

    private CacheManager cacheManager;

    @Before
    public void setup() {
        final Configuration configuration = new Configuration().name(this.getClass().getSimpleName())
            .diskStore(new DiskStoreConfiguration().path(System.getProperty("java.io.tmpdir")));
        cacheManager = new CacheManager(configuration);
    }

    @After
    public void tidy() {
        cacheManager.shutdown();
    }

    @Test
    public void testWontPrematurelyFireListenersOnEviction() throws Exception {

        final ConcurrentMap<Long, Long> evictionCounter = new ConcurrentHashMap<Long, Long>();
        final Set<Long> prematureEvictions = Collections.synchronizedSet(new HashSet<Long>());
        final int maxEntriesLocalHeap = 100;

        cacheManager.addCache(
            new Cache(new CacheConfiguration("testWontPrematurelyFireListenersOnEviction", maxEntriesLocalHeap)
                .overflowToOffHeap(false)
                .maxEntriesLocalDisk(2000)
                .diskPersistent(true)));
        final Cache cache = cacheManager.getCache("testWontPrematurelyFireListenersOnEviction");
        
        cache.getCacheEventNotificationService().registerListener(new CacheEventListenerAdapter() {


            @Override
            public void notifyElementEvicted(Ehcache cache, Element element) {
                final Long key = (Long)element.getKey();
                Long previous = evictionCounter.putIfAbsent(key, 1L);
                if (previous != null) {
                    while (!evictionCounter.replace(key, previous, previous + 1)) {
                        previous = evictionCounter.get(key);
                    }
                }

                Element o = cache.get(element.getKey());
                if (o != null) {
                    prematureEvictions.add((Long)o.getKey());
                }
            }
        });


        final long MAX = maxEntriesLocalHeap * 100;
        final Thread[] threads = new Thread[4];
        final long entries = MAX / threads.length;
        for (int i = 0, threadsLength = threads.length; i < threadsLength; i++) {
            final int index = i;
            threads[i] = new Thread() {
                @Override
                public void run() {
                    for (long i = index * entries; i < (index + 1) * entries; i++) {
                        cache.put(new Element(i, i));
                    }
                }
            };
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        DiskStoreHelper.flushAllEntriesToDisk(cache).get();
        assertTrue(cache.getSize() > maxEntriesLocalHeap);
        assertTrue(cache.getSize() < MAX);
        assertThat("We've had " + prematureEvictions.size() + " premature evictions", prematureEvictions.isEmpty(), is(true));
        for (Map.Entry<Long, Long> entry : evictionCounter.entrySet()) {
            assertThat("Key " + entry.getKey() + " got evicted multiple times!", entry.getValue(), is(1L));
        }
        assertThat((long) evictionCounter.size(), is(MAX - cache.getSize()));
    }
}