/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.transaction.xa;

import javax.transaction.Transaction;
import javax.transaction.xa.Xid;

import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.TransactionContext;

/**
 * The EhcacheXAStore is storing XA related data for a Transaction Cache instance on behalf of
 * {@link net.sf.ehcache.transaction.xa.EhcacheXAResource EhcacheXAResource}:
 * <ul>
 * <li>Maps {@link javax.transaction.xa.Xid Xid} to their {@link javax.transaction.Transaction Transaction}
 * <li>Stores {@link net.sf.ehcache.transaction.TransactionContext TransactionContext} for all Transactions 
 * <li>Tracks "checked out" versions of keys by active transaction, in order to provide an optimistic locking strategy
 * <li>Stores in a persistent manner prepared Transaction data
 * <li>Tracks versioning information on keys which are "in commit" phase (prepared, yet not commited yet)
 * </ul>
 *
 * <p>
 * Mapping Xid to Transaction is required as the TransactionManager will only ever reference {@link javax.transaction.xa.Xid Xid},
 * while the {@link EhcacheXAResource} will only get access to the current {@link javax.transaction.Transaction Transaction};
 * <p>
 * Based on that, during the UserTransaction, the {@link net.sf.ehcache.transaction.TransactionContext TransactionContext} will store the
 * {@link net.sf.ehcache.transaction.Command Commands} for the current Transaction. While on
 * {@link javax.transaction.xa.XAResource#prepare(javax.transaction.xa.Xid) prepare},
 * {@link javax.transaction.xa.XAResource#commit(javax.transaction.xa.Xid, boolean)} commit},
 * {@link javax.transaction.xa.XAResource#rollback(javax.transaction.xa.Xid)} rollback} and other JTA
 * {@link javax.transaction.xa.XAResource XAResource} operations, the
 * {@link net.sf.ehcache.transaction.xa.EhcacheXAResource EhcacheXAResource} will get data back based on the Xid;
 * <p>
 * When an operation on the Cache involves a key, the EhcacheXAStore will track version information on that key. Version information isn't
 * stored at the {@link net.sf.ehcache.Element} level, but rather in an independent store to minimize the impact on memory (only Element in
 * use will be versioned, Element "only" referenced by the Cache are not);
 * <p>
 * When the {@link net.sf.ehcache.transaction.xa.EhcacheXAResource EhcacheXAResource} has successfully prepared a Transaction, it will ask
 * EhcacheXAStore to save that data in a "safe" and persistent place, in case of failure;
 * <p>
 * The previous version of keys to be updated are "moved" to a read-only oldVersionStore, while the key on the underlying store is
 * write-locked by the {@link EhcacheXAResource}. That oldVersionStore will always be accessed first, for read operations, providing
 * non-blocking reads on "in-commit" phase keys.
 *
 * @author Alex Snaps
 * @author Nabib El-Rahman
 */
public interface EhcacheXAStore {

    /**
     * Stores the mapping of Xid to Transaction
     * @param xid the xid
     * @param transaction the transaction matching that xid
     * @return potentially returns a previously stored xid for that transaction, otherwise null
     */
    Xid storeXid2Transaction(Xid xid, Transaction transaction);

    /**
     * Creates ans stores a new TransactionContext for a given Transaction
     * @param txn the JTA Transaction
     * @return a TransactionContext for that Transaction
     */
    TransactionContext createTransactionContext(Transaction txn);

    /**
     * Gets a stored TransactionContext for a given Transaction
     * @param xid The Xid of the Transaction
     * @return the matching TransactionContext
     */
    TransactionContext getTransactionContext(Xid xid);

    /**
     * Gets a stored TransactionContext for a given Transaction
     * @param txn the Transaction
     * @return the matching TransactionContext
     */
    TransactionContext getTransactionContext(Transaction txn);

    /**
     * Checks in changes to a key
     * @param key the key of the affected Element in the Store
     * @param xid the Xid of the Transaction executing the change
     * @param readOnly whether the command modified the underlying Store
     */
    void checkin(Object key, Xid xid, boolean readOnly);

    /**
     * Checks a version for en Element out of the store
     * @param key the key to the Element in the store
     * @param xid the Xid of the Transaction reading
     * @return the version of the element
     */
    long checkout(Object key, Xid xid);

    /**
     * Checks whether a command can safely be executed against the store, depending on the version lock
     * @param command the Command
     * @return true if safe, false if cannot be applied anymore
     */
    boolean isValid(VersionAwareCommand command);

    /**
     * Save the Transaction's data as being prepared
     * @param xid the Xid of the Transaction
     * @param context the context with the transaction data 
     */
    void prepared(Xid xid, PreparedContext context);
    
    /**
     * Return a newly created prepare context if none exist.
     * @return a new PreparedContext
     */
    PreparedContext createPreparedContext();

    /**
     * Lists prepared, yet not commited Xids
     * @return array of uncommited, yet prepared xids
     */
    Xid[] getPreparedXids();

    /**
     * Gets a PreparedContext from a persistent store for a previously prepared Transaction
     * @param xid The Xid of the Transaction
     * @return the Prepared context for the Transaction, or null
     */
    public PreparedContext getPreparedContext(Xid xid);
    /**
     * Getter to the underlying store
     * @return the store
     */
    Store getUnderlyingStore();

    /**
     * Getter to the guarding read-only oldVersionStore
     * @return the oldVersionStore
     */
    Store getOldVersionStore();

    /**
     * Checks whether a Transaction is prepared
     * @param xid the Xid of the Transaction
     * @return true if prepared
     */
    boolean isPrepared(Xid xid);

    /**
     * Removes stored data of a given Xid
     * @param xid the Xid of the Transaction
     */
    void removeData(Xid xid);
}
