package net.sf.ehcache.transaction;

import javax.transaction.Transaction;

import net.sf.ehcache.Element;

import java.util.Collection;

/**
 * @author Alex Snaps
 */
public interface TransactionContext {

    void addCommand(Command command, Element element);
    
    Transaction getTransaction();

    Element get(Object key);

    boolean isRemoved(Object key);

    Collection getAddedKeys();

    Collection getRemovedKeys();

    int getSizeModifier();
}
