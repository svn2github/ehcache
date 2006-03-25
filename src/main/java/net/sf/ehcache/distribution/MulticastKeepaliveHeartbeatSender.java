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
                            LOG.error("Error receiving heartbeat. Error was " + e.getMessage());
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
