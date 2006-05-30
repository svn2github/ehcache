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
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * A provider of RMI CachePeers through JNDI lookup.
 * <p/>
 * The design allows for a peer server to go down.  When it is up again
 * the peer will be provided again.
 * <p/>
 * The JNDI Context and the CachePeers are cached locally.
 * When listRemoteCachePeers is called each CachePeer is tested for
 * staleness.  If it is stale, the peer is looked up again in JNDI.
 *
 * @author Andy McNutt
 * @author Greg Luck
 * @version $Id$
 */
public class JNDIManualRMICacheManagerPeerProvider implements CacheManagerPeerProvider {

    private static final Log LOG = LogFactory.getLog(JNDIRMICacheManagerPeerListener.class.getName());


    /**
     * Contains registered JNDI URLs as keys to their Context
     */
    protected Map peerUrls = new HashMap();

    /**
     * CachePeers keyed by jndiProviderUrl
     */
    protected Map cachePeers = new HashMap();

    /**
     * Prevent deadlock accessing peerUrls and cachePeers
     * with this lock object.
     */
    private final Object lock = new Object();

    private CacheManager cacheManager;

    private boolean isStashContexts = true;
    private boolean isStashRemoteCachePeers = true;


    /**
     * Constructor
     *
     * @param isStashRemoteCachePeers
     * @param isStashContexts
     */
    public JNDIManualRMICacheManagerPeerProvider(boolean isStashContexts, boolean isStashRemoteCachePeers) {
        this.isStashContexts = isStashContexts;
        this.isStashRemoteCachePeers = isStashRemoteCachePeers;
    }

