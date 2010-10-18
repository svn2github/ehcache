package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

/**
 * @author Ludovic Orban
 */
public interface SoftLockFactory {

    SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement);

}
