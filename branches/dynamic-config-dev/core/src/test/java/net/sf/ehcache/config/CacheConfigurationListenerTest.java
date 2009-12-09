/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.ehcache.config;

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.AbstractCacheTest;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author cdennis
 */
public class CacheConfigurationListenerTest extends AbstractCacheTest {

    @Test
    public void testTtiFires() {
        CacheConfiguration config = new CacheConfiguration();
        RecordingListener listener = new RecordingListener();
        config.addListener(listener);

        for (int i = 0; i < 10; i++) {
            config.setTimeToIdleSeconds(i + 1);
        }

        List<Event> events = listener.getFiredEvents();

        Assert.assertEquals(10, events.size());

        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            
            Assert.assertEquals("tti", e.type);
            Assert.assertEquals(Long.valueOf(i), e.oldValue);
            Assert.assertEquals(Long.valueOf(i+1), e.newValue);
        }
    }

    @Test
    public void testTtlFires() {
        CacheConfiguration config = new CacheConfiguration();
        RecordingListener listener = new RecordingListener();
        config.addListener(listener);

        for (int i = 0; i < 10; i++) {
            config.setTimeToLiveSeconds(i + 1);
        }

        List<Event> events = listener.getFiredEvents();

        Assert.assertEquals(10, events.size());

        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);

            Assert.assertEquals("ttl", e.type);
            Assert.assertEquals(Long.valueOf(i), e.oldValue);
            Assert.assertEquals(Long.valueOf(i+1), e.newValue);
        }
    }

    @Test
    public void testDiskCapacityFires() {
        CacheConfiguration config = new CacheConfiguration();
        RecordingListener listener = new RecordingListener();
        config.addListener(listener);

        for (int i = 0; i < 10; i++) {
            config.setMaxElementsOnDisk(i + 1);
        }

        List<Event> events = listener.getFiredEvents();

        Assert.assertEquals(10, events.size());

        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);

            Assert.assertEquals("disk", e.type);
            Assert.assertEquals(Integer.valueOf(i), e.oldValue);
            Assert.assertEquals(Integer.valueOf(i+1), e.newValue);
        }
    }

    @Test
    public void testMemoryCapacityFires() {
        CacheConfiguration config = new CacheConfiguration();
        RecordingListener listener = new RecordingListener();
        config.addListener(listener);

        for (int i = 0; i < 10; i++) {
            config.setMaxElementsInMemory(i + 1);
        }

        List<Event> events = listener.getFiredEvents();

        Assert.assertEquals(10, events.size());

        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);

            Assert.assertEquals("mem", e.type);
            Assert.assertEquals(Integer.valueOf(i), e.oldValue);
            Assert.assertEquals(Integer.valueOf(i+1), e.newValue);
        }
    }

    @Test
    public void testMultipleListeners() {
        CacheConfiguration config = new CacheConfiguration();
        RecordingListener listener1 = new RecordingListener();
        RecordingListener listener2 = new RecordingListener();
        config.addListener(listener1);
        config.addListener(listener2);

        for (int i = 0; i < 10; i++) {
            config.setTimeToIdleSeconds(i);
            config.setTimeToLiveSeconds(i);
            config.setMaxElementsInMemory(i);
            config.setMaxElementsOnDisk(i);
        }

        Assert.assertEquals(40, listener1.getFiredEvents().size());
        Assert.assertEquals(40, listener2.getFiredEvents().size());

        Assert.assertEquals(listener1.getFiredEvents(), listener2.getFiredEvents());
    }

    @Test
    public void testRemovingListeners() {
        CacheConfiguration config = new CacheConfiguration();
        RecordingListener listener1 = new RecordingListener();
        RecordingListener listener2 = new RecordingListener();
        config.addListener(listener1);
        config.addListener(listener2);

        for (int i = 0; i < 5; i++) {
            config.setTimeToIdleSeconds(i);
            config.setTimeToLiveSeconds(i);
            config.setMaxElementsInMemory(i);
            config.setMaxElementsOnDisk(i);
        }

        config.removeListener(listener1);

        for (int i = 5; i < 10; i++) {
            config.setTimeToIdleSeconds(i);
            config.setTimeToLiveSeconds(i);
            config.setMaxElementsInMemory(i);
            config.setMaxElementsOnDisk(i);
        }

        List<Event> events1 = listener1.getFiredEvents();
        List<Event> events2 = listener2.getFiredEvents();
        
        Assert.assertEquals(20, events1.size());
        Assert.assertEquals(40, events2.size());

        for (Event e : events1) {
            Assert.assertTrue(events2.contains(e));
        }
    }

    static class RecordingListener implements CacheConfigurationListener {

        private final List<Event> firedEvents = new ArrayList<Event>();
        
        public void timeToIdleChanged(long oldTti, long newTti) {
            firedEvents.add(new Event("tti", Long.valueOf(oldTti), Long.valueOf(newTti)));
        }

        public void timeToLiveChanged(long oldTtl, long newTtl) {
            firedEvents.add(new Event("ttl", Long.valueOf(oldTtl), Long.valueOf(newTtl)));
        }

        public void diskCapacityChanged(int oldCapacity, int newCapacity) {
            firedEvents.add(new Event("disk", Integer.valueOf(oldCapacity), Integer.valueOf(newCapacity)));
        }

        public void memoryCapacityChanged(int oldCapacity, int newCapacity) {
            firedEvents.add(new Event("mem", Integer.valueOf(oldCapacity), Integer.valueOf(newCapacity)));
        }

        public List<Event> getFiredEvents() {
            return new ArrayList<Event>(firedEvents);
        }

        public void clearFiredEvents() {
            firedEvents.clear();
        }
    }

    static class Event {
        public final String type;
        public final Number oldValue;
        public final Number newValue;

        public Event(String type, Number oldValue, Number newValue) {
            this.type = type;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Event) {
                Event e = (Event) o;
                return type.equals(e.type) && oldValue.equals(e.oldValue) && newValue.equals(e.newValue);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + (this.type != null ? this.type.hashCode() : 0);
            hash = 53 * hash + (this.oldValue != null ? this.oldValue.hashCode() : 0);
            hash = 53 * hash + (this.newValue != null ? this.newValue.hashCode() : 0);
            return hash;
        }
    }
}
