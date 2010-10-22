package net.sf.ehcache.transaction.nonxa;

/**
 * @author lorban
 */
public class DeadLockException extends TransactionException {

    public DeadLockException(String message) {
        super(message);
    }

}
