package net.sf.ehcache;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Alex Snaps
 */
public class ClockMemoryStoreTest extends MemoryStoreTester {

    private static final int ENTRIES = 40 * 1000 * 1000;

    /**
     * setup test
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        createMemoryOnlyStore(MemoryStoreEvictionPolicy.CLOCK);
    }

    @Test
    @Ignore
    public void testPerfStuff() throws Exception {

        Thread[] threads = new Thread[10];

        for (int i = 0, threadsLength = threads.length; i < threadsLength; i++) {
            final int node = i;
            threads[i] = new Thread(null, new Runnable() {
                final AtomicInteger value = new AtomicInteger(node  );
                final Random r = new Random();
                public void run() {
                    int key;
                    while ((key = value.getAndIncrement()) < ENTRIES) {
                        if (key % 2 == 0) {
                            cache.put(new Element(key, "value" + r.nextLong()));
                        } else {
                            cache.get(r.nextInt(ENTRIES));
                        }
                    }
                }
            }, "Pounding #" + i);
            threads[i].start();
        }

        final long start = System.nanoTime();
        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start));
        System.out.println(cache.getStatistics().getLocalHeapSize());
    }

    @Test
    public void testNoReadsButStillEvicts() {
        createMemoryOnlyStore(MemoryStoreEvictionPolicy.CLOCK, 150);
        for(int i = 0; i < 1000; i++) {
            cache.put(new Element(i, i));
        }
        assertThat(cache.getStatistics().getLocalHeapSize(), is(150L));
    }
}
