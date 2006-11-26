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

    /**
     * Constructor with full arguments.
     *
     * @param hostName            may be null, in which case the hostName will be looked up. Machines with multiple
     *                            interfaces should specify this if they do not want it to be the default NIC.
     * @param port                a port in the range 1025 - 65536
     * @param cacheManager        the CacheManager this listener belongs to
     * @param socketTimeoutMillis TCP/IP Socket timeout when waiting on response
     *                            Constructor
     * @throws java.net.UnknownHostException
     * @see RMICacheManagerPeerListener
     */
    public JNDIRMICacheManagerPeerListener(String hostName, Integer port, CacheManager cacheManager, Integer socketTimeoutMillis)
            throws UnknownHostException {
        super(hostName, port, cacheManager, socketTimeoutMillis);
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

            synchronized (cachePeers) {
            for (Iterator iterator = cachePeers.values().iterator(); iterator.hasNext();) {
                rmiCachePeer = (RMICachePeer) iterator.next();
                peerName = rmiCachePeer.getName();
                LOG.debug("binding " + peerName);
                initialContext.rebind(peerName, rmiCachePeer);
                counter++;
            }
            }
            LOG.debug(counter + " RMICachePeers bound in JNDI for RMI listener");
        } catch (Exception e) {
            throw new CacheException(
                    "Problem starting listener for RMICachePeer " + peerName + " .Error was " + e.getMessage(), e);
        }
    }


    /**
     * Gets the initial context
     *
     * @return an initial context
     * @throws NamingException if JNDI goes wrong
     */
    private Context getInitialContext() throws NamingException {
        String initialContextFactory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);

        if (initialContextFactory != null && initialContextFactory.startsWith("net.sf.ehcache")) {
            // Put Context.PROVIDER_URL so unit tests can work
            Hashtable hashTable = new Hashtable();
            hashTable.put(Context.PROVIDER_URL, "//localhost:" + port);
            return new InitialContext(hashTable);
        }
        return new InitialContext();
    }

    /**
     * Disposes an individual RMICachePeer. This consists of:
     * <ol>
     * <li>Unbinding the peer from the naming service
     * <li>Unexporting the peer
     * </ol>
     *
     * @param rmiCachePeer  the cache peer to dispose of
     * @throws Exception thrown if something goes wrong
     */
    protected void disposeRMICachePeer(RMICachePeer rmiCachePeer) throws Exception {
        getInitialContext().unbind(rmiCachePeer.getName());
        UnicastRemoteObject.unexportObject(rmiCachePeer, false);
    }

}
