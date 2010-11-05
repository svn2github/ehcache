package net.sf.ehcache.transaction.local;

/**
 * @author lorban
 */
public class DeadLockException extends TransactionException {

    public DeadLockException(String message) {
        super(message);
    }

}
