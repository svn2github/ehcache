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

import java.util.Set;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.transaction.xa.XidTransactionID;

import javax.transaction.xa.Xid;

/**
 * A factory for transaction IDs. Generated transaction ID's must be unique to the entire
 * cluster-wide CacheManager.
 *
 * @author Ludovic Orban
 */
public interface TransactionIDFactory {

    /**
     * Create a unique transaction ID
     *
     * @return a transaction ID
     */
    TransactionID createTransactionID();

    /**
     * Restore a transaction ID from its serialized form
     *
     * @param serializedForm the TransactionID serialized form
     * @return the restored TransactionID
     */
    TransactionID restoreTransactionID(TransactionIDSerializedForm serializedForm);

    /**
     * Create a transaction ID based on a XID for uniqueness
     *
     * @param xid the XID
     * @return a transaction ID
     */
    XidTransactionID createXidTransactionID(Xid xid, Ehcache cache);

    /**
     * Restore a XID transaction ID from its serialized form
     *
     * @param serializedForm the XidTransactionID serialized form
     * @return the restored XidTransactionID
     */
    XidTransactionID restoreXidTransactionID(XidTransactionIDSerializedForm serializedForm);

    /**
     * Mark that this transaction's decision is commit
     *
     * @param transactionID transaction to be marked
     */
    void markForCommit(TransactionID transactionID);

    /**
     * Mark this transaction ID for rollback
     */
    void markForRollback(XidTransactionID transactionID);

    /**
     * Check if the given transaction should be committed or not
     *
     * @param transactionID transaction to be queried
     * @return true if the transaction should be committed
     */
    boolean isDecisionCommit(TransactionID transactionID);

    /**
     * Clear this transaction's state representation.
     *
     * @param transactionID transaction to be cleared
     */
    void clear(TransactionID transactionID);

    /**
     * Get the set of all XID transactions of a cache.
     *
     * @return the set of transactions
     */
    Set<XidTransactionID> getAllXidTransactionIDsFor(Ehcache cache);

    /**
     * Get the set of all known transactions.
     *
     * @return the set of transactions
     */
    Set<TransactionID> getAllTransactionIDs();

    /**
     * Return {@code true} if the factory state is persistent (survives JVM restart).
     * <p>
     * This is a tri-state return:
     * <ul>
     *   <li>{@code Boolean.TRUE} : persistent</li>
     *   <li>{@code Boolean.FALSE} : not persistent</li>
     *   <li>{@code null} : not yet determined</li>
     * </ul>
     *
     * @return {@code true} is state is persistent
     */
    Boolean isPersistent();

    /**
     * Check if the transaction ID expired, ie: that the transaction died abnormally
     *
     * @return true if the transaction ID expired and should be cleaned up, false otherwise
     */
    boolean isExpired(TransactionID transactionID);

}
