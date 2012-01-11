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
package net.sf.ehcache.transaction.xa;

import javax.transaction.xa.Xid;

/**
 * @author Ludovic Orban
 */
public final class XidTransactionIDImpl implements XidTransactionID {

    /**
     * The decision types a XID transaction ID can be in
     */
    private static enum Decision {
        IN_DOUBT,
        COMMIT,
        ROLLBACK
    }

    private final SerializableXid xid;
    private volatile Decision decision = Decision.IN_DOUBT;

    /**
     * Constructor
     * @param xid a XID
     */
    public XidTransactionIDImpl(Xid xid) {
        this.xid = new SerializableXid(xid);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDecisionCommit() {
        return decision.equals(Decision.COMMIT);
    }

    /**
     * {@inheritDoc}
     */
    public void markForCommit() {
        if (decision.equals(Decision.ROLLBACK)) {
            throw new IllegalStateException(this + " already marked for rollback, cannot re-mark it for commit");
        }
        this.decision = Decision.COMMIT;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDecisionRollback() {
        return decision.equals(Decision.ROLLBACK);
    }

    /**
     * {@inheritDoc}
     */
    public void markForRollback() {
        if (decision.equals(Decision.COMMIT)) {
            throw new IllegalStateException(this + " already marked for commit, cannot re-mark it for rollback");
        }
        this.decision = Decision.ROLLBACK;
    }

    /**
     * {@inheritDoc}
     */
    public Xid getXid() {
        return xid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof XidTransactionIDImpl) {
            XidTransactionIDImpl otherId = (XidTransactionIDImpl) obj;
            return xid.equals(otherId.xid);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int hashCode() {
        return xid.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Unclustered " + xid + " (decision: " + decision + ")";
    }
}
