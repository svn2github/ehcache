/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CachePeer;
import net.sf.ehcache.distribution.CacheReplicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pierre Monestie (pmonestie[at]@gmail.com)
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 *          <p/> This implements CacheReplicator using JGroups as underlying
 *          replication mechanism The peer provider should be of type
 *          JGroupsCacheManagerPeerProvider It is assumed that the cachepeer is
 *          a JGroupManager
 */
public class JGroupsCacheReplicator implements CacheReplicator {
    /**
     * Teh default interval for async cache replication
     */
    public static final long DEFAULT_ASYNC_INTERVAL = 1000;

    private static final Logger LOG = LoggerFactory.getLogger(JGroupsCacheReplicator.class.getName());

    private long asynchronousReplicationInterval = DEFAULT_ASYNC_INTERVAL;

    /**
     * Whether or not to replicate puts
     */
    private boolean replicatePuts;

    /**
     * Whether or not to replicate updates
     */
    private boolean replicateUpdates;

    /**
     * Replicate update via copying, if false via deleting
     */
    private boolean replicateUpdatesViaCopy;

    /**
     * Whether or not to replicate remove events
     */
    private boolean replicateRemovals;

    /**
     * Weather or not to replicate asynchronously. If true a background thread
     * is run and updates are fired at a set interval
     */
    private boolean replicateAsync;

    private ReplicationThread replicationThread;

    private List replicationQueue = new LinkedList();

    private Status status;


    /**
     * Constructor called by factory
     *
     * @param replicatePuts
     * @param replicateUpdates
     * @param replicateUpdatesViaCopy
     * @param replicateRemovals
     * @param replicateAsync
     */
    public JGroupsCacheReplicator(boolean replicatePuts, boolean replicateUpdates, boolean replicateUpdatesViaCopy,
                                  boolean replicateRemovals, boolean replicateAsync) {
        super();

        this.replicatePuts = replicatePuts;
        this.replicateUpdates = replicateUpdates;
        this.replicateUpdatesViaCopy = replicateUpdatesViaCopy;
        this.replicateRemovals = replicateRemovals;
        this.replicateAsync = replicateAsync;

        if (replicateAsync) {
            replicationThread = new ReplicationThread();
            replicationThread.start();
        }
        status = Status.STATUS_ALIVE;
    }


    /**
     * {@inheritDoc}
     */
    public boolean alive() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReplicateUpdatesViaCopy() {
        return replicateUpdatesViaCopy;
    }

    /**
     * {@inheritDoc}
     */
    public boolean notAlive() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        status = Status.STATUS_SHUTDOWN;
        flushReplicationQueue();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementExpired(Ehcache cache, Element element) {

        // LOG.debugst("Sending out exp el:"+element);

    }

    /**
     * Used to send notification to the peer. If Async this method simply add
     * the element to the replication queue. If not async, searches for the
     * cachePeer and send the Message. That way the class handles both async and
     * sync replication Sending is delegated to the peer (of type JGroupManager)
     *
     * @param cache
     * @param e
     */
    protected void sendNotification(Ehcache cache, JGroupEventMessage e) {

        if (replicateAsync) {
            addMessageToQueue(e);
            return;
        }
        CacheManagerPeerProvider provider = cache.getCacheManager().getCacheManagerPeerProvider("JGroups");
        List l = provider.listRemoteCachePeers(cache);
        ArrayList a = new ArrayList();

        a.add(e);

        for (int i = 0; i < l.size(); i++) {
            CachePeer peer = (CachePeer) l.get(i);
            try {
                peer.send(a);
            } catch (RemoteException e1) {
                // e1.printStackTrace();
            }
            // peer.
        }

    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
        if (notAlive()) {
            return;
        }

        if (replicatePuts) {
            // if (log.isTraceEnabled())
            // LOG.debugst("Sending out add/upd el:" + element);
            replicatePutNotification(cache, element);
        }

    }

    private void replicatePutNotification(Ehcache cache, Element element) {
        if (!element.isKeySerializable()) {
            LOG.warn("Key {} is not Serializable and cannot be replicated.", element.getObjectKey());
            return;
        }
        if (!element.isSerializable()) {
            LOG.warn("Object with key {} is not Serializable and cannot be updated via copy", element.getObjectKey());
            return;
        }
        JGroupEventMessage e = new JGroupEventMessage(JGroupEventMessage.PUT, (Serializable) element.getObjectKey(), element,
                cache, cache.getName());

        sendNotification(cache, e);
    }

