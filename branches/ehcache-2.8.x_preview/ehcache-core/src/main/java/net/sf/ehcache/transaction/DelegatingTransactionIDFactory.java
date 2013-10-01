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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.FeaturesManager;
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.terracotta.TerracottaClient;
import net.sf.ehcache.transaction.xa.XidTransactionID;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.xa.Xid;

/**
 * A TransactionIDFactory implementation with delegates calls to either a clustered
 * or non-clustered factory
 *
 * @author Ludovic Orban
 */
public class DelegatingTransactionIDFactory implements TransactionIDFactory {

    private final FeaturesManager featuresManager;
    private final TerracottaClient terracottaClient;
    private final String cacheManagerName;
    private volatile ClusteredInstanceFactory clusteredInstanceFactory;
    private volatile AtomicReference<TransactionIDFactory> transactionIDFactory = new AtomicReference<TransactionIDFactory>();

    /**
     * Create a new DelegatingTransactionIDFactory
     *
     * @param terracottaClient a terracotta client
     * @param cacheManagerName the name of the cache manager which creates this.
     */
    public DelegatingTransactionIDFactory(FeaturesManager featuresManager, TerracottaClient terracottaClient, String cacheManagerName) {
        this.featuresManager = featuresManager;
        this.terracottaClient = terracottaClient;
        this.cacheManagerName = cacheManagerName;
    }

    private TransactionIDFactory get() {
        ClusteredInstanceFactory cif = terracottaClient.getClusteredInstanceFactory();
        if (cif != null && cif != this.clusteredInstanceFactory) {
            this.transactionIDFactory.set(cif.createTransactionIDFactory(UUID.randomUUID().toString(), cacheManagerName));
            this.clusteredInstanceFactory = cif;
        }

        if (transactionIDFactory.get() == null) {
            TransactionIDFactory constructed;
            if (featuresManager == null) {
                constructed = new TransactionIDFactoryImpl();
            } else {
                constructed = featuresManager.createTransactionIDFactory();
            }
            if (transactionIDFactory.compareAndSet(null, constructed)) {
                return constructed;
            } else {
                return transactionIDFactory.get();
            }
        } else {
            return transactionIDFactory.get();
        }
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
    public XidTransactionID createXidTransactionID(Xid xid, Ehcache cache) {
        return get().createXidTransactionID(xid, cache);
    }

    /**
     * {@inheritDoc}
     */
    public XidTransactionID restoreXidTransactionID(XidTransactionIDSerializedForm serializedForm) {
        return get().restoreXidTransactionID(serializedForm);
    }

    @Override
    public void markForCommit(TransactionID transactionID) {
        get().markForCommit(transactionID);
    }

    @Override
    public void markForRollback(XidTransactionID transactionID) {
        get().markForRollback(transactionID);
    }

    @Override
    public boolean isDecisionCommit(TransactionID transactionID) {
        return get().isDecisionCommit(transactionID);
    }

    @Override
    public void clear(TransactionID transactionID) {
        get().clear(transactionID);
    }

    @Override
    public Set<XidTransactionID> getAllXidTransactionIDsFor(Ehcache cache) {
        return get().getAllXidTransactionIDsFor(cache);
    }

    @Override
    public Set<TransactionID> getAllTransactionIDs() {
        return get().getAllTransactionIDs();
    }

    @Override
    public boolean isExpired(TransactionID transactionID) {
        return get().isExpired(transactionID);
    }

    @Override
    public Boolean isPersistent() {
        if (transactionIDFactory.get() == null) {
            return null;
        } else {
            return get().isPersistent();
        }
    }
}
