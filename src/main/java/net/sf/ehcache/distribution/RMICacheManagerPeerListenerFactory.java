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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.util.PropertyUtil;

import java.net.UnknownHostException;
import java.util.Properties;

/**
 * Builds a listener based on RMI
 * <p/>
 * Expected configuration line:
 * <p/>
 * <code>
 * &lt;cachePeerListenerFactory class="net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory"
 * properties="hostName=localhost, port=5000" /&gt;
 * </code>
 * @author Greg Luck
 * @version $Id: RMICacheManagerPeerListenerFactory.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public class RMICacheManagerPeerListenerFactory extends CacheManagerPeerListenerFactory {

    /**
     * The default timeout for cache replication for a single replication action.
     */
    public static final Integer DEFAULT_SOCKET_TIMEOUT_MILLIS = new Integer(2000);

    private static final String HOSTNAME = "hostName";
    private static final String PORT = "port";
    private static final String SOCKET_TIMEOUT_MILLIS = "socketTimeoutMillis";
    /**
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml
     */
    public CacheManagerPeerListener createCachePeerListener(CacheManager cacheManager, Properties properties)
            throws CacheException {
        String hostName = PropertyUtil.extractAndLogProperty(HOSTNAME, properties);
        String portString = PropertyUtil.extractAndLogProperty(PORT, properties);
        Integer port = new Integer(portString);
        String socketTimeoutMillisString = PropertyUtil.extractAndLogProperty(SOCKET_TIMEOUT_MILLIS, properties);
        Integer socketTimeoutMillis;
        if (socketTimeoutMillisString == null) {
            socketTimeoutMillis = DEFAULT_SOCKET_TIMEOUT_MILLIS;
        } else {
            socketTimeoutMillis = new Integer(socketTimeoutMillisString);
        }
        try {
            return new RMICacheManagerPeerListener(hostName, port, cacheManager, socketTimeoutMillis);
        } catch (UnknownHostException e) {
            throw new CacheException("Unable to create CacheManagerPeerListener. Error was " + e.getMessage());
        }
    }
}
