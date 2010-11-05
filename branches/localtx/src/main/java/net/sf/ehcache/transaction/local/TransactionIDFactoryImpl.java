package net.sf.ehcache.transaction.local;

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
