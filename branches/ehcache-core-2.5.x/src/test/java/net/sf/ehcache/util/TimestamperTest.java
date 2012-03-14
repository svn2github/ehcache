package net.sf.ehcache.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class TimestamperTest {

    public static final int TOTAL_RUNS = 750000;
    public static final int THREADS  = 8;

    @Test
    public void testCorrectness() throws Exception {
        final ConcurrentMap<Long, Integer> values = new ConcurrentHashMap<Long, Integer>();
        final AtomicLong errors = new AtomicLong();
        
        Thread[] threads = new Thread[THREADS];
        for(int i =0; i < THREADS; i++) {
            threads[i] = new Thread() {

                @Override
                public void run() {
                    for (int i = 0; i < (TOTAL_RUNS / THREADS); i++) {
                        if(values.putIfAbsent(Timestamper.next(), 0) != null) {
                            errors.incrementAndGet();
                        }
                    }
                }
            };
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertThat(errors.get(), is(0L));
    }
    
    @Test
    public void testLatency() throws Exception {
        final AtomicBoolean stopped = new AtomicBoolean(false);
        final long[] maxima = new long[THREADS];
        final int[] loops = new int[THREADS];
        Thread[] threads = new Thread[THREADS];
        for(int i =0; i < THREADS; i++) {
            final int index = i;
            threads[index] = new Thread() {

                @Override
                public void run() {
                    long max = 0;
                    int runs;
                    for (runs = 0; !stopped.get() && runs < (TOTAL_RUNS / THREADS); runs++) {
                        long start = System.nanoTime();
                        Timestamper.next();
                        long duration = System.nanoTime() - start;
                        max = Math.max(max, duration);
                        /*
                         * Schedulers are dumb - make sure everyone gets a fair share of cpu.
                         */
                        Thread.yield();
                    }
                    stopped.set(true);
                    maxima[index] = max;
                    loops[index] = runs;
                }
            };
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        
        for (int i = 0; i < THREADS; i++) {
            System.out.println(threads[i] + " " + loops[i] + " runs, maximum latency " + TimeUnit.NANOSECONDS.toMillis(maxima[i]) + "ms");
        }
        
        for (int i = 0; i < THREADS; i++) {
            Assert.assertThat(maxima[i], lessThan(TimeUnit.MILLISECONDS.toNanos(200)));
        }
    }
}
