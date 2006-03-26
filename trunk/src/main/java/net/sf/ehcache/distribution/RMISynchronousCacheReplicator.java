/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 - 2004 Greg Luck.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by Greg Luck
 *       (http://sourceforge.net/users/gregluck) and contributors.
 *       See http://sourceforge.net/project/memberlist.php?group_id=93232
 *       for a list of contributors"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "EHCache" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For written
 *    permission, please contact Greg Luck (gregluck at users.sourceforge.net).
 *
 * 5. Products derived from this software may not be called "EHCache"
 *    nor may "EHCache" appear in their names without prior written
 *    permission of Greg Luck.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GREG LUCK OR OTHER
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by contributors
 * individuals on behalf of the EHCache project.  For more
 * information on EHCache, please see <http://ehcache.sourceforge.net/>.
 *
 */
package net.sf.ehcache.distribution;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;

import java.io.Serializable;
import java.util.List;

/**
 * Listens to {@link net.sf.ehcache.CacheManager} and {@link net.sf.ehcache.Cache} events and propagates those to
 * {@link CachePeer} peers of the Cache.
 *
 * @author Greg Luck
 * @version $Id: RMISynchronousCacheReplicator.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public class RMISynchronousCacheReplicator implements CacheReplicator {

    /**
     * The status of the replicator. Only replicates when <code>STATUS_ALIVE</code>
     */
    protected Status status;

    /**
     * Whether to replicate puts.
     */
    protected boolean replicatePuts;

    /**
     * Whether to replicate updates.
     */
    protected boolean replicateUpdates;

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
    protected boolean replicateRemovals;

    /**
     * Constructor for internal and subclass use
     *
     * @param replicatePuts
     * @param replicateUpdates
     * @param replicateUpdatesViaCopy
     * @param replicateRemovals
     */
    protected RMISynchronousCacheReplicator(
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
    public void notifyElementPut(final Cache cache, final Element element) throws CacheException {
        if (notAlive()) {
            return;
        }
        if (replicatePuts) {
            replicatePutNotification(cache, element);
        }
    }

    /**
     * Does the actual RMI remote call
     *
     * @param element
     * @throws RemoteCacheException if anything goes wrong with the remote call
     */
    protected static void replicatePutNotification(Cache cache, Element element) throws RemoteCacheException {
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
    public void notifyElementUpdated(final Cache cache, final Element element) throws CacheException {
        if (notAlive()) {
            return;
        }
        if (replicateUpdates) {
            if (replicateUpdatesViaCopy) {
                replicatePutNotification(cache, element);
            } else {
                replicateRemovalNotification(cache, element.getKey());
            }
        }
    }

    /**
     * Called immediately after an element has been removed. The remove method will block until
     * this method returns.
     * <p/>
     * Ehcache does not check for
     * <p/>
     * As the {@link net.sf.ehcache.Element} has been removed, only what was the key of the element is known.
     * <p/>
     *
     * @param cache   the cache emitting the notification
     * @param element just deleted
     */
    public void notifyElementRemoved(final Cache cache, final Element element) throws CacheException {
        if (notAlive()) {
            return;
        }
        if (replicateRemovals) {
            replicateRemovalNotification(cache, element.getKey());
        }
    }

    /**
     * Does the actual RMI remote call
     *
     * @param key
     * @throws RemoteCacheException if anything goes wrong with the remote call
     */
    protected static void replicateRemovalNotification(Cache cache, Serializable key) throws RemoteCacheException {
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

    private static List listRemoteCachePeers(Cache cache) {
        CacheManagerPeerProvider provider = cache.getCacheManager().getCachePeerProvider();
        return provider.listRemoteCachePeers(cache);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation does not propagate expiries. It does not need to do anything because the element will
     * expire in the remote cache at the same time. If the remote peer is not configured the same way they should
     * not be in an cache cluster.
     */
    public final void notifyElementExpired(final Cache cache, final Element element) {
        /*do not propagate expiries. The element should expire in the remote cache at the same time, thus
          preseerving coherency.
          */
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
        return !status.equals(Status.STATUS_ALIVE);
    }

    /**
     * Checks that the replicator is is <code>STATUS_ALIVE</code>.
     */
    public final boolean alive() {
        return (status.equals(Status.STATUS_ALIVE));
    }

    /**
     * Give the replicator a chance to cleanup and free resources when no longer needed
     */
    public void dispose() {
        status = Status.STATUS_SHUTDOWN;
    }
}
