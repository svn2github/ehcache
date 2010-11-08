package net.sf.ehcache.transaction.local;

/**
 * A transaction identifier
 * @author Ludovic Orban
 */
public interface TransactionID {

    /**
     * Check if this transaction should be committed or not
     * @return true of the transaction should be committed
     */
    boolean isDecisionCommit();

    /**
     * Mark that this transaction's decision is commit
     */
    void markForCommit();

}
