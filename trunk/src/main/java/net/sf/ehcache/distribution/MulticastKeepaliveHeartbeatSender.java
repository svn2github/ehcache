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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;

import net.sf.ehcache.CacheManager;

/**
 * Sends heartbeats to a multicast group containing a compressed list of URLs. Supports up to approximately
 * 500 configured caches.
 * @author Greg Luck
 * @version $Id: MulticastKeepaliveHeartbeatSender.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public class MulticastKeepaliveHeartbeatSender {



    private static final Log LOG = LogFactory.getLog(MulticastKeepaliveHeartbeatSender.class.getName());
    private static final long HEARTBEAT_INTERVAL = 1000;

    private InetAddress groupMulticastAddress;
    private Integer groupMulticastPort;
    private MulticastServerThread serverThread;
    private boolean stopped;
    private CacheManager cacheManager;


    /**
     * Constructor
     *
     * @param multicastAddress
     * @param multicastPort
     */
    public MulticastKeepaliveHeartbeatSender(CacheManager cacheManager,
                                             InetAddress multicastAddress, Integer multicastPort) {
        this.cacheManager = cacheManager;
        this.groupMulticastAddress = multicastAddress;
        this.groupMulticastPort = multicastPort;

    }

    /**
     * Start the heartbeat thread
     */
    public void init() {
        serverThread = new MulticastServerThread();
        serverThread.start();
    }

    /**
     * Shutdown this heartbeat sender
     */
    public synchronized void dispose() {
        stopped = true;
        notifyAll();
        serverThread.interrupt();
    }

    /**
     * A thread which sends a multicast heartbeat every second
     */
    private class MulticastServerThread extends Thread {

        private MulticastSocket socket;
        private byte[] compressedUrlList;
        private int cachePeersHash;


        /**
         * Constructor
         */
        public MulticastServerThread() {
            super("Multicast Server Thread");
            setDaemon(true);
        }

        public void run() {
            try {
                socket = new MulticastSocket(groupMulticastPort.intValue());
                socket.joinGroup(groupMulticastAddress);

                while (!stopped) {
                    byte[] buffer = createCachePeersPayload();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupMulticastAddress,
                            groupMulticastPort.intValue());
                    socket.send(packet);

                    try {
                        synchronized (this) {
                            wait(HEARTBEAT_INTERVAL);
                        }
                    } catch (InterruptedException e) {
                        if (!stopped) {
                            LOG.error("Error receiving heartbeat. Initial cause was " + e.getMessage(), e);
                        }
                    }
                }
                closeSocket();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Creates a gzipped payload.
         * <p/>
         * The last gzipped payload is retained and only recalculated if the list of cache peers
         * has changed.
         * @return a gzipped byte[]
         */
        private byte[] createCachePeersPayload() {
            List localCachePeers = cacheManager.getCachePeerListener().getBoundCachePeers();
            int newCachePeersHash = localCachePeers.hashCode();
            if (cachePeersHash != newCachePeersHash) {
                cachePeersHash = newCachePeersHash;
                byte[] uncompressedUrlList = PayloadUtil.assembleUrlList(localCachePeers);
                compressedUrlList = PayloadUtil.gzip(uncompressedUrlList);
                if (compressedUrlList.length > PayloadUtil.MTU) {
                    LOG.fatal("Heartbeat is not working. Configure fewer caches for replication. " +
                            "Size is " + compressedUrlList.length + " but should be no greater than" +
                            PayloadUtil.MTU);
                }
            }
            return compressedUrlList;
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
         * Object#wait(long, int) wait(long, int)} methods of the {@link Object}
         * class, or of the {@link #join()}, {@link #join(long)}, {@link
         * #join(long, int)}, {@link #sleep(long)}, or {@link #sleep(long, int)},
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
         * @revised 1.4
         * @spec JSR-51
         */
        public void interrupt() {
            closeSocket();
            super.interrupt();
        }

        private void closeSocket() {
            if (!socket.isClosed()) {
                try {
                    socket.leaveGroup(groupMulticastAddress);
                } catch (IOException e) {
                    LOG.error("erroe leaving group");
                }
                socket.close();
            }
        }

    }
}
