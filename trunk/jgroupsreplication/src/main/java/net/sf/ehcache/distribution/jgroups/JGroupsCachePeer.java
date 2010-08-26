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

package net.sf.ehcache.distribution.jgroups;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.CachePeer;
import net.sf.ehcache.store.chm.ConcurrentHashMap;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * Handles {@link CachePeer}functions around a JGroups {@link Channel} and a
 * {@link CacheManager}
 *
 * @author Eric Dalquist
 * @version $Revision$
 */
public class JGroupsCachePeer implements CachePeer {
    private static final Logger LOG = LoggerFactory.getLogger(JGroupsCachePeer.class.getName());

    private static final int CHUNK_SIZE = 100;

    private final CacheManager cacheManager;
    private final Channel channel;
    private final ConcurrentMap<Long, Queue<JGroupEventMessage>> asyncReplicationQueues =
            new ConcurrentHashMap<Long, Queue<JGroupEventMessage>>();
    private final Timer timer;
    private volatile boolean alive;

    /**
     * Create a new {@link CachePeer}
     */
    public JGroupsCachePeer(CacheManager cacheManager, Channel channel) {
        this.cacheManager = cacheManager;
        this.channel = channel;
        this.alive = true;
        timer = new Timer();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void send(List eventMessages) throws RemoteException {
        this.send(null, eventMessages);
    }

    /* ********** Local Methods ********** */

    /**
     * @return Get the address of all members in the cluster
     */
    public List<Address> getGroupMembership() {
        final View view = this.channel.getView();
        return view.getMembers();
    }

    /**
     * @return Get the address of all members in the cluster other than this one
     */
    public List<Address> getOtherGroupMembers() {
        final Address localAddress = this.getLocalAddress();
        final List<Address> members = this.getGroupMembership();

        final List<Address> addresses = new ArrayList<Address>(members.size() - 1);
        for (final Address member : members) {
            if (!member.equals(localAddress)) {
                addresses.add(member);
            }
        }

        return addresses;
    }

    /**
     * @return Get the address of this machine in the cluster
     */
    public Address getLocalAddress() {
        return this.channel.getAddress();
    }

    /**
     * Shutdown the cache peer
     */
    public void dispose() {
        this.alive = false;

        disposeTimer();

        this.flushAllQueues();

        this.asyncReplicationQueues.clear();
    }


    private void disposeTimer() {
        // cancel the timer and all tasks
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }

    /**
     * Sends a list of {@link JGroupEventMessage}s to the specified address, if no address is set the messages
     * are sent to the entire group.
     */
    public void send(Address dest, List<JGroupEventMessage> eventMessages) {
        if (!this.alive || eventMessages == null || eventMessages.isEmpty()) {
            return;
        }

        //Iterate over messages, queuing async messages then sending sync messages immediately
        final LinkedList<JGroupEventMessage> synchronousEventMessages = new LinkedList<JGroupEventMessage>();
        for (final JGroupEventMessage groupEventMessage : eventMessages) {
            if (groupEventMessage.isAsync()) {
                final long asyncTime = groupEventMessage.getAsyncTime();
                final Queue<JGroupEventMessage> queue = this.getMessageQueue(asyncTime);

                queue.add(groupEventMessage);
                LOG.trace("Queued {} for asynchronous sending.", groupEventMessage);
            } else {
                synchronousEventMessages.add(groupEventMessage);
                LOG.trace("Sending {} synchronously.", groupEventMessage);
            }
        }

        //See if there are any messages to send synchronously
        if (synchronousEventMessages.size() == 0) {
            return;
        }

        LOG.debug("Sending {} JGroupEventMessages synchronously.", synchronousEventMessages.size());

        //Send a single message
        if (synchronousEventMessages.size() == 1) {
            final JGroupEventMessage groupEventMessage = eventMessages.get(0);
            this.send(dest, groupEventMessage);
            return;
        }

        //Send multiple messages
        this.send(dest, (Serializable) synchronousEventMessages);
    }

    private Queue<JGroupEventMessage> getMessageQueue(long asyncTime) {
        Queue<JGroupEventMessage> queue = this.asyncReplicationQueues.get(asyncTime);
        if (queue == null) {
            final Queue<JGroupEventMessage> newQueue = new ConcurrentLinkedQueue<JGroupEventMessage>();
            queue = this.asyncReplicationQueues.putIfAbsent(asyncTime, newQueue);
            if (queue == null) {
                LOG.debug("Created asynchronous message queue for {}ms period", asyncTime);

                //New queue, setup a new timer to flush the queue
                final AsyncTimerTask task = new AsyncTimerTask(newQueue);
                timer.schedule(task, asyncTime, asyncTime);

                return newQueue;
            }
        }
        return queue;
    }

    /**
     * Sends a serializable object to the specified address. If no address is specified it is sent to the
     * entire group.
     */
    private void send(Address dest, final Serializable serializable) {
        final byte[] data;
        try {
            data = Util.objectToByteBuffer(serializable);
        } catch (Exception e) {
            LOG.error("Error serializing data, it will not be sent: " + serializable, e);
            return;
        }
        final Message msg = new Message(dest, null, data);
        try {
            this.channel.send(msg);
        } catch (ChannelNotConnectedException e) {
            LOG.error("Failed to send message(s) due to the channel being disconnected: " + serializable, e);
        } catch (ChannelClosedException e) {
            LOG.error("Failed to send message(s) due to the channel being closed: " + serializable, e);
        }
    }

    private void flushAllQueues() {
        for (final Queue<JGroupEventMessage> queue : this.asyncReplicationQueues.values()) {
            this.flushQueue(queue);
        }
    }

    private void flushQueue(Queue<JGroupEventMessage> queue) {
        final List<JGroupEventMessage> events = new ArrayList<JGroupEventMessage>(CHUNK_SIZE);

        while (!queue.isEmpty()) {
            events.clear();

            while (!queue.isEmpty() && events.size() < CHUNK_SIZE) {
                final JGroupEventMessage event = queue.poll();
                if (event.isValid()) {
                    events.add(event);
                } else {
                    LOG.error("Collected soft reference during async flush: " + event);
                }
            }

            LOG.debug("Flushing {} events from asynchronous queue.", events.size());

            send(null, (Serializable) events);
        }
    }

    /**
     * TimerTask that flushes the specified queue when called.
     */
    private final class AsyncTimerTask extends TimerTask {
        private final Queue<JGroupEventMessage> queue;

        private AsyncTimerTask(Queue<JGroupEventMessage> newQueue) {
            this.queue = newQueue;
        }

        @Override
        public void run() {
            if (!alive) {
                this.cancel();
                return;
            }

            flushQueue(this.queue);

            if (!alive) {
                this.cancel();
            }
        }
    }

    /* ********** CachePeer Unused ********** */

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<?> getElements(List keys) throws RemoteException {
        //Not Implemented
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getGuid() throws RemoteException {
        //Not Implemented
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public List<?> getKeys() throws RemoteException {
        //Not Implemented
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() throws RemoteException {
        //Not Implemented
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Serializable key) throws RemoteException {
        //Not Implemented
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getUrl() throws RemoteException {
        //Not Implemented
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getUrlBase() throws RemoteException {
        //Not Implemented
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void put(Element element) throws IllegalArgumentException, IllegalStateException, RemoteException {
        //Not Implemented
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Serializable key) throws IllegalStateException, RemoteException {
        //Not Implemented
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws RemoteException, IllegalStateException {
        //Not Implemented
    }
}
