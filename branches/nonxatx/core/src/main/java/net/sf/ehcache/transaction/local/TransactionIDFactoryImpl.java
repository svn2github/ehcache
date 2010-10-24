package net.sf.ehcache.transaction.local;

import net.sf.ehcache.transaction.TransactionID;

/**
 * @author Ludovic Orban
 */
public class TransactionIDFactoryImpl implements TransactionIDFactory {

    public TransactionIDFactoryImpl() {
        //
    }

    public TransactionID createTransactionID() {
        return new TransactionIDImpl();
    }

}
