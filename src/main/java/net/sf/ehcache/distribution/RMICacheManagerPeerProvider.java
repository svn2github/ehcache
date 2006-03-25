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
import net.sf.ehcache.CacheManager;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A provider of Peer RMI addresses.
 *
 * @author Greg Luck
 * @version $Id: RMICacheManagerPeerProvider.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public class RMICacheManagerPeerProvider implements CacheManagerPeerProvider {

    private static final Log LOG = LogFactory.getLog(RMICacheManagerPeerProvider.class.getName());

    /**
     * Contains a RMI URLs of the form: "//" + hostName + ":" + port + "/" + cacheName;
     */
    protected Map peerUrls = Collections.synchronizedMap(new HashMap());
    private CacheManager cacheManager;

    /**
     * Empty constructor
     */
    public RMICacheManagerPeerProvider() {
        //nothing to do
    }


    /**
     * Constructor
     *
     * @param cacheManager
     */
    public RMICacheManagerPeerProvider(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }


    /**
     * {@inheritDoc}
     */
    public void init() {
        //nothing required for this implementation
    }

    /**
     * Register a new peer
     *
     * @param rmiUrl
     */
    public synchronized void registerPeer(String rmiUrl) {
        peerUrls.put(rmiUrl, new Date());
    }

    private String extractCacheName(String rmiUrl) {
        return rmiUrl.substring(rmiUrl.lastIndexOf('/') + 1);
    }

    /**
     * Unregisters a peer
     *
     * @param rmiUrl
     */
    public synchronized void unregisterPeer(String rmiUrl) {
        peerUrls.remove(rmiUrl);
    }

    /**
     * @return a list of {@link CachePeer} peers, excluding the local peer.
     */
    public synchronized List listRemoteCachePeers(Cache cache) throws CacheException {
        List remoteCachePeers = new ArrayList();
        List staleList = new ArrayList();
        for (Iterator iterator = peerUrls.keySet().iterator(); iterator.hasNext();) {
            String rmiUrl = (String) iterator.next();
            String rmiUrlCacheName = extractCacheName(rmiUrl);
            try {
                if (!rmiUrlCacheName.equals(cache.getName())) {
                    continue;
                }
                Date date = (Date) peerUrls.get(rmiUrl);
                if (!stale(date)) {
                    CachePeer cachePeer = lookupRemoteCachePeer(rmiUrl);
                    remoteCachePeers.add(cachePeer);
                } else {
                    if (LOG.isDebugEnabled()) {
                    LOG.debug("rmiUrl " + rmiUrl + " is stale. Either the remote peer is shutdown or the " +
                            "network connectivity has been interrupted. Will be removed from list of remote cache peers");
                    }
                    staleList.add(rmiUrl);
                }
            } catch (NotBoundException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No cache peer bound to URL at " + rmiUrl
                            + ". It must have disappeared since the last heartbeat");
                }
            } catch (Exception exception) {
                LOG.error(exception.getMessage(), exception);
                throw new CacheException("Unable to list remote cache peers. Error was " + exception.getMessage());
            }
        }

        //Remove any stale remote peers. Must be done here to avoid concurrent modification exception.
        for (int i = 0; i < staleList.size(); i++) {
            String rmiUrl = (String) staleList.get(i);
            peerUrls.remove(rmiUrl);
        }
        return remoteCachePeers;
    }

    /**
     * Whether the entry should be considered stale. This will depend on the type of RMICacheManagerPeerProvider.
     * <p/>
     * This method should be overridden for implementations that go stale based on date
     *
     * @param date the date the entry was created
     * @return true if stale
     */
    protected boolean stale(Date date) {
        return false;
    }


    /**
     * The use of one-time registry creation and Naming.rebind should mean we can create as many listeneres as we like.
     * They will simply replace the ones that were there.
     */
    public static CachePeer lookupRemoteCachePeer(String url) throws MalformedURLException, NotBoundException, RemoteException {
        return (CachePeer) Naming.lookup(url);
    }

    /**
     * Providers may be doing all sorts of exotic things and need to be able to clean up on dispose.
     *
     * @throws net.sf.ehcache.CacheException
     */
    public void dispose() throws CacheException {
        //nothing to do.
    }

    /**
     * The cacheManager this provider is bound to
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

}
