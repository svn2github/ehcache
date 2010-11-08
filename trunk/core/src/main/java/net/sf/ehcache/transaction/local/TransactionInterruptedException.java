package net.sf.ehcache.transaction.local;

/**
 * @author lorban
 */
public class TransactionInterruptedException extends TransactionException {

    public TransactionInterruptedException(String message) {
        super(message);
    }
}
