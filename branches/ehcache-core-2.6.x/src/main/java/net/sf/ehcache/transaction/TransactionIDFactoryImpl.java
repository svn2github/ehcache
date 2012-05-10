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

import net.sf.ehcache.transaction.xa.XidTransactionID;
import net.sf.ehcache.transaction.xa.XidTransactionIDImpl;

import javax.transaction.xa.Xid;

/**
 * A TransactionIDFactory implementation with uniqueness across a single JVM

 * @author Ludovic Orban
 */
public class TransactionIDFactoryImpl implements TransactionIDFactory {

    /**
     * Create a new TransactionIDFactory
     */
    public TransactionIDFactoryImpl() {
        //
    }

    /**
     * {@inheritDoc}
     */
    public TransactionID createTransactionID() {
        return new TransactionIDImpl();
    }

    /**
     * {@inheritDoc}
     */
    public TransactionID restoreTransactionID(TransactionIDSerializedForm serializedForm) {
        throw new UnsupportedOperationException("unclustered transaction IDs are directly deserializable!");
    }

    /**
     * {@inheritDoc}
     */
    public XidTransactionID createXidTransactionID(Xid xid) {
        return new XidTransactionIDImpl(xid);
    }

    /**
     * {@inheritDoc}
     */
    public XidTransactionID restoreXidTransactionID(XidTransactionIDSerializedForm serializedForm) {
        throw new UnsupportedOperationException("unclustered transaction IDs are directly deserializable!");
    }

}
