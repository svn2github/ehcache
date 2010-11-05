/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends heartbeats to a multicast group containing a compressed list of URLs.
 * <p/>
 * You can control how far the multicast packets propagate by setting the badly misnamed "TTL".
 * Using the multicast IP protocol, the TTL value indicates the scope or range in which a packet may be forwarded.
 * By convention:
 * <ul>
 * <li>0 is restricted to the same host
 * <li>1 is restricted to the same subnet
 * <li>32 is restricted to the same site
 * <li>64 is restricted to the same region
 * <li>128 is restricted to the same continent
 * <li>255 is unrestricted
 * </ul>
 * You can also control how often the heartbeat sends by setting the interval.
 *
 * @author Greg Luck
 * @version $Id$
 */
public final class MulticastKeepaliveHeartbeatSender {


    private static final Logger LOG = LoggerFactory.getLogger(MulticastKeepaliveHeartbeatSender.class.getName());

    private static final int DEFAULT_HEARTBEAT_INTERVAL = 5000;
    private static final int MINIMUM_HEARTBEAT_INTERVAL = 1000;
    private static final int MAXIMUM_PEERS_PER_SEND = 150;

    private static long heartBeatInterval = DEFAULT_HEARTBEAT_INTERVAL;

    private final InetAddress groupMulticastAddress;
    private final Integer groupMulticastPort;
    private final Integer timeToLive;
    private MulticastServerThread serverThread;
    private volatile boolean stopped;
    private final CacheManager cacheManager;
    private InetAddress hostAddress;

    /**
     * Constructor.
     *
     * @param cacheManager     the bound CacheManager. Each CacheManager has a maximum of one sender
     * @param multicastAddress
     * @param multicastPort
     * @param timeToLive       See class description for the meaning of this parameter.
     */
    public MulticastKeepaliveHeartbeatSender(CacheManager cacheManager,
                                             InetAddress multicastAddress, Integer multicastPort,
                                             Integer timeToLive,
                                             InetAddress hostAddress) {
        this.cacheManager = cacheManager;
        this.groupMulticastAddress = multicastAddress;
        this.groupMulticastPort = multicastPort;
        this.timeToLive = timeToLive;
        this.hostAddress = hostAddress;

    }

    /**
     * Start the heartbeat thread
     */
    public final void init() {
        serverThread = new MulticastServerThread();
        serverThread.start();
    }

    /**
     * Shutdown this heartbeat sender
     */
    public final synchronized void dispose() {
        stopped = true;
        notifyAll();
        serverThread.interrupt();
    }

    /**
     * A thread which sends a multicast heartbeat every second
     */
    private final class MulticastServerThread extends Thread {

        private MulticastSocket socket;
        private List compressedUrlListList = new ArrayList();
        private int cachePeersHash;


        /**
         * Constructor
         */
        public MulticastServerThread() {
            super("Multicast Heartbeat Sender Thread");
            setDaemon(true);
        }

        @Override
        public final void run() {
            while (!stopped) {
                try {
                    socket = new MulticastSocket(groupMulticastPort.intValue());
                    if (hostAddress != null) {
                        socket.setInterface(hostAddress);
                    }
                    socket.setTimeToLive(timeToLive.intValue());
                    socket.joinGroup(groupMulticastAddress);

                    while (!stopped) {
                        List buffers = createCachePeersPayload();
                        for (Iterator iter = buffers.iterator(); iter.hasNext();) {
                            byte[] buffer = (byte[]) iter.next();
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupMulticastAddress,
                                    groupMulticastPort.intValue());
                            socket.send(packet);
                        }
                        try {
                            synchronized (this) {
                                wait(heartBeatInterval);
                            }
                        } catch (InterruptedException e) {
                            if (!stopped) {
                                LOG.error("Error receiving heartbeat. Initial cause was " + e.getMessage(), e);
                            }
                        }
                    }
                } catch (IOException e) {
                    LOG.debug("Error on multicast socket", e);
                } catch (Throwable e) {
                    LOG.info("Unexpected throwable in run thread. Continuing..." + e.getMessage(), e);
                } finally {
                    closeSocket();
                }
                if (!stopped) {
                    try {
                        sleep(heartBeatInterval);
                    } catch (InterruptedException e) {
                        LOG.error("Sleep after error interrupted. Initial cause was " + e.getMessage(), e);
                    }
                }
            }
        }

