package net.sf.ehcache.transaction;

import javax.transaction.Transaction;

import net.sf.ehcache.Element;

/**
 * @author Alex Snaps
 */
public interface TransactionContext {

    void addCommand(Command command, Element element);
    
    Transaction getTransaction();

}
