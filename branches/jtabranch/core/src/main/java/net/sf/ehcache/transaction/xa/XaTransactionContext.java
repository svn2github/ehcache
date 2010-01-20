package net.sf.ehcache.transaction.xa;

import java.io.Serializable;
import java.util.*;

import javax.transaction.Transaction;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.Command;
import net.sf.ehcache.transaction.TransactionContext;

/**
 * @author Alex Snaps
 */
public class XaTransactionContext implements TransactionContext {

    private final Set<Object>               removedKeys  = new HashSet<Object>();
    private final Set<Object>               addedKeys    = new HashSet<Object>();
    private final List<VersionAwareWrapper> commands     = new ArrayList<VersionAwareWrapper>();
    private final Transaction               txn;
    private final EhCacheXAResourceImpl     resourceImpl;
    private       int                       sizeModifier;

    public XaTransactionContext(Transaction txn, EhCacheXAResourceImpl resourceImpl) {
        this.txn = txn;
        this.resourceImpl = resourceImpl;
    }

    public Transaction getTransaction() {
        return this.txn;
    }

    public Element get(Object key) {
        Element element = null;
        for(VersionAwareWrapper command : commands) {
            if(command.isPut(key)) {
                element = command.getElement();
            } else if(command.isRemove(key)) {
                element = null;
            }
        }
        return element;
    }

    public boolean isRemoved(Object key) {
        return removedKeys.contains(key);
    }

    public Collection getAddedKeys() {
        return Collections.unmodifiableSet(addedKeys);
    }

    public Collection getRemovedKeys() {
        return Collections.unmodifiableSet(removedKeys);
    }


    public void addCommand(final Command command, final Element element) {

        VersionAwareWrapper wrapper = null;
        if(element != null) {
            long version = resourceImpl.checkout(element, txn);
            wrapper = new VersionAwareWrapper(command, version, element);
        } else {
            wrapper = new VersionAwareWrapper(command);
        }

        if(element != null) {
            Serializable key = element.getKey();
            if(command.isPut(key)) {
                boolean removed = removedKeys.remove(key);
                boolean added = addedKeys.add(key);
                if(removed || added && !resourceImpl.getStore().containsKey(element.getKey())) {
                    sizeModifier++;
                }
            } else if(command.isRemove(key)) {
                removedKeys.add(key);
                if(addedKeys.remove(key) || resourceImpl.getStore().containsKey(element.getKey())) {
                    sizeModifier--;
                }
            }
        }
        commands.add(wrapper);
    }

    public List<VersionAwareWrapper> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public int getSizeModifier() {
        return sizeModifier;
    }
}
