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

import net.sf.ehcache.Element;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * An interface for a cache peer to which updates are made remotely. The distribution mechanism
 * is meant to be pluggable. The requirements of RMI force this interface to exten Remote and
 * throw RemoteException.
 * <p/>
 * It is acknowledged that not all implementations will use Remote. Remote is just a marker interface like Serializable,
 * so nothing specific is required.
 * <p/>
 * Non-RMI implementations should be able to use this interface.
 * Implementations not using RMI should
 *
 * @author Greg Luck
 * @version $Id$
 */
public interface CachePeer extends Remote {


    /**
     * Put an element in the cache.
     * <p/>
     * Resets the access statistics on the element, which would be the case if it has previously been
     * gotten from a cache, and is now being put back.
     *
     * @param element
     * @throws IllegalStateException    if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    void put(Element element) throws IllegalArgumentException, IllegalStateException, RemoteException;

    /**
     * Removes an {@link net.sf.ehcache.Element} from the Cache. This also removes it from any
     * stores it may be in.
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    boolean remove(Serializable key) throws IllegalStateException, RemoteException;

    /**
     * Removes all cached items.
     *
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    void removeAll() throws RemoteException, IllegalStateException;


    /**
     * Send the cache peer with an ordered list of {@link EventMessage}s.
     * <p/>
     * This enables multiple messages to be delivered in one network invocation.
     * @param eventMessages a list of type {@link EventMessage}
     */
    void send(List eventMessages) throws RemoteException;

    /**
     * Gets the cache name.
     */
    String getName() throws RemoteException;

    /**
     * Gets the globally unique id for the underlying <code>Cache</code> instance.
     * @return a String representation of the GUID
     * @throws RemoteException
     */
    String getGuid() throws RemoteException;

    /**
     * The URL for the remote replicator to connect. The value will only have meaning
     * for a specific implementation of replicator and remote peer.
     * <p/>
     * This method is not meant to be used remotely. The replicator already needs to know this. It has
     * to throw RemoteException to comply with RMI requirements
     * @return the URL as a string
     */
    String getUrl() throws RemoteException;


    /**
     * The URL base for the remote replicator to connect. The value will have meaning
     * only to a specific implementation of replicator and remote peer.
     */
     String getUrlBase() throws RemoteException;


    /**
     * Returns a list of all elements in the cache, whether or not they are expired.
     * <p/>
     * The returned keys are unique and can be considered a set.
     * <p/>
     * The List returned is not live. It is a copy.
     * <p/>
     * The time taken is O(n). On a single cpu 1.8Ghz P4, approximately 8ms is required
     * for each 1000 entries.
     *
     * @return a list of {@link Object} keys
     */
    List getKeys() throws RemoteException;


    /**
     * Gets an element from the cache, without updating Element statistics. Cache statistics are
     * still updated.
     * @param key a serializable value
     * @return the element, or null, if it does not exist.
     */
    Element getQuiet(Serializable key) throws RemoteException;

    /**
     * Gets a list of elements from the cache, for a list of keys, without updating Element statistics. Time to
     * idle lifetimes are therefore not affected.
     * <p/>
     * Cache statistics are still updated.
     * @param keys a list of serializable values which represent keys
     * @return a list of Elements. If an element was not found or null, it will not be in the list.
     */
    List getElements(List keys) throws RemoteException;


}
