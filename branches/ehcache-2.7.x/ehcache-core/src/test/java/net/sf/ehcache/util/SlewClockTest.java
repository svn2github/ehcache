package net.sf.ehcache.util;

import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alex Snaps
 */
public class SlewClockTest {

    private static final boolean SLOWRUN      = Boolean.getBoolean("TimestamperTest.slowRun");
    private static final long    BACK_IN_TIME = TimeUnit.SECONDS.toMillis(30);
    private static final int     THREADS      = 10;
    private static final long    DURATION     = 15;

    private static volatile SlewClock.TimeProvider timeProvider;

    @BeforeClass
    public static void installPluggableProvider() {
        TimeProviderLoader.setTimeProvider(new SlewClock.TimeProvider() {
            @Override
            public long currentTimeMillis() {
                return timeProvider.currentTimeMillis();
            }
        });
    }

    @Test
    public void testSlewClockEventuallyCatchesUpWithWallClock() throws Throwable {
        final AtomicLong slewStart = new AtomicLong();
        final AtomicLong offset = new AtomicLong(0);
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<Throwable>());
        this.timeProvider = new SlewClock.TimeProvider() {
            public long currentTimeMillis() {
                return System.currentTimeMillis() - offset.get();
            }
        };
        SlewClock.realignWithTimeProvider();
        final AtomicBoolean stopped = new AtomicBoolean(false);
        final AtomicInteger catchingUp = new AtomicInteger();
        SlewClockVerifierThread[] threads = new SlewClockVerifierThread[THREADS];
        for(int i =0; i < THREADS; i++) {
            threads[i] = new SlewClockVerifierThread(stopped, catchingUp, errors, slewStart, i == 0);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        Thread.sleep(TimeUnit.SECONDS.toMillis(DURATION));
        System.out.println("Going back in time by " + BACK_IN_TIME);
        slewStart.set(System.currentTimeMillis());
        offset.set(BACK_IN_TIME);
        Thread.sleep(SLOWRUN ? BACK_IN_TIME * 3 : BACK_IN_TIME * 2);
        stopped.set(true);
        for (SlewClockVerifierThread thread : threads) {
            thread.join();
        }
        if(!errors.isEmpty()) {
            System.err.println("We have " + errors.size() + " error(s) here!");
            throw errors.get(0);
        }
        assertThat(catchingUp.get(), is(THREADS));
    }

    @Test
    public void testHonorsMaxLatencyWhenGoingBackInTimeMultipleTimes() throws Throwable {
        this.timeProvider = mock(SlewClock.TimeProvider.class);

        final RuntimeException tooMuch = new RuntimeException("Querying time too often!");
        long value;
        long lastValue = 101L;

        when(timeProvider.currentTimeMillis()).thenReturn(100L).thenThrow(tooMuch);
        SlewClock.realignWithTimeProvider();
        verifyCurrentTimeMillisInvocationsAndReset(timeProvider, 1);

        when(timeProvider.currentTimeMillis()).thenReturn(lastValue).thenThrow(tooMuch);
        value = assertLatencyIsLessThan(25);
        assertThat(value, greaterThanOrEqualTo(lastValue));
        lastValue = value;
        verifyCurrentTimeMillisInvocationsAndReset(timeProvider, 1);

        when(timeProvider.currentTimeMillis()).thenReturn(50L).thenThrow(tooMuch);
        value = assertLatencyIsLessThan(25);
        assertThat(value, greaterThanOrEqualTo(lastValue));
        lastValue = value;
        verifyCurrentTimeMillisInvocationsAndReset(timeProvider, 1);

        when(timeProvider.currentTimeMillis()).thenReturn(51L, 52L).thenThrow(tooMuch);
        value = assertLatencyIsLessThan(50);
        assertThat(value, greaterThanOrEqualTo(lastValue));
        lastValue = value;
        verifyCurrentTimeMillisInvocationsAndReset(timeProvider, 2);

        when(timeProvider.currentTimeMillis()).thenReturn(53L, 0L, 1L).thenThrow(tooMuch);
        value = assertLatencyIsLessThan(100);
        assertThat(value, greaterThanOrEqualTo(lastValue));
        lastValue = value;
        verifyCurrentTimeMillisInvocationsAndReset(timeProvider, 3);

        when(timeProvider.currentTimeMillis()).thenReturn(2L, 3L).thenThrow(tooMuch);
        value = assertLatencyIsLessThan(50);
        assertThat(value, greaterThanOrEqualTo(lastValue));
        lastValue = value;
        verifyCurrentTimeMillisInvocationsAndReset(timeProvider, 2);

        when(timeProvider.currentTimeMillis()).thenReturn(4L, 5L).thenThrow(tooMuch);
        value = assertLatencyIsLessThan(50);
        assertThat(value, greaterThanOrEqualTo(lastValue));
        lastValue = value;
        verifyCurrentTimeMillisInvocationsAndReset(timeProvider, 2);

        when(timeProvider.currentTimeMillis()).thenReturn(103L).thenThrow(tooMuch);
        value = assertLatencyIsLessThan(25);
        assertThat(value, greaterThanOrEqualTo(lastValue));
        verifyCurrentTimeMillisInvocationsAndReset(timeProvider, 1);
    }