    /**
     * Constructor
     *
     * @param cacheManager
     */
    public JNDIManualRMICacheManagerPeerProvider(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Notifies providers to initialise themselves.
     *
     * @throws CacheException
     */
    public void init() {
        //noop
    }

    /**
     * Register a new peer
     *
     * @param jndiProviderUrl
     */
    public void registerPeer(String jndiProviderUrl) {
        registerPeerToContext(jndiProviderUrl);
    }

    /**
     * Unregisters a peer
     *
     * @param jndiProviderUrl
     */
    public void unregisterPeer(String jndiProviderUrl) {
        synchronized (lock) {
            peerUrls.remove(jndiProviderUrl);
        }
    }

    /**
     * @return a list of {@link CachePeer} peers, excluding the local peer.
     */
    public List listRemoteCachePeers(Ehcache cache) throws CacheException {
        List remoteCachePeers = new ArrayList();
        List staleCachePeers = new ArrayList();
        String jndiProviderUrl = null;
        synchronized (lock) {
            for (Iterator iterator = peerUrls.keySet().iterator(); iterator.hasNext();) {
                jndiProviderUrl = (String) iterator.next();
                String providerUrlCacheName = extractCacheName(jndiProviderUrl);
                try {
                    if (!providerUrlCacheName.equals(cache.getName())) {
                        continue;
                    }
                    CachePeer cachePeer = lookupCachePeer(jndiProviderUrl);
                    remoteCachePeers.add(cachePeer);
                } catch (NamingException ne) {
                    LOG.debug(jndiProviderUrl + " " + ne.getMessage());
                    staleCachePeers.add(jndiProviderUrl);
                } catch (Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                    throw new CacheException(jndiProviderUrl + " Unable to list remote cache peers. Error was "
                            + ex.getMessage(), ex);
                }
            }
        }
        if (!staleCachePeers.isEmpty()) {

            // Do this after loop so don't modify peerUrls in it.
            unregisterStalePeers(staleCachePeers);
        }
        if (LOG.isDebugEnabled()) {
            try {
                LOG.debug("listRemoteCachePeers " + cache.getName() + " returning " + remoteCachePeers.size() + " "
                        + printCachePeers(remoteCachePeers));
            } catch (RemoteException e) {
                LOG.warn(e.getMessage(), e);
                LOG.debug("listRemoteCachePeers " + cache.getName() + " returning " + remoteCachePeers.size());
            }
        }
        return remoteCachePeers;
    }

    /**
     * Providers may be doing all sorts of exotic things and need to be able to clean up on dispose.
     *
     * @throws net.sf.ehcache.CacheException
     */
    public void dispose() throws CacheException {

        // Remove cached objects
        synchronized (lock) {
            peerUrls.clear();
            peerUrls = null;
            cachePeers.clear();
            cachePeers = null;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("dispose " + toString());
        }
    }

    /**
     * Time for a cluster to form. This varies considerably, depending on the implementation.
     *
     * @return the time in ms, for a cluster to form
     */
    public long getTimeForClusterToForm() {
        return 0;
    }

    /**
     * The cacheManager this provider is bound to
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * Register a new peer by looking it up in JNDI and storing
     * in a local cache the Context.
     *
     * @param jndiProviderUrl
     */
    private Context registerPeerToContext(String jndiProviderUrl) {
        String initialContextFactory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
        if (LOG.isDebugEnabled()) {
            LOG.debug("registerPeerToContext: " + jndiProviderUrl
                    + " " + extractProviderUrl(jndiProviderUrl)
                    + " with " + initialContextFactory);
        }
        Hashtable hashTable = new Hashtable(1);
        hashTable.put(Context.PROVIDER_URL, extractProviderUrl(jndiProviderUrl));
        Context initialContext = null;
        try {
            initialContext = new InitialContext(hashTable);
            registerPeerToContext(jndiProviderUrl, initialContext);

        } catch (NamingException e) {
            LOG.warn(jndiProviderUrl + " " + e.getMessage());
            registerPeerToContext(jndiProviderUrl, null);
        }
        return initialContext;
    }

    private void registerPeerToContext(String jndiProviderUrl, Context context) {
        synchronized (lock) {
            if (isStashContexts) {
                peerUrls.put(jndiProviderUrl, context);
            } else {
                peerUrls.put(jndiProviderUrl, null);
            }
        }
    }

    private static String extractCacheName(String jndiProviderUrl) {
        return jndiProviderUrl.substring(jndiProviderUrl.lastIndexOf('/') + 1);
    }

    private static String extractProviderUrl(String jndiProviderUrl) {
        return jndiProviderUrl.substring(0, jndiProviderUrl.lastIndexOf('/'));
    }

    private Context getContext(String jndiProviderUrl) {
        if (isStashContexts) {
            synchronized (lock) {
                return (Context) peerUrls.get(jndiProviderUrl);
            }
        }
        return null;
    }

    /**
     * Call this method after we have checked all the CachePeers for staleness which sets stalePeerUrls.
     * <p/>
     * This method sets to null the Context in peerUrls, and
     * sets to null the CachePeer in cachePeers for each jndiProviderUrl in staleCachePeers.
     *
     * @param staleCachePeers - List of stale jndiProviderUrls
     */
    private void unregisterStalePeers(List staleCachePeers) {
        for (Iterator iterator = staleCachePeers.iterator(); iterator.hasNext();) {
            String jndiProviderUrl = (String) iterator.next();
            registerPeerToContext(jndiProviderUrl, null);
            registerCachePeer(jndiProviderUrl, null);
            if (LOG.isDebugEnabled()) {
                LOG.debug("unregisterStalePeers " + jndiProviderUrl);
            }
        }
    }

    /**
     * Get the CachePeer from a local cache.  Test isStale.
     * If it is Stale, look it up in JNDI again and test isStale again.
     *
     * @param jndiProviderUrl
     * @return CachePeer
     * @throws NamingException when JNDI lookup fails or when the CachePeer is stale and cannot be reestablished.
     */
    private CachePeer lookupCachePeer(String jndiProviderUrl) throws NamingException {
        CachePeer cachePeer = getCachePeer(jndiProviderUrl);
        boolean isAlreadyLookedupRemoteCachePeer = false;

        // The last lookup and test isStale may have caused
        // cachePeer to be null for jndiProviderUrl.
        if (cachePeer == null) {
            cachePeer = lookupRemoteCachePeer(jndiProviderUrl);
            if (cachePeer == null) {
                String msg = "cachePeer null after lookup " + jndiProviderUrl;
                LOG.debug(msg);
                throw new NamingException(msg);
            }
            isAlreadyLookedupRemoteCachePeer = true;
        }
        cachePeer = getNonStaleCachePeer(jndiProviderUrl, cachePeer,
                isAlreadyLookedupRemoteCachePeer);
        registerCachePeer(jndiProviderUrl, cachePeer);
        return cachePeer;
    }

    /**
     * @param jndiProviderUrl
     * @param cachePeer       - may not be null
     * @param isAlreadyLookedupRemoteCachePeer
     *
     * @return a CachePeer that is not stale
     * @throws NamingException
     */
    private CachePeer getNonStaleCachePeer(final String jndiProviderUrl, final CachePeer cachePeer,
                                           final boolean isAlreadyLookedupRemoteCachePeer) throws NamingException {
        boolean isStale = isStale(cachePeer);
        CachePeer localCachePeer = null;
        if (isStale) {
            if (!isAlreadyLookedupRemoteCachePeer) {
                LOG.debug("CachePeer is stale, looking it up again " + jndiProviderUrl);

                // The cachePeer is stale.  Look it up again.
                localCachePeer = lookupRemoteCachePeer(jndiProviderUrl);
                if (!isStale(localCachePeer)) {
                    isStale = false;
                }
            }
        } else {
            localCachePeer = cachePeer;
        }

        if (isStale) {
            String msg = "After lookup CachePeer is stale " + jndiProviderUrl;
            LOG.info(msg);
            throw new NamingException(msg);
        }

        return localCachePeer;
    }

    private CachePeer getCachePeer(String jndiProviderUrl) {
        if (isStashRemoteCachePeers) {
            synchronized (lock) {
                return (CachePeer) cachePeers.get(jndiProviderUrl);
            }
        }
        return null;
    }

    private void registerCachePeer(String jndiProviderUrl, CachePeer cachePeer) {
        if (isStashRemoteCachePeers) {
            synchronized (lock) {
                cachePeers.put(jndiProviderUrl, cachePeer);
            }
        }
    }

    /**
     * The last lookup and test isStale may have caused context to be null for jndiProviderUrl.
     * @param jndiProviderUrl
     * @return
     * @throws NamingException
     */
    private CachePeer lookupRemoteCachePeer(String jndiProviderUrl)
            throws NamingException {
        Context context = getContext(jndiProviderUrl);

        if (context == null) {
            context = registerPeerToContext(jndiProviderUrl);
        }
        return (CachePeer) context.lookup(extractCacheName(jndiProviderUrl));
    }

    /**
     * Any remote method call that doesn't throw RemoteException indicates the cachePeer is not stale
     * @param cachePeer  the peer to check
     * @return true if the peer is contactable
     */
    private boolean isStale(CachePeer cachePeer) {
        try {
            cachePeer.getName();
        } catch (RemoteException re) {
            return true;
        }
        return false;
    }

    /**
     * @param cachePeers - List of CachePeers.  May not be null.
     * @return StringBuffer with URL of each CachePeer
     * @throws RemoteException
     */
    private StringBuffer printCachePeers(List cachePeers) throws RemoteException {
        Iterator iterator = cachePeers.iterator();
        StringBuffer sb = new StringBuffer();
        sb.append("CachePeers=[");
        while (iterator.hasNext()) {
            CachePeer cachePeer = (CachePeer) iterator.next();
            sb.append(" ").append(cachePeer.toString());
        }
        sb.append("]");
        return sb;
    }

    /**
     * Returns a string representation of the object. In general, the
     * <code>toString</code> method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p/>
     * The <code>toString</code> method for class <code>Object</code>
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `<code>@</code>', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     */
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(super.toString()).append(" cacheManager=")
                .append(cacheManager).append(" isStashContexts=")
                .append(isStashContexts).append(" isStashRemoteCachePeers=")
                .append(isStashRemoteCachePeers);
        synchronized (lock) {
            buff.append(" peerUrls=").append(peerUrls).append(" cachePeers=").append(cachePeers);
        }
        return buff.toString();
    }

}
