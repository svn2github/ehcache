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

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.sf.ehcache.transaction.xa.XidTransactionID;
import net.sf.ehcache.transaction.xa.XidTransactionIDImpl;

import javax.transaction.xa.Xid;

/**
 * A TransactionIDFactory implementation with uniqueness across a single JVM

 * @author Ludovic Orban
 */
public class TransactionIDFactoryImpl implements TransactionIDFactory {

    private final ConcurrentMap<TransactionID, Decision> transactionStates = new ConcurrentHashMap<TransactionID, Decision>();

    /**
     * {@inheritDoc}
     */
    public TransactionID createTransactionID() {
        TransactionID id = new TransactionIDImpl();
        if (transactionStates.putIfAbsent(id, Decision.IN_DOUBT) == null) {
            return id;
        } else {
            throw new AssertionError();
        }
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
        XidTransactionID id = new XidTransactionIDImpl(xid);
        transactionStates.putIfAbsent(id, Decision.IN_DOUBT);
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public XidTransactionID restoreXidTransactionID(XidTransactionIDSerializedForm serializedForm) {
        throw new UnsupportedOperationException("unclustered transaction IDs are directly deserializable!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markForCommit(TransactionID transactionID) {
        while (true) {
            Decision current = transactionStates.get(transactionID);
            switch (current) {
                case IN_DOUBT:
                    if (transactionStates.replace(transactionID, Decision.IN_DOUBT, Decision.COMMIT)) {
                        return;
                    }
                    break;
                case ROLLBACK:
                    throw new IllegalStateException(this + " already marked for rollback, cannot re-mark it for commit");
                case COMMIT:
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markForRollback(XidTransactionID transactionID) {
        while (true) {
            Decision current = transactionStates.get(transactionID);
            switch (current) {
                case IN_DOUBT:
                    if (transactionStates.replace(transactionID, Decision.IN_DOUBT, Decision.ROLLBACK)) {
                        return;
                    }
                    break;
                case ROLLBACK:
                    return;
                case COMMIT:
                    throw new IllegalStateException(this + " already marked for commit, cannot re-mark it for rollback");
                default:
                    throw new AssertionError();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDecisionCommit(TransactionID transactionID) {
        return Decision.COMMIT.equals(transactionStates.get(transactionID));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<TransactionID> getInDoubtTransactionIDs() {
        Set<TransactionID> result = new HashSet<TransactionID>();

        for (Entry<TransactionID, Decision> e : transactionStates.entrySet()) {
            if (Decision.IN_DOUBT.equals(e.getValue())) {
                result.add(e.getKey());
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear(TransactionID transactionID) {
        transactionStates.remove(transactionID);
    }
}
