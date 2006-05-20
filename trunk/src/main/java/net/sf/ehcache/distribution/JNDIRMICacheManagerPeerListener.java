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
import net.sf.ehcache.Status;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.net.UnknownHostException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * A cache server which exposes available cache operations remotely through RMI.
 * Uses JNDI to bind the remote cache.
 *
 * @author Andy McNutt
 * @author Greg Luck
 * @version $Id$
 * @see RMICacheManagerPeerListener
 */
public class JNDIRMICacheManagerPeerListener extends RMICacheManagerPeerListener implements CacheManagerPeerListener {

    private static final Log LOG = LogFactory.getLog(JNDIRMICacheManagerPeerListener.class.getName());

    private String jndiProviderUrl;


    /**
     * @see RMICacheManagerPeerListener(String, Integer, CacheManager, Integer)
     */
    public JNDIRMICacheManagerPeerListener(String hostName, Integer port,
                                           CacheManager cacheManager, Integer socketTimeoutMillis) throws UnknownHostException {
        super(hostName, port, cacheManager, socketTimeoutMillis);
        jndiProviderUrl = "//localhost:" + port;
    }

    /**
     * {@inheritDoc}
     */
    public void init() throws CacheException {
        RMICachePeer rmiCachePeer = null;
        String peerName = null;
        int counter = 0;
        try {
            Context initialContext = getInitialContext();
            populateListOfRemoteCachePeers();

            for (Iterator iterator = cachePeers.values().iterator(); iterator.hasNext();) {
                rmiCachePeer = (RMICachePeer) iterator.next();
                peerName = rmiCachePeer.getName();
                LOG.debug("binding " + peerName);
                initialContext.rebind(peerName, rmiCachePeer);
                counter++;
            }
            LOG.debug(counter + " RMICachePeers bound in JNDI for RMI listener");
        } catch (Exception e) {
            throw new CacheException(
                    "Problem starting listener for RMICachePeer " + peerName + " .Error was " + e.getMessage(), e);
        }
    }



    /**
     * @return
     * @throws NamingException
     */
    private Context getInitialContext() throws NamingException {
        Hashtable hashTable = new Hashtable();
        String initialContextFactory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
        if (initialContextFactory != null && initialContextFactory.startsWith("net.sf.ehcache")) {

            // Put Context.PROVIDER_URL so unit tests can work
            hashTable.put(Context.PROVIDER_URL, jndiProviderUrl);
            return new InitialContext(hashTable);
        }
        return new InitialContext();
    }

    /**
     * Stop the listener. It
     * <ul>
     * <li>unexports Remote objects
     * <li>unbinds the objects from JNDI
     * </ul>
     */
    public void dispose() throws CacheException {
        try {
            int counter = 0;
            Context initialContext = getInitialContext();
            for (Iterator iterator = cachePeers.values().iterator(); iterator.hasNext();) {
                RMICachePeer rmiCachePeer = (RMICachePeer) iterator.next();
                UnicastRemoteObject.unexportObject(rmiCachePeer, false);
                initialContext.unbind(rmiCachePeer.getName());
            }
            LOG.debug(counter + " RMICachePeers unbound from JNDI in RMI listener");
            status = Status.STATUS_SHUTDOWN;
        } catch (Exception e) {
            throw new CacheException("Problem unbinding remote cache peers. Error was " + e.getMessage(), e);
        } finally {
            removeShutdownHook();
        }
    }
}
