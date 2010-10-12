package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.CacheException;

/**
 * @author lorban
 */
public class TransactionException extends CacheException {

    public TransactionException(String message) {
        super(message);
    }
    
}
