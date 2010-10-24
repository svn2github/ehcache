package net.sf.ehcache.transaction.local;

import net.sf.ehcache.transaction.TransactionID;

/**
 * @author Ludovic Orban
 */
public interface TransactionIDFactory {

    TransactionID createTransactionID();

}
