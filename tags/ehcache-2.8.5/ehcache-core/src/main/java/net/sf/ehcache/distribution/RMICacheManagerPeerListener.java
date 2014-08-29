/**
 *  Copyright Terracotta, Inc.
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
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheEventListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * If no security manager is specified, then no class loading, either by RMI clients or servers, is allowed,
 * aside from what can be found in the local CLASSPATH. The classpath of each instance of this class should have
 * all required classes to enable distribution, so no remote classloading is required or desirable. Accordingly,
 * no security manager is set and there are no special JVM configuration requirements.
 * <p/>
 * This class opens a ServerSocket. The dispose method should be called for orderly closure of that socket. This class
 * has a shutdown hook which calls dispose() as a convenience feature for developers.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class RMICacheManagerPeerListener implements CacheManagerPeerListener {

    private static final Logger LOG = LoggerFactory.getLogger(RMICacheManagerPeerListener.class.getName());
    private static final int MINIMUM_SENSIBLE_TIMEOUT = 200;
    private static final int NAMING_UNBIND_RETRY_INTERVAL = 400;
    private static final int NAMING_UNBIND_MAX_RETRIES = 10;

    /**
     * The cache peers. The value is an RMICachePeer.
     */
    protected final Map cachePeers = new HashMap();

    /**
     * status.
     */
    protected Status status;

    /**
     * The RMI listener port
     */
    protected Integer port;

    private Registry registry;
    private boolean registryCreated;
    private final String hostName;

    private CacheManager cacheManager;
    private Integer socketTimeoutMillis;
    private Integer remoteObjectPort;

    /**
     * Constructor with full arguments.
     *
     * @param hostName            may be null, in which case the hostName will be looked up. Machines with multiple
     *                            interfaces should specify this if they do not want it to be the default NIC.
     * @param port                a port in the range 1025 - 65536
     * @param remoteObjectPort    the port number on which the remote objects bound in the registry receive calls.
                                  This defaults to a free port if not specified.
     * @param cacheManager        the CacheManager this listener belongs to
     * @param socketTimeoutMillis TCP/IP Socket timeout when waiting on response
     */
    public RMICacheManagerPeerListener(String hostName, Integer port, Integer remoteObjectPort, CacheManager cacheManager,
                                       Integer socketTimeoutMillis) throws UnknownHostException {

        status = Status.STATUS_UNINITIALISED;

        if (hostName != null && hostName.length() != 0) {
            this.hostName = hostName;
            if (hostName.equals("localhost")) {
                LOG.warn("Explicitly setting the listener hostname to 'localhost' is not recommended. "
                        + "It will only work if all CacheManager peers are on the same machine.");
            }
        } else {
            this.hostName = calculateHostAddress();
        }
        if (port == null || port.intValue() == 0) {
            assignFreePort(false);
        } else {
            this.port = port;
        }

        //by default is 0, which is ok.
        this.remoteObjectPort = remoteObjectPort;

        this.cacheManager = cacheManager;
        if (socketTimeoutMillis == null || socketTimeoutMillis.intValue() < MINIMUM_SENSIBLE_TIMEOUT) {
            throw new IllegalArgumentException("socketTimoutMillis must be a reasonable value greater than 200ms");
        }
        this.socketTimeoutMillis = socketTimeoutMillis;

    }

    /**
     * Assigns a free port to be the listener port.
     *
     * @throws IllegalStateException if the statis of the listener is not {@link net.sf.ehcache.Status#STATUS_UNINITIALISED}
     */
    protected void assignFreePort(boolean forced) throws IllegalStateException {
        if (status != Status.STATUS_UNINITIALISED) {
            throw new IllegalStateException("Cannot change the port of an already started listener.");
        }
        this.port = Integer.valueOf(this.getFreePort());
        if (forced) {
            LOG.warn("Resolving RMI port conflict by automatically using a free TCP/IP port to listen on: " + this.port);
        } else {
            LOG.debug("Automatically finding a free TCP/IP port to listen on: " + this.port);
        }
    }


    /**
     * Calculates the host address as the default NICs IP address
     *
     * @throws UnknownHostException
     */
    protected String calculateHostAddress() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }


    /**
     * Gets a free server socket port.
     *
     * @return a number in the range 1025 - 65536 that was free at the time this method was executed
     * @throws IllegalArgumentException
     */
    protected int getFreePort() throws IllegalArgumentException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(0);
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not acquire a free port number.");
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    LOG.debug("Error closing ServerSocket: " + e.getMessage());
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void init() throws CacheException {
        if (!status.equals(Status.STATUS_UNINITIALISED)) {
            return;
        }
        RMICachePeer rmiCachePeer = null;
        try {
            startRegistry();
            int counter = 0;
            populateListOfRemoteCachePeers();
            synchronized (cachePeers) {
                for (Iterator iterator = cachePeers.values().iterator(); iterator.hasNext();) {
                    rmiCachePeer = (RMICachePeer) iterator.next();
                    bind(rmiCachePeer.getUrl(), rmiCachePeer);
                    counter++;
                }
            }
            LOG.debug(counter + " RMICachePeers bound in registry for RMI listener");
            status = Status.STATUS_ALIVE;
        } catch (Exception e) {
            String url = null;
            if (rmiCachePeer != null) {
                url = rmiCachePeer.getUrl();
            }

            throw new CacheException("Problem starting listener for RMICachePeer "
                    + url + ". Initial cause was " + e.getMessage(), e);
        }
    }

    /**
     * Bind a cache peer
     *
     * @param rmiCachePeer
     */
    protected void bind(String peerName, RMICachePeer rmiCachePeer) throws Exception {
        Naming.rebind(peerName, rmiCachePeer);
    }

    /**
     * Returns a list of bound objects.
     * <p/>
     * This should match the list of cachePeers i.e. they should always be bound
     *
     * @return a list of String representations of <code>RMICachePeer</code> objects
     */
    protected String[] listBoundRMICachePeers() throws CacheException {
        try {
            return registry.list();
        } catch (RemoteException e) {
            throw new CacheException("Unable to list cache peers " + e.getMessage());
        }
    }

    /**
     * Returns a reference to the remote object.
     *
     * @param name the name of the cache e.g. <code>sampleCache1</code>
     */
    protected Remote lookupPeer(String name) throws CacheException {
        try {
            return registry.lookup(name);
        } catch (Exception e) {
            throw new CacheException("Unable to lookup peer for replicated cache " + name + " "
                    + e.getMessage());
        }
    }

    /**
     * Should be called on init because this is one of the last things that should happen on CacheManager startup.
     */
    protected void populateListOfRemoteCachePeers() throws RemoteException {
        String[] names = cacheManager.getCacheNames();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            Ehcache cache = cacheManager.getEhcache(name);
            synchronized (cachePeers) {
                if (cachePeers.get(name) == null) {
                    if (isDistributed(cache)) {
                        RMICachePeer peer;
                        if (cache.getCacheConfiguration().getTransactionalMode().isTransactional()) {
                            peer = new TransactionalRMICachePeer(cache, hostName, port, remoteObjectPort, socketTimeoutMillis);
                        } else {
                            peer = new RMICachePeer(cache, hostName, port, remoteObjectPort, socketTimeoutMillis);
                        }
                        cachePeers.put(name, peer);
                    }
                }
            }
        }

    }

    /**
     * Determine if the given cache is distributed.
     *
     * @param cache the cache to check
     * @return true if a <code>CacheReplicator</code> is found in the listeners
     */
    protected boolean isDistributed(Ehcache cache) {
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
     * Start the rmiregistry.
     * <p/>
     * The alternative is to use the <code>rmiregistry</code> binary, in which case:
     * <ol/>
     * <li>rmiregistry running
     * <li>-Djava.rmi.server.codebase="file:///Users/gluck/work/ehcache/build/classes/ file:///Users/gluck/work/ehcache/lib/commons-logging-1.0.4.jar"
     * </ol>
     *
     * @throws RemoteException
     */
    protected void startRegistry() throws RemoteException {
        try {
            registry = LocateRegistry.getRegistry(port.intValue());
            try {
                registry.list();
            } catch (RemoteException e) {
                //may not be created. Let's create it.
                registry = LocateRegistry.createRegistry(port.intValue());
                registryCreated = true;
            }
        } catch (ExportException exception) {
            LOG.error("Exception starting RMI registry. Error was " + exception.getMessage(), exception);
        }
    }

    /**
     * Stop the rmiregistry if it was started by this class.
     *
     * @throws RemoteException
     */
    protected void stopRegistry() throws RemoteException {
        if (registryCreated) {
            // the unexportObject call must be done on the Registry object returned
            // by createRegistry not by getRegistry, a NoSuchObjectException is
            // thrown otherwise
            boolean success = UnicastRemoteObject.unexportObject(registry, true);
            if (success) {
                LOG.debug("rmiregistry unexported.");
            } else {
                LOG.warn("Could not unexport rmiregistry.");
            }
        }
    }

    /**
     * Stop the listener. It
     * <ul>
     * <li>unbinds the objects from the registry
     * <li>unexports Remote objects
     * </ul>
     */
    public void dispose() throws CacheException {
        if (!status.equals(Status.STATUS_ALIVE)) {
            return;
        }
        try {
            int counter = 0;
            synchronized (cachePeers) {
                for (Iterator iterator = cachePeers.values().iterator(); iterator.hasNext();) {
                    RMICachePeer rmiCachePeer = (RMICachePeer) iterator.next();
                    disposeRMICachePeer(rmiCachePeer);
                    counter++;
                }
                stopRegistry();
            }
            LOG.debug(counter + " RMICachePeers unbound from registry in RMI listener");
            status = Status.STATUS_SHUTDOWN;
        } catch (Exception e) {
            throw new CacheException("Problem unbinding remote cache peers. Initial cause was " + e.getMessage(), e);
        }
    }

    /**
     * A template method to dispose an individual RMICachePeer. This consists of:
     * <ol>
     * <li>Unbinding the peer from the naming service
     * <li>Unexporting the peer
     * </ol>
     * Override to specialise behaviour
     *
     * @param rmiCachePeer the cache peer to dispose of
     * @throws Exception thrown if something goes wrong
     */
    protected void disposeRMICachePeer(RMICachePeer rmiCachePeer) throws Exception {
        unbind(rmiCachePeer);
    }

    /**
     * Unbinds an RMICachePeer and unexports it.
     * <p/>
     * We unbind from the registry first before unexporting.
     * Unbinding first removes the very small possibility of a client
     * getting the object from the registry while we are trying to unexport it.
     * <p/>
     * This method may take up to 4 seconds to complete, if we are having trouble
     * unexporting the peer.
     *
     * @param rmiCachePeer the bound and exported cache peer
     * @throws Exception
     */
    protected void unbind(RMICachePeer rmiCachePeer) throws Exception {
        String url = rmiCachePeer.getUrl();
        try {
            Naming.unbind(url);
        } catch (NotBoundException e) {
            LOG.warn(url + " not bound therefore not unbinding.");
        }
        // Try to gracefully unexport before forcing it.
        boolean unexported = UnicastRemoteObject.unexportObject(rmiCachePeer, false);
        for (int count = 1; (count < NAMING_UNBIND_MAX_RETRIES) && !unexported; count++) {
            try {
                Thread.sleep(NAMING_UNBIND_RETRY_INTERVAL);
            } catch (InterruptedException ie) {
                // break out of the unexportObject loop
                break;
            }
            unexported = UnicastRemoteObject.unexportObject(rmiCachePeer, false);
        }

        // If we still haven't been able to unexport, force the unexport
        // as a last resort.
        if (!unexported) {
            if (!UnicastRemoteObject.unexportObject(rmiCachePeer, true)) {
                LOG.warn("Unable to unexport rmiCachePeer: " + rmiCachePeer.getUrl() + ".  Skipping.");
            }
        }
    }

    /**
     * All of the caches which are listening for remote changes.
     *
     * @return a list of <code>RMICachePeer</code> objects. The list if not live
     */
    public List getBoundCachePeers() {
        List cachePeerList = new ArrayList();
        synchronized (cachePeers) {
            for (Iterator iterator = cachePeers.values().iterator(); iterator.hasNext();) {
                RMICachePeer rmiCachePeer = (RMICachePeer) iterator.next();
                cachePeerList.add(rmiCachePeer);
            }
        }
        return cachePeerList;
    }

    /**
     * Returns the listener status.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * A listener will normally have a resource that only one instance can use at the same time,
     * such as a port. This identifier is used to tell if it is unique and will not conflict with an
     * existing instance using the resource.
     *
     * @return a String identifier for the resource
     */
    public String getUniqueResourceIdentifier() {
        return "RMI listener port: " + port;
    }

    /**
     * If a conflict is detected in unique resource use, this method signals the listener to attempt
     * automatic resolution of the resource conflict.
     *
     * @throws IllegalStateException if the statis of the listener is not {@link net.sf.ehcache.Status#STATUS_UNINITIALISED}
     */
    public void attemptResolutionOfUniqueResourceConflict() throws IllegalStateException, CacheException {
        assignFreePort(true);
    }

    /**
     * The replication scheme this listener interacts with.
     * Each peer provider has a scheme name, which can be used by caches to specify for replication and bootstrap purposes.
     *
     * @return the well-known scheme name, which is determined by the replication provider author.
     */
    public String getScheme() {
        return "RMI";
    }

    /**
     * Called immediately after a cache has been added and activated.
     * <p/>
     * Note that the CacheManager calls this method from a synchronized method. Any attempt to call a synchronized
     * method on CacheManager from this method will cause a deadlock.
     * <p/>
     * Note that activation will also cause a CacheEventListener status change notification from
     * {@link net.sf.ehcache.Status#STATUS_UNINITIALISED} to {@link net.sf.ehcache.Status#STATUS_ALIVE}. Care should be
     * taken on processing that notification because:
     * <ul>
     * <li>the cache will not yet be accessible from the CacheManager.
     * <li>the addCaches methods whih cause this notification are synchronized on the CacheManager. An attempt to call
     * {@link net.sf.ehcache.CacheManager#getCache(String)} will cause a deadlock.
     * </ul>
     * The calling method will block until this method returns.
     * <p/>
     * Repopulates the list of cache peers and rebinds the list.
     * This method should be called if a cache is dynamically added
     *
     * @param cacheName the name of the <code>Cache</code> the operation relates to
     * @see net.sf.ehcache.event.CacheEventListener
     */
    public void notifyCacheAdded(String cacheName) throws CacheException {


            LOG.debug("Adding to RMI listener", cacheName);

        //Don't add if exists.
        synchronized (cachePeers) {
            if (cachePeers.get(cacheName) != null) {
                return;
            }
        }

        Ehcache cache = cacheManager.getEhcache(cacheName);
        if (isDistributed(cache)) {
            RMICachePeer rmiCachePeer = null;
            String url = null;
            try {
                if (cache.getCacheConfiguration().getTransactionalMode().isTransactional()) {
                    rmiCachePeer = new TransactionalRMICachePeer(cache, hostName, port, remoteObjectPort, socketTimeoutMillis);
                } else {
                    rmiCachePeer = new RMICachePeer(cache, hostName, port, remoteObjectPort, socketTimeoutMillis);
                }
                url = rmiCachePeer.getUrl();
                bind(url, rmiCachePeer);
            } catch (Exception e) {
                throw new CacheException("Problem starting listener for RMICachePeer "
                        + url + ". Initial cause was " + e.getMessage(), e);
            }

            synchronized (cachePeers) {
                cachePeers.put(cacheName, rmiCachePeer);
            }

        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(cachePeers.size() + " RMICachePeers bound in registry for RMI listener");
        }
    }

    /**
     * Called immediately after a cache has been disposed and removed. The calling method will block until
     * this method returns.
     * <p/>
     * Note that the CacheManager calls this method from a synchronized method. Any attempt to call a synchronized
     * method on CacheManager from this method will cause a deadlock.
     * <p/>
     * Note that a {@link net.sf.ehcache.event.CacheEventListener} status changed will also be triggered. Any attempt from that notification
     * to access CacheManager will also result in a deadlock.
     *
     * @param cacheName the name of the <code>Cache</code> the operation relates to
     */
    public void notifyCacheRemoved(String cacheName) {


            LOG.debug("Removing from RMI listener", cacheName);

        //don't remove if already removed.
        synchronized (cachePeers) {
            if (cachePeers.get(cacheName) == null) {
                return;
            }
        }

        RMICachePeer rmiCachePeer;
        synchronized (cachePeers) {
            rmiCachePeer = (RMICachePeer) cachePeers.remove(cacheName);
        }
        String url = null;
        try {
            unbind(rmiCachePeer);
        } catch (Exception e) {
            throw new CacheException("Error removing Cache Peer "
                    + url + " from listener. Message was: " + e.getMessage(), e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(cachePeers.size() + " RMICachePeers bound in registry for RMI listener");
        }
    }


    /**
     * Package local method for testing
     */
    void addCachePeer(String name, RMICachePeer peer) {
        synchronized (cachePeers) {
            cachePeers.put(name, peer);

        }
    }
}
