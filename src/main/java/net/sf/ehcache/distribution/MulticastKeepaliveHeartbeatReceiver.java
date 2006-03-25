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

import net.sf.ehcache.CacheManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

/**
 * Receives heartbeats from any {@link MulticastKeepaliveHeartbeatSender}s out there.
 * <p/>
 * Our own multicast heartbeats are ignored.
 *
 * @author Greg Luck
 * @version $Id: MulticastKeepaliveHeartbeatReceiver.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public class MulticastKeepaliveHeartbeatReceiver {

    private static final Log LOG = LogFactory.getLog(MulticastKeepaliveHeartbeatReceiver.class.getName());

    private InetAddress groupMulticastAddress;
    private Integer groupMulticastPort;
    private MulticastReceiverThread receiverThread;
    private MulticastSocket socket;
    private boolean stopped;
    private MulticastRMICacheManagerPeerProvider peerProvider;

    /**
     * Constructor
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
     * Start
     * @throws IOException
     */
    void init() throws IOException {

        socket = new MulticastSocket(groupMulticastPort.intValue());
        socket.joinGroup(groupMulticastAddress);
        receiverThread = new MulticastReceiverThread();
        receiverThread.start();
    }

    /**
     * Shutdown the heartbeat
     */
    public void dispose() {
        stopped = true;
        receiverThread.interrupt();
    }


    /**
     * A multicast receiver which continously receives heartbeats.
     */
    private class MulticastReceiverThread extends Thread {



        public void run() {
            byte[] buf = new byte[PayloadUtil.MTU];
            while (!stopped) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);
                    byte[] payload = packet.getData();
                    processPayload(payload);


                } catch (IOException e) {
                    if (!stopped) {
                        LOG.error("Error receiving heartbeat. " + e.getMessage() + ". Error was " + e.getMessage());
                    }
                }
            }
        }

        private void processPayload(byte[] compressedPayload) {
            byte[] payload = PayloadUtil.ungzip(compressedPayload);
            String rmiUrls = new String(payload);
            if (self(rmiUrls)) {
                return;
            }
            rmiUrls = rmiUrls.trim();
            if (LOG.isDebugEnabled()) {
                LOG.debug("rmiUrls received " + rmiUrls);
            }
            for (StringTokenizer stringTokenizer = new StringTokenizer(rmiUrls,
                    PayloadUtil.URL_DELIMITER); stringTokenizer.hasMoreTokens();) {
                String rmiUrl = stringTokenizer.nextToken();
                registerNotification(rmiUrl);
            }
        }


        /**
         * @param rmiUrls
         * @return true if our own hostname and listener port are found in the list. This then means we have
         * caught our onw multicast, and should be ignored.
         */
        private boolean self(String rmiUrls) {
            CacheManager cacheManager = peerProvider.getCacheManager();
            CachePeer peer = (CachePeer) cacheManager.getCachePeerListener().getBoundCachePeers().get(0);
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
        public void interrupt() {
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
