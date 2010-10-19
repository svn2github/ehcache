package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

/**
 * @author Ludovic Orban
 */
public interface SoftLock {

    Object getKey();

    Element getNewElement();

    void setNewElement(Element newElement);

    TransactionID getTransactionID();

    void lock();

    boolean tryLock(int transactionTimeout) throws InterruptedException;

    void unlock();

}
