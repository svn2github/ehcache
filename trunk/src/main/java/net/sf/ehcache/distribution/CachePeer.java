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

import net.sf.ehcache.Element;
import net.sf.ehcache.Status;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

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
 * todo test Cannot keep up with replication
 * todo test production simulation with lots of threads
 * todo new Hibernate plugin to be submitted to Hibernate
 * todo test default replication properties - should replicate sensibly
 * todo test default listener properties - should not need to specify host or timeout
 * todo create sample configs for different purposes
 * todo test sycnchronous performance replicating to five peers
 * todo test asynchronous performance replicating to five peers
 * todo updates backing up if one drops out
 * todo update test for synchronous replicator. Async already tested.
 *
 * @author Greg Luck
 * @version $Id: CachePeer.java,v 1.3 2006/03/25 04:05:56 gregluck Exp $
 */
public interface CachePeer extends Remote {


    /**
     * Put an element in the cache.
     * <p/>
     * Resets the access statistics on the element, which would be the case if it has previously been
     * gotten from a cache, and is now being put back.
     *
     * @param element
     * @throws IllegalStateException    if the cache is not {@link Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    void put(Element element) throws IllegalArgumentException, IllegalStateException, RemoteException;


    /**
     * Removes an {@link net.sf.ehcache.Element} from the Cache. This also removes it from any
     * stores it may be in.
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    boolean remove(Serializable key) throws IllegalStateException, RemoteException;

    /**
     * Removes all cached items.
     *
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    void removeAll() throws RemoteException, IllegalStateException;

    /**
     * Gets the status attribute of the Store object
     *
     * @return The status value
     */
    Status getStatus() throws RemoteException;

    /**
     * Gets the cache name
     */
    String getName() throws RemoteException;

    /**
     * Gets the globally unique id for the underlying <code>Cache</code> instance
     * @return a String representation of the GUID
     * @throws RemoteException
     */
    String getGuid() throws RemoteException;

    /**
     * The URL for the remote replicator to connect. The value will have meaning
     * only to a specific implementation of replicator and remote peer.
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

}
