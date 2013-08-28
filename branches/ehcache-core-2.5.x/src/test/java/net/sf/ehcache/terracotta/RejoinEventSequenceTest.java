/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.terracotta;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.cluster.ClusterTopologyListener;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class RejoinEventSequenceTest extends TestCase {
    private static final AtomicBoolean nodeLeftFired = new AtomicBoolean(false);
    private static final Logger LOG = LoggerFactory.getLogger(RejoinEventSequenceTest.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    @Test
    public void testRejoinEventAfterJoinAndOnline() throws Exception {
        final ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        final MockCacheCluster mockCacheCluster = new MockCacheCluster();
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final AtomicBoolean firstTime = new AtomicBoolean(true);
        TerracottaUnitTesting.setupTerracottaTesting(mockFactory, new Runnable() {
            public void run() {
                if(!firstTime.getAndSet(false) && nodeLeftFired.get()) {
                    try {
                        barrier.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (BrokenBarrierException e) {
                        e.printStackTrace();
                    }
                }
                mockCacheCluster.removeAllListeners();
            }

        });
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        CacheManager cacheManager = new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/basic-rejoin-test.xml"));
        RecordingListener listener = new RecordingListener(barrier);
        cacheManager.getCluster(ClusterScheme.TERRACOTTA).addTopologyListener(listener);

        // do rejoin for 20 times
        int n = 0;
        while (n < 20) {
            n++;
            nodeLeftFired.set(false);
            info("================================= Run: " + n + " =========================================================");
            Assert.assertFalse("firstTime should be false", firstTime.get());
            info("Sleeping for 5 secs...");
            Thread.sleep(5000);
            info("firing node left...");
            // trigger rejoin
            mockCacheCluster.fireCurrentNodeLeft();

            info("Sleeping for 2 secs");
            Thread.sleep(2000);
            info("Waiting until rejoin");
            listener.verifyAndWaitUntil(EventType.REJOINED);

            info("Recorded events: ");
            for (Event e : listener.getEvents()) {
                info(e.toString());
            }

            info("Clearing events");
            listener.clearEvents();
            if(nodeLeftFired.get()) {
                barrier.reset();
            }
        }
    }

    private static void info(String string) {
        LOG.info("____ " + string);
    }

    private static final class RecordingListener implements ClusterTopologyListener {
        private final List<Event> events = new ArrayList<Event>();
        private volatile EventType state;
        private final CyclicBarrier barrier;

        public RecordingListener(CyclicBarrier barrier) {
            this.barrier = barrier;
        }

        public synchronized void nodeJoined(ClusterNode node) {
            info("XXX node joined");
            events.add(new Event(EventType.JOINED));
            state = EventType.JOINED;
            notifyAll();
        }

        public synchronized void clearEvents() {
            events.clear();
        }

        public synchronized void nodeLeft(ClusterNode node) {
            info("XXX node left");
            events.add(new Event(EventType.LEFT));
            state = EventType.LEFT;
            notifyAll();
            nodeLeftFired.set(true);
            try {
                barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }

        public synchronized void clusterOnline(ClusterNode node) {
            info("XXX node online");
            events.add(new Event(EventType.ONLINE));
            state = EventType.ONLINE;
            notifyAll();
        }

        public synchronized void clusterOffline(ClusterNode node) {
            info("XXX node offline");
            events.add(new Event(EventType.OFFLINE));
            state = EventType.OFFLINE;
            notifyAll();
        }

        public synchronized void clusterRejoined(ClusterNode oldNode, ClusterNode newNode) {
            info("XXX node rejoined");
            events.add(new Event(EventType.REJOINED));
            state = EventType.REJOINED;
            notifyAll();
        }

        public synchronized List<Event> getEvents() {
            return new ArrayList<RejoinEventSequenceTest.Event>(events);
        }

        public void verifyAndWaitUntil(EventType type) {
            synchronized (this) {
                EventType current = null;
                for (Event e : events) {
                    verifyTransition(current, e.type);
                    current = e.type;
                }
                while (state != type) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void verifyTransition(EventType current, EventType next) {
            if (current == null) {
                return;
            }
            if (!EventType.validateTransition(current, next)) {
                AssertionError error = new AssertionError("Possible transitions from: " + current + " -> "
                        + EventType.getPossibleTransitions(current) + ", but next event received: " + next
                        + ", probably events fired are not in right sequence - " + events);
                LOG.error("Problem in test", error);
                throw error;
            }
        }

    }

    private static class Event {
        private final EventType type;
        private final Thread thread;

        public Event(EventType type) {
            super();
            this.type = type;
            this.thread = Thread.currentThread();
        }

        @Override
        public String toString() {
            return "Event [type=" + type + ", thread=" + thread.getName() + "]";
        }

    }

    private static enum EventType {
        JOINED, ONLINE, OFFLINE, LEFT, REJOINED;
        private static final Map<EventType, List<EventType>> possibleTransitions = new HashMap<RejoinEventSequenceTest.EventType, List<EventType>>();
        static {
            possibleTransitions.put(JOINED, Arrays.asList(new EventType[] { EventType.ONLINE }));
            possibleTransitions.put(ONLINE, Arrays.asList(new EventType[] { EventType.OFFLINE, EventType.REJOINED }));
            possibleTransitions.put(OFFLINE, Arrays.asList(new EventType[] { EventType.ONLINE, EventType.LEFT }));
            possibleTransitions.put(LEFT, Arrays.asList(new EventType[] { EventType.JOINED }));
            possibleTransitions.put(REJOINED, Arrays.asList(new EventType[] { EventType.OFFLINE }));
        }

        public static boolean validateTransition(EventType from, EventType next) {
            List<EventType> list = possibleTransitions.get(from);
            if (list == null) {
                throw new AssertionError("No possible transitions list for: " + from);
            }
            return list.contains(next);
        }

        public static List<EventType> getPossibleTransitions(EventType from) {
            return possibleTransitions.get(from);
        }
    }
}
