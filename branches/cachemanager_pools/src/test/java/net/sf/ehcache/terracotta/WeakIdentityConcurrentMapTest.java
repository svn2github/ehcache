package net.sf.ehcache.terracotta;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class WeakIdentityConcurrentMapTest {

    private Long someKey = 1024L;

    @Test
    public void testReturnsValueAndCleanUpsProperly() {

        final ConcurrentMap<String, AtomicLong> cleanedUpValues = new ConcurrentHashMap<String, AtomicLong>();

        WeakIdentityConcurrentMap<Long, String> map = new WeakIdentityConcurrentMap<Long, String>(new WeakIdentityConcurrentMap.CleanUpTask<String>() {
            public void cleanUp(final String value) {
                final AtomicLong previous = cleanedUpValues.putIfAbsent(value, new AtomicLong(1));
                if(previous != null) {
                    previous.incrementAndGet();
                }
            }
        });

        final String value1 = "someValue for 1";
        final String value2 = "someValue for 1024";
        assertThat(map.putIfAbsent(1L, value1), CoreMatchers.<Object>nullValue());
        assertThat(map.putIfAbsent(someKey, value2), CoreMatchers.<Object>nullValue());
        assertThat(map.putIfAbsent(someKey, value2), equalTo(value2));
        assertThat(map.get(1L), equalTo(value1));
        assertThat(map.get(someKey), equalTo(value2));
        someKey = null;
        int i = 0;
        while(i++ < 500) {
            System.gc();
            assertThat(map.get(someKey), CoreMatchers.<Object>nullValue());
        }
        assertThat(cleanedUpValues.size(), is(1));
        assertThat(cleanedUpValues.get(value2), CoreMatchers.<Object>notNullValue());
        assertThat(cleanedUpValues.get(value2).get(), is(1L));
    }
}
