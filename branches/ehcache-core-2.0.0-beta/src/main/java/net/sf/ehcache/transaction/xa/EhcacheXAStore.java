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
 * @author Alex Snaps
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
     */
    void prepared(Xid xid);

    /**
     * Lists prepared, yet not commited Xids
     * @return array of uncommited, yet prepared xids
     */
    Xid[] getPreparedXids();

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
