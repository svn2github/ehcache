package org.terracotta.modules.ehcache.transaction.xa;

import net.sf.ehcache.transaction.xa.XidTransactionID;

import javax.transaction.xa.Xid;

/**
 * @author Ludovic Orban
 */
public class ClusteredXidTransactionID implements XidTransactionID {

    private static enum Decision {
        IN_DOUBT,
        COMMIT,
        ROLLBACK
    }

    private final Xid xid;
    private volatile Decision decision = Decision.IN_DOUBT;

    public ClusteredXidTransactionID(Xid xid) {
        this.xid = new XidClustered(xid);
    }

    // autolocked in config
    public synchronized boolean isDecisionCommit() {
        return decision.equals(Decision.COMMIT);
    }

    // autolocked in config
    public synchronized void markForCommit() {
        if (decision.equals(Decision.ROLLBACK)) {
            throw new IllegalStateException(this + " already marked for rollback, cannot re-mark it for commit");
        }
        this.decision = Decision.COMMIT;
    }

    // autolocked in config
    public synchronized boolean isDecisionRollback() {
        return decision.equals(Decision.ROLLBACK);
    }

    // autolocked in config
    public synchronized void markForRollback() {
        if (decision.equals(Decision.COMMIT)) {
            throw new IllegalStateException(this + " already marked for commit, cannot re-mark it for rollback");
        }
        this.decision = Decision.ROLLBACK;
    }

    public Xid getXid() {
        return xid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof ClusteredXidTransactionID) {
            ClusteredXidTransactionID otherId = (ClusteredXidTransactionID) obj;
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
        return "Clustered [" + xid + "] (decision: " + decision + ")";
    }
}
