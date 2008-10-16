/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

package net.sf.ehcache.distribution.jms;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CachePeer;
import net.sf.ehcache.distribution.CacheReplicator;
import net.sf.ehcache.distribution.EventMessage;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author benoit.perroud@elca.ch
 * @author Greg Luck
 */
public class JMSCacheReplicator implements CacheReplicator {

    /**
     * The default replication interval
     */
    public static final long DEFAULT_ASYNC_INTERVAL = 1000;

    private static final Logger LOG = Logger.getLogger(JMSCacheReplicator.class.getName());

    private long asynchronousReplicationInterval = DEFAULT_ASYNC_INTERVAL;

    private boolean replicatePuts;
    private boolean replicateUpdates;
    private boolean replicateUpdatesViaCopy;
    private boolean replicateRemovals;
    private boolean replicateAsync;

    private List<AsyncJMSEventMessage> replicationQueue;

    private Status status;

    /**
     * Constructs a replicator
     * @param replicatePuts whether to replicate puts
     * @param replicateUpdates whether to replicate puts
     * @param replicateUpdatesViaCopy whether to replicate updates via copy or by invalidation
     * @param replicateRemovals whether to replicate removals
     * @param replicateAsync whether to replicate asynchronously
     * @param asynchronousReplicationInterval interval for asynchronous(batch) replicates. The default is 1000ms.
     *
     */
    public JMSCacheReplicator(boolean replicatePuts, boolean replicateUpdates,
                              boolean replicateUpdatesViaCopy, boolean replicateRemovals,
                              boolean replicateAsync, long asynchronousReplicationInterval) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("JMSCacheReplicator constructor ( replicatePuts = "
                    + replicatePuts + ", replicateUpdates = " + replicateUpdates + ", " +
                    "replicateUpdatesViaCopy = " + replicateUpdatesViaCopy + ", replicateRemovals = "
                    + replicateRemovals + ", replicateAsync = " + replicateAsync + " ) called");
        }

        replicationQueue = new LinkedList<AsyncJMSEventMessage>();

        this.replicatePuts = replicatePuts;
        this.replicateUpdates = replicateUpdates;

        this.replicateUpdatesViaCopy = replicateUpdatesViaCopy;
        this.replicateRemovals = replicateRemovals;
        this.replicateAsync = replicateAsync;
        this.asynchronousReplicationInterval = asynchronousReplicationInterval;

        if (replicateAsync) {
            JMSReplicationThread replicationThread = new JMSReplicationThread();
            replicationThread.start();
        }

        status = Status.STATUS_ALIVE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean alive() {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("alive ( ) called ");
        }

        return status == Status.STATUS_ALIVE;
    }

    /**
     * Returns whether update is through copy or invalidate
     *
     * @return true if update is via copy, else false if invalidate
     */
    public boolean isReplicateUpdatesViaCopy() {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("isReplicateUpdatesViaCopy ( ) called ");
        }

        return replicateUpdatesViaCopy;
    }

    /**
     * Returns whether the replicator is not active.
     *
     * @return true if the status is not STATUS_ALIVE
     */
    public boolean notAlive() {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("notAlive ( ) called ");
        }

        return !alive();
    }

    /**
     * @return The asynchronous replication interval, in ms
     */
    public long getAsynchronousReplicationInterval() {
        return asynchronousReplicationInterval;
    }

    /**
     * Give the listener a chance to cleanup and free resources when no longer needed
     */
    public void dispose() {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("dispose ( ) called ");
        }

        status = Status.STATUS_SHUTDOWN;

        if (replicateAsync) {
            flushReplicationQueue();
        }

    }

    /**
     * Called immediately after an element is evicted from the cache. Evicted in this sense
     * means evicted from one store and not moved to another, so that it exists nowhere in the
     * local cache.
     * <p/>
     * In a sense the Element has been <i>removed</i> from the cache, but it is different,
     * thus the separate notification.
     * <p/>
     * This message is not replicated by this replicator.
     *
     * @param cache   the cache emitting the notification
     * @param element the element that has just been evicted
     */
    public void notifyElementEvicted(Ehcache cache, Element element) {
        //noop
    }

    /**
     * Called immediately after an element is <i>found</i> to be expired. The
     * {@link net.sf.ehcache.Cache#remove(Object)} method will block until this method returns.
     * <p/>
     * As the {@link Element} has been expired, only what was the key of the element is known.
     * <p/>
     * Elements are checked for expiry in ehcache at the following times:
     * <ul>
     * <li>When a get request is made
     * <li>When an element is spooled to the diskStore in accordance with a MemoryStore
     * eviction policy
     * <li>In the DiskStore when the expiry thread runs, which by default is
     * {@link net.sf.ehcache.Cache#DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS}
     * </ul>
     * If an element is found to be expired, it is deleted and this method is notified.
     * <p/>
     * This message is not replicated by this replicator.
     *
     * @param cache   the cache emitting the notification
     * @param element the element that has just expired
     *                <p/>
     *                Deadlock Warning: expiry will often come from the <code>DiskStore</code>
     *                expiry thread. It holds a lock to the DiskStorea the time the
     *                notification is sent. If the implementation of this method calls into a
     *                synchronized <code>Cache</code> method and that subsequently calls into
     *                DiskStore a deadlock will result. Accordingly implementers of this method
     *                should not call back into Cache.
     */
    public void notifyElementExpired(Ehcache cache, Element element) {
        //noop
    }

    /**
     * Called immediately after an element has been put into the cache. The
     * {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method
     * will block until this method returns.
     * <p/>
     * Implementers may wish to have access to the Element's fields, including value, so the
     * element is provided. Implementers should be careful not to modify the element. The
     * effect of any modifications is undefined.
     *
     * @param cache   the cache emitting the notification
     * @param element the element which was just put into the cache.
     */
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
        if (notAlive()) {
            return;
        }

        if (!replicatePuts) {
            return;
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("notifyElementPut ( cache = " + cache + ", element = " + element + ") called ");
        }

        if (!element.isKeySerializable()) {
            LOG.warning("Key " + element.getObjectKey() + " is not Serializable and cannot be replicated.");
            return;
        }
        if (!element.isSerializable()) {
            LOG.warning("Object with key " + element.getObjectKey() + " is not Serializable and cannot be replicated");
            return;
        }
        replicatePut(cache, element);


    }


    /**
     * Called immediately after an element has been put into the cache and the element already
     * existed in the cache. This is thus an update.
     * <p/>
     * The {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method
     * will block until this method returns.
     * <p/>
     * Implementers may wish to have access to the Element's fields, including value, so the
     * element is provided. Implementers should be careful not to modify the element. The
     * effect of any modifications is undefined.
     *
     * @param cache   the cache emitting the notification
     * @param element the element which was just put into the cache.
     */
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {

        if (notAlive()) {
            return;
        }

        if (!replicateUpdates) {
            return;
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("notifyElementUpdated ( cache = " + cache + ", element = " + element + ") called ");
        }

        if (!element.isKeySerializable()) {
            LOG.warning("Key " + element.getObjectKey() + " is not Serializable and cannot be replicated.");
            return;
        }
        if (!element.isSerializable()) {
            LOG.warning("Object with key " + element.getObjectKey() + " is not Serializable and cannot be replicated");
            return;
        }

        if (replicateUpdatesViaCopy) {
            replicatePut(cache, element);
        } else {
            replicateRemoval(cache, element);
        }

    }


    private void replicatePut(Ehcache cache, Element element) {
        JMSEventMessage message = new JMSEventMessage(Action.PUT, element.getKey(), element, cache.getName(), null);

        sendNotification(cache, message);
    }

    /**
     * Called immediately after an attempt to remove an element. The remove method will block until
     * this method returns.
     * <p/>
     * This notification is received regardless of whether the cache had an element matching
     * the removal key or not. If an element was removed, the element is passed to this method,
     * otherwise a synthetic element, with only the key set is passed in.
     * <p/>
     * This notification is not called for the following special cases:
     * <ol>
     * <li>removeAll was called. See {@link #notifyRemoveAll(net.sf.ehcache.Ehcache)}
     * <li>An element was evicted from the cache.
     * See {@link #notifyElementEvicted(net.sf.ehcache.Ehcache, net.sf.ehcache.Element)}
     * </ol>
     *
     * @param cache   the cache emitting the notification
     * @param element the element just deleted, or a synthetic element with just the key set if
     *                no element was removed.
     */
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("notifyElementRemoved ( cache = " + cache + ", element = " + element + ")");
        }

        if (notAlive()) {
            return;
        }

        if (!replicateRemovals) {
            return;
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("notifyElementRemoved ( cache = " + cache + ", element = " + element + ")");
        }

        if (!element.isKeySerializable()) {
            LOG.warning("Key " + element.getObjectKey() + " is not Serializable and cannot be replicated.");
            return;
        }
        replicateRemoval(cache, element);


    }

    private void replicateRemoval(Ehcache cache, Element element) {
        //uniquely identifies the originator to ensure that a message does not update the originator
        JMSEventMessage message = new JMSEventMessage(Action.REMOVE, element.getKey(), null, cache.getName(),
                null);

        sendNotification(cache, message);
    }


    /**
     * Called during {@link net.sf.ehcache.Ehcache#removeAll()} to indicate that the all
     * elements have been removed from the cache in a bulk operation. The usual
     * {@link #notifyElementRemoved(net.sf.ehcache.Ehcache, net.sf.ehcache.Element)}
     * is not called.
     * <p/>
     * This notification exists because clearing a cache is a special case. It is often
     * not practical to serially process notifications where potentially millions of elements
     * have been bulk deleted.
     *
     * @param cache the cache emitting the notification
     */
    public void notifyRemoveAll(Ehcache cache) {

        if (notAlive()) {
            return;
        }

        if (!replicateRemovals) {
            return;
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("notifyRemoveAll ( cache = " + cache + ") ");
        }

        JMSEventMessage message = new JMSEventMessage(Action.REMOVE_ALL, null, null, cache.getName(), null);

        sendNotification(cache, message);
    }

    /**
     * Creates a clone of this listener. This method will only be called by ehcache before a
     * cache is initialized.
     * <p/>
     * This may not be possible for listeners after they have been initialized. Implementations
     * should throw CloneNotSupportedException if they do not support clone.
     *
     * @return a clone
     * @throws CloneNotSupportedException if the listener could not be cloned.
     */
    public Object clone() throws CloneNotSupportedException {
        return new JMSCacheReplicator(replicatePuts, replicateUpdates,
                replicateUpdatesViaCopy, replicateRemovals, replicateAsync, asynchronousReplicationInterval);
    }

    /**
     * Sends the message
     *
     * @param cache
     * @param message
     */
    protected void sendNotification(Ehcache cache, JMSEventMessage message) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("sendNotification ( " + message.toString() + " )");
        }

        if (replicateAsync) {
            addMessageToQueue(cache, message);
            return;
        }

        List<EventMessage> messages = new ArrayList<EventMessage>();
        messages.add(message);

        //Is only ever one for JMS in the current implementation
        for (CachePeer peer : listRemoteCachePeers(cache)) {
            try {
                peer.send(messages);
            } catch (RemoteException e) {
                throw new CacheException(e);
            }
        }
    }

    /**
     * @param cache the cache we are getting peers for
     * @return the list of cache peers
     */
    protected static List<CachePeer> listRemoteCachePeers(Ehcache cache) {
        CacheManagerPeerProvider provider = cache.getCacheManager().getCachePeerProvider();
        return provider.listRemoteCachePeers(cache);
    }

    /**
     *
     */
    private final class JMSReplicationThread extends Thread {

        /**
         *
         */
        public JMSReplicationThread() {
            super("JMS Replication Thread");
            setDaemon(true);
            int replicationThreadPriority = Thread.NORM_PRIORITY;
            setPriority(replicationThreadPriority);
        }

        /**
         *
         */
        public final void run() {
            replicationThreadMain();
        }
    }

    /**
     * Used to hold the JMSEventMessage and the cache the message belongs to
     */
    protected final static class AsyncJMSEventMessage {
        private Ehcache cache;
        private JMSEventMessage message;

        /**
         * @param cache -
         * @param message - 
         */
        public AsyncJMSEventMessage(Ehcache cache, JMSEventMessage message) {
            this.cache = cache;
            this.message = message;
        }

        /**
         * @return the cache this replicator is replicating for.
         */
        public Ehcache getCache() {
            return cache;
        }

        /**
         * @return the message
         */
        public JMSEventMessage getMessage() {
            return message;
        }
    }

    private void replicationThreadMain() {
        while (true) {
            // Wait for elements in the replicationQueue
            while (alive() && replicationQueue != null
                    && replicationQueue.size() == 0) {
                try {
                    Thread.sleep(getAsynchronousReplicationInterval());
                } catch (InterruptedException e) {
                    LOG.fine("Spool Thread interrupted.");
                    return;
                }
            }
            if (notAlive()) {
                return;
            }
            try {
                flushReplicationQueue();
            } catch (Throwable e) {
                LOG.log(Level.WARNING, "Exception on flushing of replication queue: "
                        + e.getMessage() + ". Continuing...", e);
            }
        }
    }

    private void addMessageToQueue(Ehcache cache, JMSEventMessage message) {
        synchronized (replicationQueue) {
            replicationQueue.add(new AsyncJMSEventMessage(cache, message));
        }
    }

    private void flushReplicationQueue() {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("flushReplicationQueue ( ) called ");
        }

        List<AsyncJMSEventMessage> replicationQueueCopy;

        synchronized (replicationQueue) {
            if (replicationQueue.size() == 0) {
                return;
            }
            //free up write queue
            replicationQueueCopy = new ArrayList<AsyncJMSEventMessage>(replicationQueue);
            replicationQueue.clear();
        }


        List<EventMessage> messages = new ArrayList<EventMessage>(1);

        for (AsyncJMSEventMessage message : replicationQueueCopy) {
            Ehcache cache = message.getCache();
            List<CachePeer> cachePeers = listRemoteCachePeers(cache);
            messages.add(message.getMessage());
            for (CachePeer peer : cachePeers) {
                try {
                    peer.send(messages);
                } catch (RemoteException e) {
                    LOG.warning("Unable to send message to remote peer. Message was: " + e.getMessage() + " continuing to send" +
                            "remaining messages.");
                }
            }
            messages.clear();
        }
    }
}
