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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 * An RMI based implementation of <code>CachePeer</code>.
 * <p/>
 * This class features a customised RMIClientSocketFactory which enables socket timeouts to be configured.
 *
 * @author Greg Luck
 * @version $Id$
 * @noinspection FieldCanBeLocal
 */
public class RMICachePeer extends UnicastRemoteObject implements CachePeer, Remote {

    private static final Log LOG = LogFactory.getLog(RMICachePeer.class.getName());

    private final String hostname;
    private final Integer port;
    private final Ehcache cache;

    /**
     * Construct a new remote peer.
     *
     * @param cache
     * @param hostName
     * @param port
     * @param socketTimeoutMillis
     * @throws RemoteException
     */
    public RMICachePeer(Ehcache cache, String hostName, Integer port, Integer socketTimeoutMillis)
            throws RemoteException {
        super(0, new ConfigurableRMIClientSocketFactory(socketTimeoutMillis),
                RMISocketFactory.getDefaultSocketFactory());

        this.hostname = hostName;
        this.port = port;
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation gives an URL which has meaning to the RMI remoting system.
     *
     * @return the URL, without the scheme, as a string e.g. //hostname:port/cacheName
     */
    public final String getUrl() {
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
     * <p/>
     * This implementation gives an URL which has meaning to the RMI remoting system.
     *
     * @return the URL, without the scheme, as a string e.g. //hostname:port
     */
    public final String getUrlBase() {
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
    public final boolean remove(Serializable key) throws RemoteException, IllegalStateException {
        return cache.remove(key, true);
    }

    /**
     * Removes all cached items.
     *
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public final void removeAll() throws RemoteException, IllegalStateException {
        cache.removeAll();
    }

    /**
     * Send the cache peer with an ordered list of {@link EventMessage}s
     * <p/>
     * This enables multiple messages to be delivered in one network invocation.
     */
    public final void send(List eventMessages) throws RemoteException {
        for (int i = 0; i < eventMessages.size(); i++) {
            EventMessage eventMessage = (EventMessage) eventMessages.get(i);
            if (eventMessage.getEvent() == EventMessage.PUT) {
                put(eventMessage.getElement());
            } else {
                remove(eventMessage.getSerializableKey());
            }
        }
    }

    /**
     * Gets the cache name
     */
    public final String getName() throws RemoteException {
        return cache.getName();
    }


    /**
     * {@inheritDoc}
     */
    public final String getGuid() throws RemoteException {
        return cache.getGuid();
    }

    /**
     * Gets the cache instance that this listener is bound to
     */
    final Ehcache getBoundCacheInstance() {
        return cache;
    }

}
