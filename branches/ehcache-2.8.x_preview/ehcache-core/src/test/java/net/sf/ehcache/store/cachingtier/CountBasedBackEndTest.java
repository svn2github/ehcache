package net.sf.ehcache.store.cachingtier;

import net.sf.ehcache.Element;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class CountBasedBackEndTest {

    @Test
    public void testEvictsWhenAtCapacity() {
        final int limit = 100;
        final CountBasedBackEnd<String, Element> countBasedBackEnd = new CountBasedBackEnd<String, Element>(limit);
        for (int i = 0; i < limit * 10; i++) {
            final String key = Integer.toString(i);
            countBasedBackEnd.putIfAbsent(key, new Element(key, i));
            assertThat("Grew to " + countBasedBackEnd.size(), countBasedBackEnd.size() <= limit, is(true));
        }
        countBasedBackEnd.setMaxEntriesLocalHeap(limit * 2);
        for (int i = 0; i < limit * 10; i++) {
            final String key = Integer.toString(i);
            countBasedBackEnd.putIfAbsent(key, new Element(key, i));
            assertThat(countBasedBackEnd.size() > limit, is(true));
            assertThat("Grew to " + countBasedBackEnd.size(), countBasedBackEnd.size() <= limit * 2, is(true));
        }
    }

    @Test
    public void testEvictsWhenAtCapacityMultiThreaded() throws InterruptedException {
        final int limit = 1000;
        final int threadNumber = Runtime.getRuntime().availableProcessors() * 2;
        final CountBasedBackEnd<String, Element> countBasedBackEnd = new CountBasedBackEnd<String, Element>(limit);
        final AtomicInteger counter = new AtomicInteger(0);
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int i;
                while ((i = counter.getAndIncrement()) < limit * threadNumber * 10) {
                    final String key = Integer.toString(i);
                    countBasedBackEnd.putIfAbsent(key, new Element(key, i));
                }
            }
        };

        Thread[] threads = new Thread[threadNumber];
        for (int i = 0, threadsLength = threads.length; i < threadsLength; i++) {
            threads[i] = new Thread(runnable);
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertThat("Grew to " + countBasedBackEnd.size(), countBasedBackEnd.size() <= limit, is(true));
        int size = countBasedBackEnd.size();
        for (String s : countBasedBackEnd.keySet()) {
            assertThat(countBasedBackEnd.remove(s), notNullValue());
            size--;
        }
        assertThat(size, is(0));
    }

    @Test
    public void testRemoveNotifiesEvictionCallback() {
        CountBasedBackEnd backEnd = new CountBasedBackEnd(10);
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
        CountBasedBackEnd backEnd = new CountBasedBackEnd(10);
        backEnd.remove("foo");
        backEnd.putIfAbsent("foo", new Element("foo", "bar"));
        assertThat(backEnd.remove("foo"), notNullValue());
        assertThat(backEnd.get("foo"), nullValue());
    }
}
