package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.CacheException;

import javax.transaction.SystemException;

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
