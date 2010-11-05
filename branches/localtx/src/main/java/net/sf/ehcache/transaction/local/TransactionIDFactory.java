package net.sf.ehcache.transaction.local;

/**
 * Factory for transaction IDs
 * @author Ludovic Orban
 */
public interface TransactionIDFactory {

    /**
     * Create a unique transaction ID
     * @return a transaction ID
     */
    TransactionID createTransactionID();

}
