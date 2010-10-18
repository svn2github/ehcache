package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.transaction.TransactionID;

/**
 * @author Ludovic Orban
 */
public interface TransactionIDFactory {

    TransactionID createTransactionID();

}
