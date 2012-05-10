/**
 *  Copyright Terracotta, Inc.
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
import net.sf.ehcache.util.CacheTransactionHelper;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;

/**
 * An RMI based implementation of <code>CachePeer</code> supporting transactions.
 *
 * @author Ludovic Orban
 */
public class TransactionalRMICachePeer extends RMICachePeer {

    private final Ehcache cache;

    /**
     * Construct a new remote peer supporting transactions
     *
     * @param cache               The cache attached to the peer
     * @param hostName            The host name the peer is running on.
     * @param rmiRegistryPort     The port number on which the RMI Registry listens. Should be an unused port in
     *                            the range 1025 - 65536
     * @param remoteObjectPort    the port number on which the remote objects bound in the registry receive calls.
     *                            This defaults to a free port if not specified.
     *                            Should be an unused port in the range 1025 - 65536
     * @param socketTimeoutMillis
     * @throws java.rmi.RemoteException
     */
    public TransactionalRMICachePeer(Ehcache cache, String hostName, Integer rmiRegistryPort,
                                     Integer remoteObjectPort, Integer socketTimeoutMillis) throws RemoteException {
        super(cache, hostName, rmiRegistryPort, remoteObjectPort, socketTimeoutMillis);
        this.cache = cache;
    }

    @Override
    public List getKeys() throws RemoteException {
        boolean started = CacheTransactionHelper.isTransactionStarted(cache);
        if (!started) {
            CacheTransactionHelper.beginTransactionIfNeeded(cache);
        }

        try {
            return super.getKeys();
        } finally {
            if (!started) {
                CacheTransactionHelper.commitTransactionIfNeeded(cache);
            }
        }
    }

    @Override
    public Element getQuiet(Serializable key) throws RemoteException {
        boolean started = CacheTransactionHelper.isTransactionStarted(cache);
        if (!started) {
            CacheTransactionHelper.beginTransactionIfNeeded(cache);
        }

        try {
            return super.getQuiet(key);
        } finally {
            if (!started) {
                CacheTransactionHelper.commitTransactionIfNeeded(cache);
            }
        }
    }

    @Override
    public List getElements(List keys) throws RemoteException {
        boolean started = CacheTransactionHelper.isTransactionStarted(cache);
        if (!started) {
            CacheTransactionHelper.beginTransactionIfNeeded(cache);
        }

        try {
            return super.getElements(keys);
        } finally {
            if (!started) {
                CacheTransactionHelper.commitTransactionIfNeeded(cache);
            }
        }
    }

    @Override
    public void put(Element element) throws RemoteException, IllegalArgumentException, IllegalStateException {
        boolean started = CacheTransactionHelper.isTransactionStarted(cache);
        if (!started) {
            CacheTransactionHelper.beginTransactionIfNeeded(cache);
        }

        try {
            super.put(element);
        } finally {
            if (!started) {
                CacheTransactionHelper.commitTransactionIfNeeded(cache);
            }
        }
    }

    @Override
    public boolean remove(Serializable key) throws RemoteException, IllegalStateException {
        boolean started = CacheTransactionHelper.isTransactionStarted(cache);
        if (!started) {
            CacheTransactionHelper.beginTransactionIfNeeded(cache);
        }

        try {
            return super.remove(key);
        } finally {
            if (!started) {
                CacheTransactionHelper.commitTransactionIfNeeded(cache);
            }
        }
    }

    @Override
    public void removeAll() throws RemoteException, IllegalStateException {
        boolean started = CacheTransactionHelper.isTransactionStarted(cache);
        if (!started) {
            CacheTransactionHelper.beginTransactionIfNeeded(cache);
        }

        try {
            super.removeAll();
        } finally {
            if (!started) {
                CacheTransactionHelper.commitTransactionIfNeeded(cache);
            }
        }
    }

    @Override
    public void send(List eventMessages) throws RemoteException {
        boolean started = CacheTransactionHelper.isTransactionStarted(cache);
        if (!started) {
            CacheTransactionHelper.beginTransactionIfNeeded(cache);
        }

        try {
            super.send(eventMessages);
        } finally {
            if (!started) {
                CacheTransactionHelper.commitTransactionIfNeeded(cache);
            }
        }
    }
}
