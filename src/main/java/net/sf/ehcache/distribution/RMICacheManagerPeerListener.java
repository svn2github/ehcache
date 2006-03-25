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
import net.sf.ehcache.event.CacheEventListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A cache server which exposes available cache operations remotely through RMI.
 * <p/>
 * It acts as a Decorator to a Cache. It holds an instance of cache, which is a local cache it talks to.
 * <p/>
 * This class could specify a security manager with code like:
 * <pre>
 * if (System.getSecurityManager() == null) {
 *     System.setSecurityManager(new RMISecurityManager());
 * }
 * </pre>
 * Doing so would require the addition of <code>grant</code> statements in the <code>java.policy</code> file.
 * <p/>
 * Per the JDK documentation: "If no security manager is specified no class loading, by RMI clients or servers, is allowed,
 * aside from what can be found in the local CLASSPATH." The classpath of each instance of this class should have
 * all required classes to enable distribution, so no remote classloading is required or desirable. Accordingly,
 * no security manager is set and there are no special JVM configuration requirements.
 * <p/>
 *
 * @author Greg Luck
 * @version $Id: RMICacheManagerPeerListener.java,v 1.2 2006/03/12 02:03:24 gregluck Exp $
 */
public class RMICacheManagerPeerListener implements CacheManagerPeerListener {

    private static final Log LOG = LogFactory.getLog(RMICacheManagerPeerListener.class.getName());
    private static final int MINIMUM_SENSIBLE_TIMEOUT = 200;

    private Registry registry;

    private String hostName;
    private Integer port;
    private CacheManager cacheManager;
    private Integer socketTimeoutMillis;

    private List cachePeers = new ArrayList();

    /**
     * Constructor with full arguements
     *
     * @param hostname            may be null, in which case the hostName will be looked up. Machines with multiple
     *                            interfaces should specify this if they do not want it to be the default NIC.
     * @param port                a port in the range 1025 - 65536
     * @param cacheManager        the CacheManager this listener belongs to
     * @param socketTimeoutMillis TCP/IP Socket timeout when waiting on response
     */
    public RMICacheManagerPeerListener(String hostname, Integer port, CacheManager cacheManager,
                                       Integer socketTimeoutMillis) throws UnknownHostException {
        if (hostname != null) {
            this.hostName = hostname;
        } else {
            this.hostName = calculateHostAddress();
        }
        if (port == null) {
            throw new IllegalArgumentException("port must be specified in the range 1025 - 65536");
        } else {
            this.port = port;
        }
        this.cacheManager = cacheManager;
        if (socketTimeoutMillis == null || socketTimeoutMillis.intValue() < MINIMUM_SENSIBLE_TIMEOUT) {
            throw new IllegalArgumentException("socketTimoutMillis must be a reasonable value greater than 200ms");
        }
        this.socketTimeoutMillis = socketTimeoutMillis;

    }


    private String calculateHostAddress() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }


    /**
     * {@inheritDoc}
     */
    public void init() throws CacheException {
        RMICachePeer rmiCachePeer = null;
        try {
            startRegistry();
            populateListOfRemoteCachePeers();
            for (int i = 0; i < cachePeers.size(); i++) {
                rmiCachePeer = (RMICachePeer) cachePeers.get(i);
                Naming.rebind(rmiCachePeer.getUrl(), rmiCachePeer);
            }
            LOG.debug("Server bound in registry");
        } catch (Exception e) {
            throw new CacheException("Problem starting listener for RMICachePeer "
                    + rmiCachePeer.getUrl() + ".Error was " + e.getMessage());
        }
    }

    /**
     * Returns a list of bound objects.
     * <p/>
     * This should match the list of cachePeers i.e. they should always be bound
     * @return a list of String representations of <code>RMICachePeer</code> objects
     */
    String[] listBoundRMICachePeers() throws CacheException {
        try {
            return registry.list();
        } catch (RemoteException e) {
            throw new CacheException("Unable to list cache peers " + e.getMessage());
        }
    }

    /**
     * Returns a reference to the remote object.
     * @param name the name of the cache e.g. <code>sampleCache1</code>
     */
    Remote lookupPeer(String name) throws CacheException {
        try {
            return (Remote) registry.lookup(name);
        } catch (Exception e) {
            throw new CacheException("Unable to lookup peer for replicated cache " + name + " "
                    + e.getMessage());
        }
    }

    /**
     * Should be called on init because this is one of the last things that should happen on CacheManager
     * startup
     */
    private void populateListOfRemoteCachePeers() throws RemoteException {
        String[] names = cacheManager.getCacheNames();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            Cache cache = cacheManager.getCache(name);
            if (isDistributed(cache)) {
                RMICachePeer peer = new RMICachePeer(cache, hostName, port, socketTimeoutMillis);
                cachePeers.add(peer);
            }
        }

    }

    /**
     * Determine if the given cache is distributed.
     *
     * @param cache the cache to check
     * @return true if a <code>CacheReplicator</code> is found in the listeners
     */
    private boolean isDistributed(Cache cache) {
        Set listeners = cache.getCacheEventNotificationService().getCacheEventListeners();
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
            if (cacheEventListener instanceof CacheReplicator) {
                return true;
            }
        }
        return false;
    }

    /**
     * Start the rmiregistry
     * <p/>
     * The alternative is to use the <code>rmiregistry</code> binary, in which case:
     * <ol/>
     * <li>rmiregistry running
     * <li>-Djava.rmi.server.codebase="file:///Users/gluck/work/ehcache/build/classes/ file:///Users/gluck/work/ehcache/lib/commons-logging-1.0.4.jar"
     * </ol>
     * There appears to be no way to stop an rmiregistry. We check to see if one if already "there"
     * before we create a new one.
     *
     * @throws RemoteException
     */
    private void startRegistry() throws RemoteException {
        try {
            registry = LocateRegistry.getRegistry(port.intValue());
            try {
                registry.list();
            } catch (RemoteException e) {
                //may not be created. Let's create it.
                registry = LocateRegistry.createRegistry(port.intValue());
            }
        } catch (ExportException exception) {
            LOG.fatal("Exception starting RMI registry. Error was " + exception.getMessage(), exception);
        }
    }

    /**
     * Stop the listener. It
     * <ul>
     * <li>unexports Remote objects
     * <li>unbinds the objects from the registry
     * </ul>
     */
    public void dispose() throws CacheException {
        try {
            for (int i = 0; i < cachePeers.size(); i++) {
                RMICachePeer rmiCachePeer = (RMICachePeer) cachePeers.get(i);
                UnicastRemoteObject.unexportObject(rmiCachePeer, false);
                Naming.unbind(rmiCachePeer.getUrl());
            }
            LOG.debug("Server unbound in registry");
        } catch (Exception e) {
            throw new CacheException("Problem unbinding remote cache peers. Error was " + e.getMessage());
        }
    }

    /**
     * All of the caches which are listenting for remote changes.
     *
     * @return a list of <code>RMICachePeer</code> objects
     */
    public List getBoundCachePeers() {
        return cachePeers;
    }

    /**
     * Gets a list of cache peers
     */
    List getCachePeers() {
        return cachePeers;
    }
}
