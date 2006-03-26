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

import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.rmi.RemoteException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;

/**
 * This class provides utility methods for assembling and disassembling a heartbeat payload.
 * <p/>
 * Care is taken to fit the payload into the MTU of ethernet, which is 1500 bytes.
 * The algorithms in this class are capable of creating payloads for CacheManagers containing
 * approximately 500 caches to be replicated.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: PayloadUtil.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public final class PayloadUtil {

    /**
     * The maximum transmission unit. This varies by link layer. For ethernet, fast ethernet and
     * gigabit ethernet it is 1500 bytes, the value chosen.
     * <p/>
     * Payloads are limited to this so that there is no fragmentation and no necessity for a complex
     * reassembly protocol.
     */
    public static final int MTU = 1500;

    /**
     * Delmits URLS sent via heartbeats over sockets
     */
    public static final String URL_DELIMITER = "|";

    private static final Log LOG = LogFactory.getLog(PayloadUtil.class.getName());


    /**
     * Utility class therefore precent construction
     */
    private PayloadUtil() {
        //noop
    }

    /**
     * Assembles a list of URLs
     *
     * @param localCachePeers
     * @return an uncompressed payload with catenated rmiUrls.
     */
    public static byte[] assembleUrlList(List localCachePeers) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < localCachePeers.size(); i++) {
            CachePeer cachePeer = (CachePeer) localCachePeers.get(i);
            String rmiUrl = null;
            try {
                rmiUrl = cachePeer.getUrl();
            } catch (RemoteException e) {
                LOG.error("This should never be thrown as it is called locally");
            }
            if (i != localCachePeers.size() - 1) {
                sb.append(rmiUrl).append(URL_DELIMITER);
            } else {
                sb.append(rmiUrl);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Cache peers: " + sb);
        }
        return sb.toString().getBytes();
    }

    /**
     * Gzips a byte[]. For text, approximately 10:1 compression is achieved.
     * @param ungzipped the bytes to be gzipped
     * @return gzipped bytes
     */
    public static byte[] gzip(byte[] ungzipped) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bytes);
            gzipOutputStream.write(ungzipped);
            gzipOutputStream.close();
        } catch (IOException e) {
            LOG.fatal("Could not gzip " + ungzipped);
        }
        return bytes.toByteArray();
    }

    /**
     * The fastest Ungzip implementation. See PageInfoTest in ehcache-constructs.
     * A high performance implementation, although not as fast as gunzip3.
     * gunzips 100000 of ungzipped content in 9ms on the reference machine.
     * It does not use a fixed size buffer and is therefore suitable for arbitrary
     * length arrays.
     *
     * @param gzipped
     * @return a plain, uncompressed byte[]
     */
    public static byte[] ungzip(final byte[] gzipped) {
        byte[] ungzipped = new byte[0];
        try {
            final GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(gzipped));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(gzipped.length);
            final byte[] buffer = new byte[PayloadUtil.MTU];
            int bytesRead = 0;
            while (bytesRead != -1) {
                bytesRead = inputStream.read(buffer, 0, PayloadUtil.MTU);
                if (bytesRead != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
            }
            ungzipped = byteArrayOutputStream.toByteArray();
            inputStream.close();
            byteArrayOutputStream.close();
        } catch (IOException e) {
            LOG.fatal("Could not ungzip. Heartbeat will not be working. " + e.getMessage());
        }
        return ungzipped;
    }

    

}
