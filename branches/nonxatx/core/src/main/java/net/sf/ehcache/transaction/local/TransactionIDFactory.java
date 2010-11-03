package net.sf.ehcache.transaction.local;

import net.sf.ehcache.transaction.TransactionID;

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
