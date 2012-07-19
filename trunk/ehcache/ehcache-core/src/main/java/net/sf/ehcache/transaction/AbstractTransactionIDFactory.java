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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.transaction.xa.XidTransactionID;

/**
 * An abstract map backed transaction id factory.
 *
 * @author Chris Dennis
 */
public abstract class AbstractTransactionIDFactory implements TransactionIDFactory {

    /**
     * Return the map of transaction states.
     *
     * @return the map of transaction states
     */
    protected abstract ConcurrentMap<TransactionID, Decision> getTransactionStates();

    /**
     * {@inheritDoc}
     */
    @Override
    public void markForCommit(TransactionID transactionID) {
        while (true) {
            Decision current = getTransactionStates().get(transactionID);
            if (current == null) {
                throw new TransactionIDNotFoundException("transaction state of transaction ID [" + transactionID + "] already cleaned up");
            }
            switch (current) {
                case IN_DOUBT:
                    if (getTransactionStates().replace(transactionID, Decision.IN_DOUBT, Decision.COMMIT)) {
                        return;
                    }
                    break;
                case ROLLBACK:
                    throw new IllegalStateException(this + " already marked for rollback, cannot re-mark it for commit");
                case COMMIT:
                    return;
                default:
                    throw new AssertionError("unreachable code");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markForRollback(XidTransactionID transactionID) {
        while (true) {
            Decision current = getTransactionStates().get(transactionID);
            if (current == null) {
                throw new TransactionIDNotFoundException("transaction state of transaction ID [" + transactionID + "] already cleaned up");
            }
            switch (current) {
                case IN_DOUBT:
                    if (getTransactionStates().replace(transactionID, Decision.IN_DOUBT, Decision.ROLLBACK)) {
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
        return Decision.COMMIT.equals(getTransactionStates().get(transactionID));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear(TransactionID transactionID) {
        getTransactionStates().remove(transactionID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<XidTransactionID> getAllXidTransactionIDsFor(Ehcache cache) {
        String cacheName = cache.getName();
        Set<XidTransactionID> result = new HashSet<XidTransactionID>();

        for (TransactionID id : getTransactionStates().keySet()) {
            if (id instanceof XidTransactionID) {
                XidTransactionID xid = (XidTransactionID) id;
                if (cacheName.equals(xid.getCacheName())) {
                    result.add(xid);
                }
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<TransactionID> getAllTransactionIDs() {
        return Collections.unmodifiableSet(getTransactionStates().keySet());
    }
}
