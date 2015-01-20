package net.sf.ehcache.util;

import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class WeakIdentityConcurrentMapTest {

    private Long someKey = 1024L;

    @Test
    public void testReturnsValueAndCleanUpsProperly() {

        final ConcurrentMap<String, AtomicLong> cleanedUpValues = new ConcurrentHashMap<String, AtomicLong>();

        final WeakIdentityConcurrentMap<Long, String> map = new WeakIdentityConcurrentMap<Long, String>(new WeakIdentityConcurrentMap.CleanUpTask<String>() {
            public void cleanUp(final String value) {
                final AtomicLong previous = cleanedUpValues.putIfAbsent(value, new AtomicLong(1));
                if(previous != null) {
                    previous.incrementAndGet();
                }
            }
        });

        final String value1 = "someValue for 1";
        final String value2 = "someValue for 1024";
        assertThat(map.putIfAbsent(1L, value1), nullValue());
        assertThat(map.putIfAbsent(someKey, value2), nullValue());
        assertThat(map.putIfAbsent(someKey, value2), equalTo(value2));
        assertThat(map.get(1L), equalTo(value1));
        assertThat(map.get(someKey), equalTo(value2));
        someKey = null;

        RetryAssert.assertBy(10, SECONDS, new Callable<Integer>() {
            public Integer call() throws Exception {
                int i = 0;
                while (i++ < 50) {
                    System.gc();
                    assertThat(map.get(someKey), nullValue());
                }
                return cleanedUpValues.size();
            }
        }, Is.is(1));

        assertThat(cleanedUpValues.get(value2), notNullValue());
        assertThat(cleanedUpValues.get(value2).get(), is(1L));
    }
}
