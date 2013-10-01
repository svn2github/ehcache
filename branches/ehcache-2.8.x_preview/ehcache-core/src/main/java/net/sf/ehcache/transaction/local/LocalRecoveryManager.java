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
package net.sf.ehcache.transaction.local;

import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.TransactionIDFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The local transactions mode recovery manager which is used to trigger full recovery of all
 * caches configured with local transaction mode.
 *
 * @author Ludovic Orban
 */
public class LocalRecoveryManager {

    private final TransactionIDFactory transactionIdFactory;
    private final List<LocalTransactionStore> localTransactionStores = new CopyOnWriteArrayList<LocalTransactionStore>();
    private volatile Set<TransactionID> previouslyRecoveredTransactionIDs = Collections.emptySet();

    /**
     * Create a LocalRecoveryManager instance
     * @param transactionIdFactory the TransactionIDFactory
     */
    public LocalRecoveryManager(TransactionIDFactory transactionIdFactory) {
        this.transactionIdFactory = transactionIdFactory;
    }

    /**
     * Register a LocalTransactionStore from the recovery manager
     * @param localTransactionStore the LocalTransactionStore
     */
    void register(LocalTransactionStore localTransactionStore) {
        localTransactionStores.add(localTransactionStore);
    }

    /**
     * Unregister a LocalTransactionStore from the recovery manager
     * @param localTransactionStore the LocalTransactionStore
     */
    void unregister(LocalTransactionStore localTransactionStore) {
        localTransactionStores.remove(localTransactionStore);
    }

    /**
     * Run recovery on all registered local transaction stores. The latter
     * are used internally by caches when they're configured with local transaction mode.
     * @return the set of recovered TransactionIDs
     */
    public Set<TransactionID> recover() {
        Set<TransactionID> recovered = new HashSet<TransactionID>();
        // first ask all stores to cleanup their soft locks
        for (LocalTransactionStore localTransactionStore : localTransactionStores) {
            recovered.addAll(localTransactionStore.recover());
        }
        // then clear the transaction ID
        for (TransactionID transactionId : recovered) {
            transactionIdFactory.clear(transactionId);
        }

        previouslyRecoveredTransactionIDs = recovered;
        return recovered;
    }

    /**
     * Get the set of transaction IDs collected by the previous recover() call
     * @return the set of previously recovered TransactionIDs
     */
    public Set<TransactionID> getPreviouslyRecoveredTransactionIDs() {
        return previouslyRecoveredTransactionIDs;
    }
}
