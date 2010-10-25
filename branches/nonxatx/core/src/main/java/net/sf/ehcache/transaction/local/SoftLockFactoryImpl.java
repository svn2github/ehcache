package net.sf.ehcache.transaction.local;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

/**
 * @author Ludovic Orban
 */
public class SoftLockFactoryImpl implements SoftLockFactory {

    public SoftLockFactoryImpl() {
        //
    }

    public SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement, Element oldElement) {
        return new ReadCommittedSoftLockImpl(transactionID, key, newElement, oldElement);
    }

}
