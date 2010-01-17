package net.sf.ehcache.transaction.xa;

import javax.transaction.xa.XAException;

/**
 * @author Alex Snaps
 */
public class EhCacheXAException extends XAException {

    public EhCacheXAException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode; 
    }

    public EhCacheXAException(String message, int errorCode, Throwable cause) {
        super(message);
        this.errorCode = errorCode;
        initCause(cause);
    }
}
