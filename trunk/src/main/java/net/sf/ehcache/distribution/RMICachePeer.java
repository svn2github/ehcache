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
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.RMISocketFactory;

/**
 * An RMI based implementation of <code>CachePeer</code>.
 * <p/>
 * This class features a customised RMIClientSocketFactory which enables socket timeouts to be configured.
 * @author Greg Luck
 * @version $Id: RMICachePeer.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public class RMICachePeer extends UnicastRemoteObject implements CachePeer, Remote {

    private static final Log LOG = LogFactory.getLog(RMICachePeer.class.getName());

    private String hostname;
    private Integer port;
    private Cache cache;
    private Integer socketTimeoutMillis;

    /**
     * Construct a new remote peer
     * @param cache
     * @param hostName
     * @param port
     * @param socketTimeoutMillis
     * @throws RemoteException
     */
    public RMICachePeer(Cache cache, String hostName, Integer port, Integer socketTimeoutMillis)
            throws RemoteException {
        super(0, new ConfigurableRMIClientSocketFactory(socketTimeoutMillis),
                RMISocketFactory.getDefaultSocketFactory());

        this.hostname = hostName;
        this.port = port;
        this.cache = cache;
        this.socketTimeoutMillis = socketTimeoutMillis;
    }

    /**
     * {@inheritDoc}
     *
     * This implementation gives an URL which has meaning to the RMI remoting system
     * @return the URL, without the scheme, as a string e.g. //hostname:port/cacheName
     */
    public String getUrl() {
        return new StringBuffer()
                .append("//")
                .append(hostname)
                .append(":")
                .append(port)
                .append("/")
                .append(cache.getName())
                .toString();
    }

    /**
     * {@inheritDoc}
     *
     * This implementation gives an URL which has meaning to the RMI remoting system
     * @return the URL, without the scheme, as a string e.g. //hostname:port
     */
    public String getUrlBase() {
        return new StringBuffer()
                .append("//")
                .append(hostname)
                .append(":")
                .append(port)
                .toString();
    }


    /**
     * Puts an Element into the underlying cache without notifying listeners or updating statistics.
     *
     * @param element
     * @throws java.rmi.RemoteException
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     */
    public void put(Element element) throws RemoteException, IllegalArgumentException, IllegalStateException {
        cache.put(element, true);
    }


    /**
     * Removes an Element from the underlying cache without notifying listeners or updating statistics.
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the cache
     * @throws RemoteException
     * @throws IllegalStateException
     */
    public boolean remove(Serializable key) throws RemoteException, IllegalStateException {
        return cache.remove(key, true);
    }

    /**
     * Removes all cached items.
     *
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public void removeAll() throws RemoteException, IllegalStateException {
        try {
            cache.removeAll();
        } catch (IOException e) {
            LOG.error(e.getMessage());
            throw new RemoteException(e.getMessage());
        }
    }

    /**
     * Gets the status attribute of the Store object
     *
     * @return The status value
     */
    public Status getStatus() throws RemoteException {
        return Status.STATUS_UNINITIALISED;
    }

    /**
     * Gets the cache name
     */
    public String getName() throws RemoteException {
        return cache.getName();
    }



    /**
     * {@inheritDoc}
     */
    public String getGuid() throws RemoteException {
        return cache.getGuid();
    }

    /**
     * Gets the cache instance that this listener is bound to
     */
    Cache getBoundCacheInstance() {
        return cache;
    }

}
