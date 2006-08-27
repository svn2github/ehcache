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
import java.util.List;

/**
 * Listens to {@link net.sf.ehcache.CacheManager} and {@link net.sf.ehcache.Cache} events and propagates those to
 * {@link CachePeer} peers of the Cache.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class RMISynchronousCacheReplicator implements CacheReplicator {

    private static final Log LOG = LogFactory.getLog(RMISynchronousCacheReplicator.class.getName());


    /**
     * The status of the replicator. Only replicates when <code>STATUS_ALIVE</code>
     */
    protected Status status;

    /**
     * Whether to replicate puts.
     */
    protected final boolean replicatePuts;

    /**
     * Whether to replicate updates.
     */
    protected final boolean replicateUpdates;

    /**
     * Whether an update (a put) should be by copy or by invalidation, (a remove).
     * <p/>
     * By copy is best when the entry is expensive to produce. By invalidation is best when
     * we are really trying to force other caches to sync back to a canonical source like a database.
     * An example of a latter usage would be a read/write cache being used in Hibernate.
     * <p/>
     * This setting only has effect if <code>#replicateUpdates</code> is true.
     */
    protected final boolean replicateUpdatesViaCopy;

    /**
     * Whether to replicate removes
     */
    protected final boolean replicateRemovals;

    /**
     * Constructor for internal and subclass use
     *
     * @param replicatePuts
     * @param replicateUpdates
     * @param replicateUpdatesViaCopy
     * @param replicateRemovals
     */
    public RMISynchronousCacheReplicator(
            boolean replicatePuts,
            boolean replicateUpdates,
            boolean replicateUpdatesViaCopy,
            boolean replicateRemovals) {
        this.replicatePuts = replicatePuts;
        this.replicateUpdates = replicateUpdates;
        this.replicateUpdatesViaCopy = replicateUpdatesViaCopy;
        this.replicateRemovals = replicateRemovals;
        status = Status.STATUS_ALIVE;
    }

    /**
     * Called immediately after an element has been put into the cache. The {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method
     * will block until this method returns.
     * <p/>
     * Implementers may wish to have access to the Element's fields, including value, so the element is provided.
     * Implementers should be careful not to modify the element. The effect of any modifications is undefined.
     *
     * @param cache   the cache emitting the notification
     * @param element the element which was just put into the cache.
     */
    public void notifyElementPut(final Ehcache cache, final Element element) throws CacheException {
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


        replicatePutNotification(cache, element);
    }

    /**
     * Does the actual RMI remote call
     *
     * @param element
     * @param cache
     * @throws RemoteCacheException if anything goes wrong with the remote call
     */
    private static void replicatePutNotification(Ehcache cache, Element element) throws RemoteCacheException {
        List cachePeers = listRemoteCachePeers(cache);
        for (int i = 0; i < cachePeers.size(); i++) {
            CachePeer cachePeer = (CachePeer) cachePeers.get(i);
            try {
                cachePeer.put(element);
            } catch (Throwable t) {
                throw new RemoteCacheException("Error doing put to remote peer. Message was: " + t.getMessage());
            }
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
    public void notifyElementUpdated(final Ehcache cache, final Element element) throws CacheException {
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

            replicatePutNotification(cache, element);
        } else {
            if (!element.isKeySerializable()) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Key " + element.getObjectKey() + " is not Serializable and cannot be replicated.");
                }
                return;
            }

            replicateRemovalNotification(cache, (Serializable) element.getObjectKey());
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
     *
     * @param cache   the cache emitting the notification
     * @param element the element just deleted, or a synthetic element with just the key set if
     *                no element was removed.param element just deleted
     */
    public void notifyElementRemoved(final Ehcache cache, final Element element) throws CacheException {
        if (notAlive()) {
            return;
        }

        if (!replicateRemovals) {
            return;
        }

        if (!element.isKeySerializable()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Key " + element.getObjectKey() + " is not Serializable and cannot be replicated.");
            }
            return;
        }

        replicateRemovalNotification(cache, (Serializable) element.getObjectKey());
    }

    /**
     * Does the actual RMI remote call
     *
     * @param key
     * @param cache
     * @throws RemoteCacheException if anything goes wrong with the remote call
     */
    private static void replicateRemovalNotification(Ehcache cache, Serializable key) throws RemoteCacheException {
        List cachePeers = listRemoteCachePeers(cache);
        for (int i = 0; i < cachePeers.size(); i++) {
            CachePeer cachePeer = (CachePeer) cachePeers.get(i);
            try {
                cachePeer.remove(key);
            } catch (Throwable e) {
                throw new RemoteCacheException("Error doing remove to remote peer. Message was: " + e.getMessage());
            }
        }
    }


    /**
     * {@inheritDoc}
     * <p/>
     * This implementation does not propagate expiries. It does not need to do anything because the element will
     * expire in the remote cache at the same time. If the remote peer is not configured the same way they should
     * not be in an cache cluster.
     */
    public final void notifyElementExpired(final Ehcache cache, final Element element) {
        /*do not propagate expiries. The element should expire in the remote cache at the same time, thus
          preseerving coherency.
          */
    }

    /**
     * Called immediately after an element is evicted from the cache. Evicted in this sense
     * means evicted from one store and not moved to another, so that it exists nowhere in the
     * local cache.
     * <p/>
     * In a sense the Element has been <i>removed</i> from the cache, but it is different,
     * thus the separate notification.
     * <p/>
     * This replicator does not propagate these events
     *
     * @param cache   the cache emitting the notification
     * @param element the element that has just been evicted
     */
    public void notifyElementEvicted(final Ehcache cache, final Element element) {
        /**
         * do not notify these
         */
    }


    /**
     * Called during {@link net.sf.ehcache.Ehcache#removeAll()} to indicate that the all
     * elements have been removed from the cache in a bulk operation. The usual
     * {@link #notifyElementRemoved(net.sf.ehcache.Ehcache,net.sf.ehcache.Element)}
     * is not called.
     * <p/>
     * This notification exists because clearing a cache is a special case. It is often
     * not practical to serially process notifications where potentially millions of elements
     * have been bulk deleted.
     *
     * @param cache the cache emitting the notification
     */
    public void notifyRemoveAll(final Ehcache cache) {
        if (notAlive()) {
            return;
        }

        if (!replicateRemovals) {
            return;
        }

        replicateRemoveAllNotification(cache);
    }

    private void replicateRemoveAllNotification(Ehcache cache) {
        List cachePeers = listRemoteCachePeers(cache);
        for (int i = 0; i < cachePeers.size(); i++) {
            CachePeer cachePeer = (CachePeer) cachePeers.get(i);
            try {
                cachePeer.removeAll();
            } catch (Throwable e) {
                throw new RemoteCacheException("Error doing removeAll to remote peer. Message was: " + e.getMessage());
            }
        }
    }

    /**
     * Package protected List of cache peers
     *
     * @param cache
     * @return a list of {@link CachePeer} peers for the given cache, excluding the local peer.
     */
    static List listRemoteCachePeers(Ehcache cache) {
        CacheManagerPeerProvider provider = cache.getCacheManager().getCachePeerProvider();
        return provider.listRemoteCachePeers(cache);
    }


    /**
     * @return whether update is through copy or invalidate
     */
    public final boolean isReplicateUpdatesViaCopy() {
        return replicateUpdatesViaCopy;
    }

    /**
     * Asserts that the replicator is active.
     *
     * @return true if the status is not STATUS_ALIVE
     */
    public final boolean notAlive() {
        return !alive();
    }

    /**
     * Checks that the replicator is is <code>STATUS_ALIVE</code>.
     */
    public final boolean alive() {
        if (status == null) {
            return false;
        } else {
            return (status.equals(Status.STATUS_ALIVE));
        }
    }

    /**
     * Give the replicator a chance to cleanup and free resources when no longer needed
     */
    public void dispose() {
        status = Status.STATUS_SHUTDOWN;
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
        return new RMISynchronousCacheReplicator(replicatePuts, replicateUpdates, replicateUpdatesViaCopy, replicateRemovals);
    }
}
