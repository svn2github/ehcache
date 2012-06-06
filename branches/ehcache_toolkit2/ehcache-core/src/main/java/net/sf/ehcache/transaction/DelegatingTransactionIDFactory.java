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
package net.sf.ehcache.transaction;

import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.terracotta.TerracottaClient;
import net.sf.ehcache.transaction.xa.XidTransactionID;

import java.util.UUID;

import javax.transaction.xa.Xid;

/**
 * A TransactionIDFactory implementation with delegates calls to either a clustered
 * or non-clustered factory
 * 
 * @author Ludovic Orban
 */
public class DelegatingTransactionIDFactory implements TransactionIDFactory {

    private final TerracottaClient terracottaClient;
    private final String cacheManagerName;
    private volatile ClusteredInstanceFactory clusteredInstanceFactory;
    private volatile TransactionIDFactory transactionIDFactory;

    /**
     * Create a new DelegatingTransactionIDFactory
     *
     * @param terracottaClient a terracotta client
     * @param cacheManagerName the name of the cache manager which creates this.
     */
    public DelegatingTransactionIDFactory(TerracottaClient terracottaClient, String cacheManagerName) {
        this.terracottaClient = terracottaClient;
        this.cacheManagerName = cacheManagerName;
    }

    private TransactionIDFactory get() {
        ClusteredInstanceFactory cif = terracottaClient.getClusteredInstanceFactory();
        if (cif != null && cif != this.clusteredInstanceFactory) {
            this.transactionIDFactory = cif.createTransactionIDFactory(UUID.randomUUID().toString(), cacheManagerName);
            this.clusteredInstanceFactory = cif;
        }

        if (transactionIDFactory == null) {
            transactionIDFactory = new TransactionIDFactoryImpl();
        }
        return transactionIDFactory;
    }

    /**
     * {@inheritDoc}
     */
    public TransactionID createTransactionID() {
        return get().createTransactionID();
    }

    /**
     * {@inheritDoc}
     */
    public TransactionID restoreTransactionID(TransactionIDSerializedForm serializedForm) {
        return get().restoreTransactionID(serializedForm);
    }

    /**
     * {@inheritDoc}
     */
    public XidTransactionID createXidTransactionID(Xid xid) {
        return get().createXidTransactionID(xid);
    }

    /**
     * {@inheritDoc}
     */
    public XidTransactionID restoreXidTransactionID(XidTransactionIDSerializedForm serializedForm) {
        return get().restoreXidTransactionID(serializedForm);
    }

}
