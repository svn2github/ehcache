package net.sf.ehcache.transaction.local;

import net.sf.ehcache.CacheException;

/**
 * @author lorban
 */
public class TransactionException extends CacheException {

    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