    private static void verifyCurrentTimeMillisInvocationsAndReset(final SlewClock.TimeProvider mock, final int wantedNumberOfInvocations) {
        verify(mock, times(wantedNumberOfInvocations)).currentTimeMillis();
        reset(mock);
    }

    private static long assertLatencyIsLessThan(final int maxLatencyInMillis) {
        long begin = System.nanoTime();
        final long value = SlewClock.timeMillis();
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
        assertThat("SlewClock.timeMillis() latency", duration, Matchers.lessThan((long)maxLatencyInMillis));
        System.err.println("Duration: " + duration + " < " + maxLatencyInMillis);
        return value;
    }

    private static class SlewClockVerifierThread extends Thread {
        private final AtomicBoolean stopped;
        private final AtomicInteger catchingUp;
        private final AtomicLong slewTime;
        private final boolean log;
        private List<Throwable> errors;
        private boolean wasSlewing;
        private int previousSecond;
        private long previous;

        public SlewClockVerifierThread(final AtomicBoolean stopped, final AtomicInteger catchingUp, final List<Throwable> errors,
                                       final AtomicLong slewTime, final boolean log) {
            this.stopped = stopped;
            this.catchingUp = catchingUp;
            this.errors = errors;
            this.slewTime = slewTime;
            this.log = log;
        }

        @Override
        public void run() {
            while (!stopped.get()) {
                long timeMillis = SlewClock.timeMillis();
                if(SlewClock.isThreadCatchingUp()) {
                    if (!wasSlewing) {
                        catchingUp.incrementAndGet();
                    }
                    wasSlewing = true;
                    if(log) {
                        int x = (int) TimeUnit.MILLISECONDS.toSeconds(timeMillis) % 10;
                        if (SLOWRUN || x != previousSecond) {
                            previousSecond = x;
                            System.out.println(new Date(timeMillis) + " (" + timeMillis + "): " +
                                               new Date(TimeProviderLoader.getTimeProvider().currentTimeMillis()) + " " +
                                               SlewClock.behind());
                        }
                    }
                    if (SLOWRUN) {
                        try {
                            Thread.sleep(15);
                        } catch (InterruptedException e) {
                            interrupt();
                        }
                    }
                } else if(wasSlewing) {
                    System.out.println("Caught up in " + (System.currentTimeMillis() - slewTime.get()) + "ms");
                    wasSlewing = false;
                }
                assertThat(timeMillis >= previous, is(true));
                previous = timeMillis;
            }
            try {
                assertThat(SlewClock.isThreadCatchingUp(), is(false));
            } catch (AssertionError e) {
                errors.add(e);
            }
        }
    }
}