    private void replicateRemoveNotification(Ehcache cache, Element element) {
        if (!element.isKeySerializable()) {
            LOG.warn("Key {} is not Serializable and cannot be replicated.", element.getObjectKey());
            return;
        }
        JGroupEventMessage e = new JGroupEventMessage(JGroupEventMessage.REMOVE, (Serializable) element.getObjectKey(), null,
                cache, cache.getName());

        sendNotification(cache, e);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
        if (notAlive()) {
            return;
        }
        if (replicateRemovals) {
            replicateRemoveNotification(cache, element);

        }

    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
        if (notAlive()) {
            return;
        }
        if (!replicateUpdates) {
            return;
        }

        if (isReplicateUpdatesViaCopy()) {
            replicatePutNotification(cache, element);
        } else {
            replicateRemoveNotification(cache, element);
        }

    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementEvicted(Ehcache cache, Element element) {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    public void notifyRemoveAll(Ehcache cache) {
        if (replicateRemovals) {
            LOG.debug("Remove all elements called");
            JGroupEventMessage e = new JGroupEventMessage(JGroupEventMessage.REMOVE_ALL, null, null, cache, cache.getName());
            sendNotification(cache, e);
        }

    }

    /**
     * Package protected List of cache peers
     *
     * @param cache
     * @return a list of {@link CachePeer} peers for the given cache, excluding
     *         the local peer.
     */
    static List listRemoteCachePeers(Ehcache cache) {
        CacheManagerPeerProvider provider = cache.getCacheManager().getCacheManagerPeerProvider("JGroups");
        return provider.listRemoteCachePeers(cache);
    }

    /**
     * {@inheritDoc}
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * The replication thread
     *
     * @author pierrem
     */
    private final class ReplicationThread extends Thread {
        public ReplicationThread() {
            super("Replication Thread");
            setDaemon(true);
            setPriority(Thread.NORM_PRIORITY);
        }

        /**
         * RemoteDebugger thread method.
         */
        public final void run() {
            replicationThreadMain();
        }
    }

    private void replicationThreadMain() {
        while (true) {
            // Wait for elements in the replicationQueue
            while (alive() && replicationQueue != null && replicationQueue.size() == 0) {
                try {

                    Thread.sleep(asynchronousReplicationInterval);
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
                LOG.warn("Exception on flushing of replication queue: {}. Continuing...", e.getMessage(), e);
            }
        }
    }

    private void addMessageToQueue(JGroupEventMessage msg) {
        synchronized (replicationQueue) {
            replicationQueue.add(msg);
        }
    }

    /**
     * Gets called once per {@link #asynchronousReplicationInterval}. <p/>
     * Sends accumulated messages in bulk to each peer. i.e. if ther are 100
     * messages and 1 peer, 1 RMI invocation results, not 100. Also, if a peer
     * is unavailable this is discovered in only 1 try. <p/> Makes a copy of the
     * queue so as not to hold up the enqueue operations. <p/> Any exceptions
     * are caught so that the replication thread does not die, and because
     * errors are expected, due to peers becoming unavailable. <p/> This method
     * issues warnings for problems that can be fixed with configuration
     * changes.
     */
    private void flushReplicationQueue() {
        List resolvedEventMessages;
        Ehcache cache;
        synchronized (replicationQueue) {
            if (replicationQueue.size() == 0) {
                return;
            }
            resolvedEventMessages = extractAndResolveEventMessages(replicationQueue);
            cache = ((JGroupEventMessage) replicationQueue.get(0)).getCache();
            replicationQueue.clear();
        }

        List cachePeers = listRemoteCachePeers(cache);

        for (int j = 0; j < cachePeers.size(); j++) {
            CachePeer cachePeer = (CachePeer) cachePeers.get(j);
            try {
                cachePeer.send(resolvedEventMessages);
            } catch (UnmarshalException e) {
                String message = e.getMessage();
                if (message.indexOf("Read time out") != 0) {
                    LOG.warn("Unable to send message to remote peer due to socket read timeout. Consider increasing"
                            + " the socketTimeoutMillis setting in the cacheManagerPeerListenerFactory. Message was: {}",
                            e.getMessage());
                } else {
                    LOG.debug("Unable to send message to remote peer.  Message was: {}", e.getMessage());
                }
            } catch (Throwable t) {
                LOG.warn("Unable to send message to remote peer.  Message was: {}", t.getMessage(), t);
            }
        }

    }

    /**
     * Extracts CacheEventMessages and attempts to get a hard reference to the
     * underlying EventMessage <p/> If an EventMessage has been invalidated due
     * to SoftReference collection of the Element, it is not propagated. This
     * only affects puts and updates via copy.
     *
     * @param replicationQueueCopy
     * @return a list of EventMessages which were able to be resolved
     */
    private static List extractAndResolveEventMessages(List replicationQueueCopy) {
        List list = new ArrayList();
        for (int i = 0; i < replicationQueueCopy.size(); i++) {
            JGroupEventMessage eventMessage = (JGroupEventMessage) replicationQueueCopy.get(i);
            if (eventMessage != null && eventMessage.isValid()) {
                list.add(eventMessage);
            } else {
                LOG.error("Collected soft ref");
            }
        }
        return list;
    }

    /**
     * Get the time interval is ms between asynchronous replication
     *
     * @return the interval
     */
    public long getAsynchronousReplicationInterval() {
        return asynchronousReplicationInterval;
    }

    /**
     * Set the time inteval for asynchronous replication
     *
     * @param asynchronousReplicationInterval
     *         the interval between replication
     */
    public void setAsynchronousReplicationInterval(long asynchronousReplicationInterval) {
        this.asynchronousReplicationInterval = asynchronousReplicationInterval;
    }

}
