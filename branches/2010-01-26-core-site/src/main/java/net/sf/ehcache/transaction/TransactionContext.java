package net.sf.ehcache.transaction;

import java.util.Collection;
import java.util.List;

import javax.transaction.Transaction;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.xa.VersionAwareCommand;

/**
 * @author Alex Snaps
 */
public interface TransactionContext {

    void addCommand(Command command, Element element);
    
    Transaction getTransaction();
    
    public Element get(Object key);

    boolean isRemoved(Object key);

    Collection getAddedKeys();

    Collection getRemovedKeys();

    int getSizeModifier();
    
    List<VersionAwareCommand> getCommands();
}
