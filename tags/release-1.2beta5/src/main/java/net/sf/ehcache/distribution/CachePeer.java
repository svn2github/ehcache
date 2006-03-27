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