        /**
         * Creates a gzipped payload.
         * <p/>
         * The last gzipped payload is retained and only recalculated if the list of cache peers
         * has changed.
         *
         * @return a gzipped byte[]
         */
        private List createCachePeersPayload() {

            CacheManagerPeerListener cacheManagerPeerListener = cacheManager.getCachePeerListener("RMI");
            if (cacheManagerPeerListener == null) {
                LOG.warn("The RMICacheManagerPeerListener is missing. You need to configure a cacheManagerPeerListenerFactory" +
                        " with class=\"net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory\" in ehcache.xml.");
                return new ArrayList();
            }
            List localCachePeers = cacheManagerPeerListener.getBoundCachePeers();
            int newCachePeersHash = localCachePeers.hashCode();
            if (cachePeersHash != newCachePeersHash) {
                cachePeersHash = newCachePeersHash;
                compressedUrlListList = PayloadUtil.createCompressedPayloadList(localCachePeers, MAXIMUM_PEERS_PER_SEND);
            }
            return compressedUrlListList;
        }


        /**
         * Interrupts this thread.
         * <p/>
         * <p> Unless the current thread is interrupting itself, which is
         * always permitted, the {@link #checkAccess() checkAccess} method
         * of this thread is invoked, which may cause a {@link
         * SecurityException} to be thrown.
         * <p/>
         * <p> If this thread is blocked in an invocation of the {@link
         * Object#wait() wait()}, {@link Object#wait(long) wait(long)}, or {@link
         * Object#wait(long,int) wait(long, int)} methods of the {@link Object}
         * class, or of the {@link #join()}, {@link #join(long)}, {@link
         * #join(long,int)}, {@link #sleep(long)}, or {@link #sleep(long,int)},
         * methods of this class, then its interrupt status will be cleared and it
         * will receive an {@link InterruptedException}.
         * <p/>
         * <p> If this thread is blocked in an I/O operation upon an {@link
         * java.nio.channels.InterruptibleChannel </code>interruptible
         * channel<code>} then the channel will be closed, the thread's interrupt
         * status will be set, and the thread will receive a {@link
         * java.nio.channels.ClosedByInterruptException}.
         * <p/>
         * <p> If this thread is blocked in a {@link java.nio.channels.Selector}
         * then the thread's interrupt status will be set and it will return
         * immediately from the selection operation, possibly with a non-zero
         * value, just as if the selector's {@link
         * java.nio.channels.Selector#wakeup wakeup} method were invoked.
         * <p/>
         * <p> If none of the previous conditions hold then this thread's interrupt
         * status will be set. </p>
         *
         * @throws SecurityException if the current thread cannot modify this thread
         */
        @Override
        public final void interrupt() {
            closeSocket();
            super.interrupt();
        }

        private void closeSocket() {
            try {
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.leaveGroup(groupMulticastAddress);
                    } catch (IOException e) {
                        LOG.error("Error leaving multicast group. Message was " + e.getMessage());
                    }
                    socket.close();
                }
            } catch (NoSuchMethodError e) {
                LOG.debug("socket.isClosed is not supported by JDK1.3");
                try {
                    socket.leaveGroup(groupMulticastAddress);
                } catch (IOException ex) {
                    LOG.error("Error leaving multicast group. Message was " + ex.getMessage());
                }
                socket.close();
            }
        }

    }

    /**
     * Sets the heartbeat interval to something other than the default of 5000ms. This is useful for testing,
     * but not recommended for production. This method is static and so affects the heartbeat interval of all
     * senders. The change takes effect after the next scheduled heartbeat.
     *
     * @param heartBeatInterval a time in ms, greater than 1000
     */
    public static void setHeartBeatInterval(long heartBeatInterval) {
        if (heartBeatInterval < MINIMUM_HEARTBEAT_INTERVAL) {
            LOG.warn("Trying to set heartbeat interval too low. Using MINIMUM_HEARTBEAT_INTERVAL instead.");
            MulticastKeepaliveHeartbeatSender.heartBeatInterval = MINIMUM_HEARTBEAT_INTERVAL;
        } else {
            MulticastKeepaliveHeartbeatSender.heartBeatInterval = heartBeatInterval;
        }
    }

    /**
     * Returns the heartbeat interval.
     */
    public static long getHeartBeatInterval() {
        return heartBeatInterval;
    }

    /**
     * @return the TTL
     */
    public Integer getTimeToLive() {
        return timeToLive;
    }
}
