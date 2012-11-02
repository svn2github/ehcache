/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.config;

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.AbstractCacheTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author cdennis
 */
public class CacheConfigurationListenerTest extends AbstractCacheTest {

    @Test
    public void testTtiFires() {
        CacheConfiguration config = new CacheConfiguration();
        RecordingListener listener = new RecordingListener();
        config.addConfigurationListener(listener);

        assertRegistered(listener, config);
        listener.clearFiredEvents();

        for (int i = 0; i < 10; i++) {
            config.setTimeToIdleSeconds(i + 1);
        }

        List<Event> events = listener.getFiredEvents();

        Assert.assertEquals(10, events.size());

        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);

            Assert.assertEquals("tti", e.type);
            Assert.assertEquals(Long.valueOf(i), e.oldValue);
            Assert.assertEquals(Long.valueOf(i + 1), e.newValue);
        }
    }

    @Test
    public void testTtlFires() {
        CacheConfiguration config = new CacheConfiguration();
        RecordingListener listener = new RecordingListener();
        config.addConfigurationListener(listener);

        assertRegistered(listener, config);
        listener.clearFiredEvents();

        for (int i = 0; i < 10; i++) {
            config.setTimeToLiveSeconds(i + 1);
        }

        List<Event> events = listener.getFiredEvents();

        Assert.assertEquals(10, events.size());

        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);

            Assert.assertEquals("ttl", e.type);
            Assert.assertEquals(Long.valueOf(i), e.oldValue);
            Assert.assertEquals(Long.valueOf(i + 1), e.newValue);
        }
    }

    @Test
    public void testDiskCapacityFires() {
        CacheConfiguration config = new CacheConfiguration();
        RecordingListener listener = new RecordingListener();
        config.addConfigurationListener(listener);

        assertRegistered(listener, config);
        listener.clearFiredEvents();

        for (int i = 0; i < 10; i++) {
            config.setMaxElementsOnDisk(i + 1);
        }

        List<Event> events = listener.getFiredEvents();

        Assert.assertEquals(10, events.size());

        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);

            Assert.assertEquals("disk", e.type);
            Assert.assertEquals(Integer.valueOf(i), e.oldValue);
            Assert.assertEquals(Integer.valueOf(i + 1), e.newValue);
        }
    }

    @Test
    public void testMemoryCapacityFires() {
        CacheConfiguration config = new CacheConfiguration();
        RecordingListener listener = new RecordingListener();
        config.addConfigurationListener(listener);

        assertRegistered(listener, config);
        listener.clearFiredEvents();

        for (int i = 0; i < 10; i++) {
            config.setMaxElementsInMemory(i + 1);
        }

        List<Event> events = listener.getFiredEvents();

        Assert.assertEquals(10, events.size());

        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);

            Assert.assertEquals("mem", e.type);
            Assert.assertEquals(Integer.valueOf(i), e.oldValue);
            Assert.assertEquals(Integer.valueOf(i + 1), e.newValue);
        }
    }

    @Test
    public void testLoggingEnableDisable() {
        CacheConfiguration config = new CacheConfiguration();
        RecordingListener listener = new RecordingListener();
        config.addConfigurationListener(listener);

        assertRegistered(listener, config);
        listener.clearFiredEvents();

        config.setLogging(true);
        List<Event> events = listener.getFiredEvents();
        Assert.assertEquals(1, events.size());

        config.setLogging(false);
        events = listener.getFiredEvents();
        Assert.assertEquals(2, events.size());

        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);

            Assert.assertEquals("logging", e.type);
            Assert.assertEquals(Boolean.valueOf(i != 0), e.oldValue);
            Assert.assertEquals(Boolean.valueOf(i == 0), e.newValue);
        }
    }

    @Test
    public void testMultipleListeners() {
        CacheConfiguration config = new CacheConfiguration();
        RecordingListener listener1 = new RecordingListener();
        RecordingListener listener2 = new RecordingListener();
        config.addConfigurationListener(listener1);
        config.addConfigurationListener(listener2);

        assertRegistered(listener1, config);
        assertRegistered(listener2, config);
        listener1.clearFiredEvents();
        listener2.clearFiredEvents();

        for (int i = 0; i < 10; i++) {
            config.setTimeToIdleSeconds(i);
            config.setTimeToLiveSeconds(i);
            config.setMaxElementsInMemory(i);
            config.setMaxElementsOnDisk(i);
        }

        //36 not 40 since the first four events don't change anything...
        Assert.assertEquals(36, listener1.getFiredEvents().size());
        Assert.assertEquals(36, listener2.getFiredEvents().size());

        Assert.assertEquals(listener1.getFiredEvents(), listener2.getFiredEvents());
    }

    @Test
    public void testRemovingListeners() {
        CacheConfiguration config = new CacheConfiguration();
        RecordingListener listener1 = new RecordingListener();
        RecordingListener listener2 = new RecordingListener();
        config.addConfigurationListener(listener1);
        config.addConfigurationListener(listener2);

        assertRegistered(listener1, config);
        assertRegistered(listener2, config);
        listener1.clearFiredEvents();
        listener2.clearFiredEvents();

        for (int i = 0; i < 5; i++) {
            config.setTimeToIdleSeconds(i);
            config.setTimeToLiveSeconds(i);
            config.setMaxElementsInMemory(i);
            config.setMaxElementsOnDisk(i);
        }

        config.removeConfigurationListener(listener1);
        assertDeregistered(listener1, config);

        for (int i = 5; i < 10; i++) {
            config.setTimeToIdleSeconds(i);
            config.setTimeToLiveSeconds(i);
            config.setMaxElementsInMemory(i);
            config.setMaxElementsOnDisk(i);
        }

        config.removeConfigurationListener(listener2);
        assertDeregistered(listener2, config);

        List<Event> events1 = listener1.getFiredEvents();
        List<Event> events2 = listener2.getFiredEvents();

        //17 not 21 since the first four events don't change anything...
        Assert.assertEquals(17, events1.size());
        //37 not 41 since the first four events don't change anything...
        Assert.assertEquals(37, events2.size());

        for (Event e : events1) {
            Assert.assertTrue(events2.contains(e));
        }
    }

    private void assertRegistered(RecordingListener listener, CacheConfiguration config) {
        List<Event> events = listener.getFiredEvents();
        Assert.assertTrue(events.contains(new Event("registered", null, config)));
    }

    private void assertDeregistered(RecordingListener listener, CacheConfiguration config) {
        List<Event> events = listener.getFiredEvents();
        Assert.assertTrue(events.contains(new Event("deregistered", config, null)));
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

        public void loggingChanged(boolean oldValue, boolean newValue) {
            firedEvents.add(new Event("logging", Boolean.valueOf(oldValue), Boolean.valueOf(newValue)));
        }

        public void memoryCapacityChanged(int oldCapacity, int newCapacity) {
            firedEvents.add(new Event("mem", Integer.valueOf(oldCapacity), Integer.valueOf(newCapacity)));
        }

        public void registered(CacheConfiguration config) {
            firedEvents.add(new Event("registered", null, config));
        }

        public void deregistered(CacheConfiguration config) {
            firedEvents.add(new Event("deregistered", config, null));
        }

        public List<Event> getFiredEvents() {
            return new ArrayList<Event>(firedEvents);
        }

        public void clearFiredEvents() {
            firedEvents.clear();
        }

        /**
         * {@inheritDoc}
         */
        public void maxBytesLocalHeapChanged(final long oldValue, final long newValue) {
            // no-op
        }

        /**
         * {@inheritDoc}
         */
        public void maxBytesLocalDiskChanged(final long oldValue, final long newValue) {
            // no-op
        }

        /**
         * {@inheritDoc}
         */
        public void maxEntriesInCacheChanged(final int oldValue, final int newValue) {
            // no-op
        }
    }

    static class Event {
        public final String type;
        public final Object oldValue;
        public final Object newValue;

        public Event(String type, Object oldValue, Object newValue) {
            this.type = type;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Event) {
                Event e = (Event) o;
                return type.equals(e.type)
                        && (oldValue == null ? e.oldValue == null : oldValue.equals(e.oldValue))
                        && (newValue == null ? e.newValue == null : newValue.equals(e.newValue));
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
