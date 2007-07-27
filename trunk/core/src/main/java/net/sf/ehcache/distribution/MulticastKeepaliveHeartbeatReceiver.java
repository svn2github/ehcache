/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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

import net.sf.ehcache.CacheManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;

/**
 * Receives heartbeats from any {@link MulticastKeepaliveHeartbeatSender}s out there.
 * <p/>
 * Our own multicast heartbeats are ignored.
 *
 * @author Greg Luck
 * @version $Id$
 */
public final class MulticastKeepaliveHeartbeatReceiver {
    private static final Log LOG = LogFactory.getLog(MulticastKeepaliveHeartbeatReceiver.class.getName());

    private ExecutorService processingThreadPool;
    private Set rmiUrlsProcessingQueue = Collections.synchronizedSet(new HashSet());
    private final InetAddress groupMulticastAddress;
    private final Integer groupMulticastPort;
    private MulticastReceiverThread receiverThread;
    private MulticastSocket socket;
    private boolean stopped;
    private final MulticastRMICacheManagerPeerProvider peerProvider;

    /**
     * Constructor.
     *
     * @param peerProvider
     * @param multicastAddress
     * @param multicastPort
     */
    public MulticastKeepaliveHeartbeatReceiver(
            MulticastRMICacheManagerPeerProvider peerProvider, InetAddress multicastAddress, Integer multicastPort) {
        this.peerProvider = peerProvider;
        this.groupMulticastAddress = multicastAddress;
        this.groupMulticastPort = multicastPort;
    }

    /**
     * Start.
     *
     * @throws IOException
     */
    final void init() throws IOException {
        socket = new MulticastSocket(groupMulticastPort.intValue());
        socket.joinGroup(groupMulticastAddress);
        receiverThread = new MulticastReceiverThread();
        receiverThread.start();
        processingThreadPool = Executors.newCachedThreadPool();
    }

    /**
     * Shutdown the heartbeat.
     */
    public final void dispose() {
        LOG.debug("dispose called");
        processingThreadPool.shutdownNow();
        stopped = true;
        receiverThread.interrupt();
    }

    /**
     * A multicast receiver which continously receives heartbeats.
     */
    private final class MulticastReceiverThread extends Thread {

        /**
         * Constructor
         */
        public MulticastReceiverThread() {
            super("Multicast Heartbeat Receiver Thread");
            setDaemon(true);
        }

        public final void run() {
            byte[] buf = new byte[PayloadUtil.MTU];
            try {
                while (!stopped) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    try {
                        socket.receive(packet);
                        byte[] payload = packet.getData();
                        processPayload(payload);


                    } catch (IOException e) {
                        if (!stopped) {
                            LOG.error("Error receiving heartbeat. " + e.getMessage() +
                                    ". Initial cause was " + e.getMessage(), e);
                        }
                    }
                }
            } catch (Throwable t) {
                LOG.error("Multicast receiver thread caught throwable. Cause was " + t.getMessage() + ". Continuing...");
            }
        }

        private void processPayload(byte[] compressedPayload) {
            byte[] payload = PayloadUtil.ungzip(compressedPayload);
            String rmiUrls = new String(payload);
            if (self(rmiUrls)) {
                return;
            }
            rmiUrls = rmiUrls.trim();
            if (LOG.isTraceEnabled()) {
                LOG.trace("rmiUrls received " + rmiUrls);
            }
            processRmiUrls(rmiUrls);
        }

        /**
         * This method forks a new executor to process the received heartbeat in a thread pool.
         * That way each remote cache manager cannot interfere with others.
         * <p/>
         * In the worst case, we have as many concurrent threads as remote cache managers.
         *
         * @param rmiUrls
         */
        private void processRmiUrls(final String rmiUrls) {
            if (rmiUrlsProcessingQueue.contains(rmiUrls)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("We are already processing these rmiUrls. Another heartbeat came before we finished: " + rmiUrls);
                }
                return;
            }

            processingThreadPool.execute(new Runnable() {
                public void run() {
                    try {
                        // Add the rmiUrls we are processing.
                        rmiUrlsProcessingQueue.add(rmiUrls);
                        for (StringTokenizer stringTokenizer = new StringTokenizer(rmiUrls,
                                PayloadUtil.URL_DELIMITER); stringTokenizer.hasMoreTokens();) {
                            if (stopped) {
                                return;
                            }
                            String rmiUrl = stringTokenizer.nextToken();
                            registerNotification(rmiUrl);
                            if (!peerProvider.peerUrls.containsKey(rmiUrl)) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Aborting processing of rmiUrls since failed to add rmiUrl: " + rmiUrl);
                                }
                                return;
                            }
                        }
                    } finally {
                        // Remove the rmiUrls we just processed
                        rmiUrlsProcessingQueue.remove(rmiUrls);
                    }
                }
            });
        }


        /**
         * @param rmiUrls
         * @return true if our own hostname and listener port are found in the list. This then means we have
         *         caught our onw multicast, and should be ignored.
         */
        private boolean self(String rmiUrls) {
            CacheManager cacheManager = peerProvider.getCacheManager();
            CacheManagerPeerListener cacheManagerPeerListener = cacheManager.getCachePeerListener();
            if (cacheManagerPeerListener == null) {
                return false;
            }
            List boundCachePeers = cacheManagerPeerListener.getBoundCachePeers();
            if (boundCachePeers == null || boundCachePeers.size() == 0) {
                return false;
            }
            CachePeer peer = (CachePeer) boundCachePeers.get(0);
            String cacheManagerUrlBase = null;
            try {
                cacheManagerUrlBase = peer.getUrlBase();
            } catch (RemoteException e) {
                LOG.error("Error geting url base");
            }
            int baseUrlMatch = rmiUrls.indexOf(cacheManagerUrlBase);
            return baseUrlMatch != -1;
        }

        private void registerNotification(String rmiUrl) {
            peerProvider.registerPeer(rmiUrl);
        }


        /**
         * {@inheritDoc}
         */
        public final void interrupt() {
            try {
                socket.leaveGroup(groupMulticastAddress);
            } catch (IOException e) {
                LOG.error("Error leaving group");
            }
            socket.close();
            super.interrupt();
        }
    }


}
