package net.sf.ehcache.transaction.xa;

import javax.transaction.xa.XAException;

/**
 * @author Alex Snaps
 */
public class EhcacheXAException extends XAException {

    public EhcacheXAException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode; 
    }

    public EhcacheXAException(String message, int errorCode, Throwable cause) {
        super(message);
        this.errorCode = errorCode;
        initCause(cause);
    }
}
