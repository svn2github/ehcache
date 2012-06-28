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
package net.sf.ehcache.transaction;

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
    XidTransactionID createXidTransactionID(Xid xid);

    /**
     * Restore a XID transaction ID from its serialized form
     *
     * @param serializedForm the XidTransactionID serialized form
     * @return the restored XidTransactionID
     */
    XidTransactionID restoreXidTransactionID(XidTransactionIDSerializedForm serializedForm);
}
