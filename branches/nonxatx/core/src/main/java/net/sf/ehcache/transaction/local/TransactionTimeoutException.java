package net.sf.ehcache.transaction.local;

/**
 * @author lorban
 */
public class TransactionTimeoutException extends TransactionException {

    public TransactionTimeoutException(String message) {
        super(message);
    }
}
