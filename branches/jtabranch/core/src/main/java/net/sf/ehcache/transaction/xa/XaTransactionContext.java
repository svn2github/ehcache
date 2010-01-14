package net.sf.ehcache.transaction.xa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.transaction.Transaction;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.Command;
import net.sf.ehcache.transaction.TransactionContext;

/**
 * @author Alex Snaps
 */
public class XaTransactionContext implements TransactionContext {

    private final List<VersionAwareWrapper> commands = new ArrayList<VersionAwareWrapper>();
    private final Transaction txn;
    private final EhCacheXAResourceImpl resourceImpl;

    public XaTransactionContext(Transaction txn, EhCacheXAResourceImpl resourceImpl) {
        this.txn = txn;
        this.resourceImpl = resourceImpl;
    }

    public Transaction getTransaction() {
        return this.txn;
    }
    

    public void addCommand(final Command command, final Element element) {
        
        VersionAwareWrapper wrapper = null;
        if(element != null) {
            long version = resourceImpl.checkout(element, txn);
            wrapper = new VersionAwareWrapper(command, version, element);
        } else {
            wrapper = new VersionAwareWrapper(command);
        }
        commands.add(wrapper);
    }

    public List<VersionAwareWrapper> getCommands() {
        return Collections.unmodifiableList(commands);
    }

}
