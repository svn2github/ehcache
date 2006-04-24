/**
 *  Copyright 2003-2006 Greg Luck
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

package net.sf.ehcache.distribution;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.rmi.UnmarshalException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Listens to {@link net.sf.ehcache.CacheManager} and {@link net.sf.ehcache.Cache} events and propagates those to
 * {@link CachePeer} peers of the Cache asynchronously.
 * <p/>
 * Updates are guaranteed to be replicated in the order in which they are received.
 * <p/>
 * While much faster in operation than {@link RMISynchronousCacheReplicator}, it does suffer from a number
 * of problems. Elements, which may be being spooled to DiskStore may stay around in memory because references
 * are being held to them from {@link EventMessage}s which are queued up. The replication thread runs once
 * per second, limiting the build up. However a lot of elements can be put into a cache in that time. We do not want
 * to get an {@link OutOfMemoryError} using distribution in circumstances when it would not happen if we were
 * just using the DiskStore.
 * <p/>
 * Accordingly, {@link EventMessage}s are held by {@link SoftReference} in the queue,
 * so that they can be discarded if required by the GC to avoid an {@link OutOfMemoryError}. A log message
 * will be issued on each flush of the queue if there were any forced discards. One problem with GC collection
 * of SoftReferences is that the VM (JDK1.5 anyway) will do that rather than grow the heap size to the maximum.
 * The workaround is to either set minimum heap size to the maximum heap size to force heap allocation at start
 * up, or put up with a few lost messages while the heap grows.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class RMIAsynchronousCacheReplicator extends RMISynchronousCacheReplicator {

    /**
     * The amount of time the replication thread sleeps after it detects the replicationQueue is empty
     * before checking again.
     */
    protected static final int REPLICATION_THREAD_INTERVAL = 1000;

    private static final Log LOG = LogFactory.getLog(RMIAsynchronousCacheReplicator.class.getName());

    /**
     * A thread which handles replication, so that replication can take place asynchronously and not hold up the cache
     */
    protected final Thread replicationThread = new ReplicationThread();

    /**
     * A queue of updates.
     */
    protected final List replicationQueue = new LinkedList();

    /**
     * Constructor for internal and subclass use
     *
     * @param replicatePuts
     * @param replicateUpdates
     * @param replicateUpdatesViaCopy
     * @param replicateRemovals
     */
    protected RMIAsynchronousCacheReplicator(
            boolean replicatePuts,
            boolean replicateUpdates,
            boolean replicateUpdatesViaCopy,
            boolean replicateRemovals) {
        super(replicatePuts,
                replicateUpdates,
                replicateUpdatesViaCopy,
                replicateRemovals);
        status = Status.STATUS_ALIVE;
        replicationThread.start();
    }

    /**
     * Main method for the replicationQueue thread.
     * <p/>
     * Note that the replicationQueue thread locks the cache for the entire time it is writing elements to the disk.
     */
    private void replicationThreadMain() {
        while (true) {
            // Wait for elements in the replicationQueue
            while (alive() && replicationQueue.size() == 0) {
                try {
                    Thread.sleep(REPLICATION_THREAD_INTERVAL);
                } catch (InterruptedException e) {
                    LOG.debug("Spool Thread interrupted.");
                    return;
                }
            }
            if (notAlive()) {
                return;
            }
            if (replicationQueue.size() != 0) {
                flushReplicationQueue();
            }
        }
    }


    /**
     * {@inheritDoc}
     * <p/>
     * This implementation queues the put notification for in-order replication to peers.
     *
     * @param cache   the cache emitting the notification
     * @param element the element which was just put into the cache.
     */
    public void notifyElementPut(final Cache cache, final Element element) throws CacheException {
        if (notAlive()) {
            return;
        }

        if (!replicatePuts) {
            return;
        }

        if (!element.isSerializable()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Object with key " + element.getObjectKey() + " is not Serializable and cannot be replicated");
            }
            return;
        }

        synchronized (replicationQueue) {
            replicationQueue.add(new CacheEventMessage(EventMessage.PUT, cache, element));
        }
    }

    /**
     * Called immediately after an element has been put into the cache and the element already
     * existed in the cache. This is thus an update.
     * <p/>
     * The {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method
     * will block until this method returns.
     * <p/>
     * Implementers may wish to have access to the Element's fields, including value, so the element is provided.
     * Implementers should be careful not to modify the element. The effect of any modifications is undefined.
     *
     * @param cache   the cache emitting the notification
     * @param element the element which was just put into the cache.
     */
    public void notifyElementUpdated(final Cache cache, final Element element) throws CacheException {
        if (notAlive()) {
            return;
        }
        if (!replicateUpdates) {
            return;
        }

        if (replicateUpdatesViaCopy) {
            if (!element.isSerializable()) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Object with key " + element.getObjectKey() + " is not Serializable and cannot be updated via copy");
                }
                return;
            }
            replicationQueue.add(new CacheEventMessage(EventMessage.PUT, cache, element));
        } else {
            if (!element.isKeySerializable()) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Key " + element.getObjectKey() + " is not Serializable and cannot be replicated.");
                }
                return;
            }
            replicationQueue.add(new CacheEventMessage(EventMessage.REMOVE, cache, (Serializable) element.getObjectKey()));
        }
    }

    /**
     * Called immediately after an element has been removed. The remove method will block until
     * this method returns.
     * <p/>
     * This implementation queues the removal notification for in order replication to peers.
     *
     * @param cache   the cache emitting the notification
     * @param element just deleted
     */
    public void notifyElementRemoved(final Cache cache, final Element element) throws CacheException {
        if (!replicateRemovals) {
            return;
        }

        if (!element.isKeySerializable()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Key " + element.getObjectKey() + " is not Serializable and cannot be replicated.");
            }
            return;
        }
        synchronized (replicationQueue) {
            replicationQueue.add(new CacheEventMessage(EventMessage.REMOVE, cache, (Serializable) element.getObjectKey()));
        }
    }

    /**
     * Gets called once per {@link #REPLICATION_THREAD_INTERVAL}.
     * <p/>
     * Sends accumulated messages in bulk to each peer. i.e. if ther are 100 messages and 1 peer,
     * 1 RMI invocation results, not 100. Also, if a peer is unavailable this is discovered in only 1 try.
     * <p/>
     * Makes a copy of the queue so as not to hold up the enqueue operations.
     * <p/>
     * Any exceptions are caught so that the replication thread does not die, and because errors are expected,
     * due to peers becoming unavailable.
     * <p/>
     * This method issues warnings for problems that can be fixed with configuration changes.
     */
    private void flushReplicationQueue() {

        int discardCounter = 0;

        Object[] replicationQueueCopy;
        synchronized (replicationQueue) {
            if (replicationQueue.size() == 0) {
                return;
            }

            replicationQueueCopy = replicationQueue.toArray();
            replicationQueue.clear();
        }


        Cache cache = ((CacheEventMessage) replicationQueueCopy[0]).cache;
        List cachePeers = listRemoteCachePeers(cache);

        List resolvedEventMessages = extractAndResolveEventMessages(replicationQueueCopy);


        for (int j = 0; j < cachePeers.size(); j++) {
            CachePeer cachePeer = (CachePeer) cachePeers.get(j);
            try {
                cachePeer.send(resolvedEventMessages);
            } catch (Throwable t) {
                if (t instanceof UnmarshalException) {
                    String message = t.getMessage();
                    if (message.indexOf("Read time out") != 0) {
                        LOG.warn("Unable to send message to remote peer due to socket read timeout. Consider increasing" +
                                " the socketTimeoutMillis setting in the cacheManagerPeerListenerFactory. " +
                                "Message was: " + t.getMessage());
                    }
                } else {
                    LOG.debug("Unable to send message to remote peer.  Message was: " + t.getMessage());
                }

            }
        }
        if (LOG.isWarnEnabled()) {
            int eventMessagesNotResolved = replicationQueueCopy.length - resolvedEventMessages.size();
            if (eventMessagesNotResolved > 0) {
                LOG.warn(eventMessagesNotResolved + " messages were discarded on replicate due to reclamation of " +
                        "SoftReferences by the VM. Consider increasing the maximum heap size and/or setting the " +
                        "starting heap size to a higher value.");
            }

        }
    }

    /**
     * Extracts CacheEventMessages and attempts to get a hard reference to the underlying EventMessage
     *
     * @param replicationQueueCopy
     * @return a list of EventMessages which were able to be resolved
     */
    private List extractAndResolveEventMessages(Object[] replicationQueueCopy) {
        List list = new ArrayList();
        for (int i = 0; i < replicationQueueCopy.length; i++) {
            EventMessage eventMessage = ((CacheEventMessage) replicationQueueCopy[i]).getEventMessage();
            if (eventMessage != null) {
                list.add(eventMessage);
            }
            replicationQueueCopy[i] = null;
        }
        return list;
    }

    /**
     * A background daemon thread that writes objects to the file.
     */
    private class ReplicationThread extends Thread {
        public ReplicationThread() {
            super("Replication Thread");
            setDaemon(true);
            setPriority(2);
        }

        /**
         * Main thread method.
         */
        public void run() {
            replicationThreadMain();
        }
    }

    /**
     * A wrapper around an EventMessage, which enables the element to enqueued along with
     * what is to be done with it.
     * <p/>
     * The wrapper holds a {@link SoftReference} to the {@link EventMessage}, so that the queue is never
     * the cause of an {@link OutOfMemoryError}
     */
    private class CacheEventMessage {

        private Cache cache;
        private SoftReference softEventMessage;

        public CacheEventMessage(int event, Cache cache, Element element) {
            EventMessage eventMessage = new EventMessage(event, element);
            softEventMessage = new SoftReference(eventMessage);
            this.cache = cache;
        }

        public CacheEventMessage(int event, Cache cache, Serializable key) {
            EventMessage eventMessage = new EventMessage(event, key);
            softEventMessage = new SoftReference(eventMessage);
            this.cache = cache;
        }

        /**
         * Gets the component EventMessage
         */
        public EventMessage getEventMessage() {
            return (EventMessage) softEventMessage.get();
        }

    }

    /**
     * Give the replicator a chance to cleanup and free resources when no longer needed
     */
    public void dispose() {
        status = Status.STATUS_SHUTDOWN;
        synchronized (replicationQueue) {
            replicationQueue.clear();
        }

    }


}
