package net.sf.ehcache.transaction;

/**
 * @author Ludovic Orban
 */
public interface TransactionID {

    boolean isDecisionCommit();

    void markAsCommit(boolean commit);

}
