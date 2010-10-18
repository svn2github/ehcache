package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

/**
 * @author Ludovic Orban
 */
public class SoftLockFactoryImpl implements SoftLockFactory {

    public SoftLockFactoryImpl() {
        //
    }

    public SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement) {
        return new SoftLockImpl(transactionID, key, newElement);
    }

}
