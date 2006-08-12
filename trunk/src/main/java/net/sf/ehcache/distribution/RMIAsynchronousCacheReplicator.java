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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
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
 * Accordingly, the Element values in {@link EventMessage}s are held by {@link java.lang.ref.SoftReference} in the queue,
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
    protected Thread replicationThread = new ReplicationThread();

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
    public RMIAsynchronousCacheReplicator(
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
            while (alive() && replicationQueue != null && replicationQueue.size() == 0) {
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
            try {
                if (replicationQueue.size() != 0) {
                    flushReplicationQueue();
                }
            } catch (Throwable e) {
                LOG.warn("Exception on flushing of replication queue: " + e.getMessage()
                        + ". Continuing...", e);
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
    public final void notifyElementPut(final Ehcache cache, final Element element) throws CacheException {
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
        addToReplicationQueue(new CacheEventMessage(EventMessage.PUT, cache, element, null));
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
    public final void notifyElementUpdated(final Ehcache cache, final Element element) throws CacheException {
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
            addToReplicationQueue(new CacheEventMessage(EventMessage.PUT, cache, element, null));
        } else {
            if (!element.isKeySerializable()) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Key " + element.getObjectKey() + " is not Serializable and cannot be replicated.");
                }
                return;
            }
            addToReplicationQueue(new CacheEventMessage(EventMessage.REMOVE, cache, null, element.getKey()));
        }
    }

    /**
     * Called immediately after an attempt to remove an element. The remove method will block until
     * this method returns.
     * <p/>
     * This notification is received regardless of whether the cache had an element matching
     * the removal key or not. If an element was removed, the element is passed to this method,
     * otherwise a synthetic element, with only the key set is passed in.
     * <p/>
     * @param cache   the cache emitting the notification
     * @param element the element just deleted, or a synthetic element with just the key set if
     *                no element was removed.
     */
    public final void notifyElementRemoved(final Ehcache cache, final Element element) throws CacheException {
        if (!replicateRemovals) {
            return;
        }

        if (!element.isKeySerializable()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Key " + element.getObjectKey() + " is not Serializable and cannot be replicated.");
            }
            return;
        }
        addToReplicationQueue(new CacheEventMessage(EventMessage.REMOVE, cache, null, element.getKey()));
    }

    /**
     * Adds a message to the queue.
     * <p/>
     * This method checks the state of the replication thread and warns
     * if it has stopped and then discards the message.
     *
     * @param cacheEventMessage
     */
    protected void addToReplicationQueue(CacheEventMessage cacheEventMessage) {
        if (!replicationThread.isAlive()) {
            LOG.error("CacheEventMessages cannot be added to the replication queue"
                    + " because the replication thread has died.");
        } else {
            synchronized (replicationQueue) {
                replicationQueue.add(cacheEventMessage);
            }
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
        List replicationQueueCopy;
        synchronized (replicationQueue) {
            if (replicationQueue.size() == 0) {
                return;
            }

            replicationQueueCopy = new ArrayList(replicationQueue.size());
            for (int i = 0; i < replicationQueue.size(); i++) {
                CacheEventMessage cacheEventMessage = (CacheEventMessage) replicationQueue.get(i);
                replicationQueueCopy.add(cacheEventMessage);
            }
            replicationQueue.clear();
        }


        Ehcache cache = ((CacheEventMessage) replicationQueueCopy.get(0)).cache;
        List cachePeers = listRemoteCachePeers(cache);

        List resolvedEventMessages = extractAndResolveEventMessages(replicationQueueCopy);


        for (int j = 0; j < cachePeers.size(); j++) {
            CachePeer cachePeer = (CachePeer) cachePeers.get(j);
            try {
                cachePeer.send(resolvedEventMessages);
            } catch (UnmarshalException e) {
                String message = e.getMessage();
                if (message.indexOf("Read time out") != 0) {
                    LOG.warn("Unable to send message to remote peer due to socket read timeout. Consider increasing" +
                            " the socketTimeoutMillis setting in the cacheManagerPeerListenerFactory. " +
                            "Message was: " + e.getMessage());
                } else {
                    LOG.debug("Unable to send message to remote peer.  Message was: " + e.getMessage());
                }
            } catch (Throwable t) {
                LOG.debug("Unable to send message to remote peer.  Message was: " + t.getMessage());
            }
        }
        if (LOG.isWarnEnabled()) {
            int eventMessagesNotResolved = replicationQueueCopy.size() - resolvedEventMessages.size();
            if (eventMessagesNotResolved > 0) {
                LOG.warn(eventMessagesNotResolved + " messages were discarded on replicate due to reclamation of " +
                        "SoftReferences by the VM. Consider increasing the maximum heap size and/or setting the " +
                        "starting heap size to a higher value.");
            }

        }
    }

    /**
     * Extracts CacheEventMessages and attempts to get a hard reference to the underlying EventMessage
     * <p/>
     * If an EventMessage has been invalidated due to SoftReference collection of the Element, it is not
     * propagated. This only affects puts and updates via copy.
     *
     * @param replicationQueueCopy
     * @return a list of EventMessages which were able to be resolved
     */
    private static List extractAndResolveEventMessages(List replicationQueueCopy) {
        List list = new ArrayList();
        for (int i = 0; i < replicationQueueCopy.size(); i++) {
            EventMessage eventMessage = ((CacheEventMessage) replicationQueueCopy.get(i)).getEventMessage();
            if (eventMessage != null && eventMessage.isValid()) {
                list.add(eventMessage);
            }
        }
        return list;
    }

    /**
     * A background daemon thread that writes objects to the file.
     */
    private final class ReplicationThread extends Thread {
        public ReplicationThread() {
            super("Replication Thread");
            setDaemon(true);
            setPriority(2);
        }

        /**
         * Main thread method.
         */
        public final void run() {
            replicationThreadMain();
        }
    }


    /**
     * A wrapper around an EventMessage, which enables the element to be enqueued along with
     * what is to be done with it.
     * <p/>
     * The wrapper holds a {@link java.lang.ref.SoftReference} to the {@link EventMessage}, so that the queue is never
     * the cause of an {@link OutOfMemoryError}
     */
    private static final class CacheEventMessage {

        private final Ehcache cache;
        private final EventMessage eventMessage;

        public CacheEventMessage(int event, Ehcache cache, Element element, Serializable key) {
            eventMessage = new EventMessage(event, key, element);
            this.cache = cache;
        }

        /**
         * Gets the component EventMessage
         */
        public final EventMessage getEventMessage() {
            return eventMessage;
        }

    }

    /**
     * Give the replicator a chance to cleanup and free resources when no longer needed
     */
    public final void dispose() {
        status = Status.STATUS_SHUTDOWN;
        synchronized (replicationQueue) {
            replicationQueue.clear();
        }

    }


    /**
     * Creates a clone of this listener. This method will only be called by ehcache before a cache is initialized.
     * <p/>
     * This may not be possible for listeners after they have been initialized. Implementations should throw
     * CloneNotSupportedException if they do not support clone.
     *
     * @return a clone
     * @throws CloneNotSupportedException if the listener could not be cloned.
     */
    public Object clone() throws CloneNotSupportedException {
        //shutup checkstyle
        super.clone();
        return new RMIAsynchronousCacheReplicator(replicatePuts, replicateUpdates, replicateUpdatesViaCopy, replicateRemovals);
    }


}
