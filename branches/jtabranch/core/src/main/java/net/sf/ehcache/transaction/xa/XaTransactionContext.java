package net.sf.ehcache.transaction.xa;

import net.sf.ehcache.transaction.StoreWriteCommand;
import net.sf.ehcache.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.transaction.Transaction;

/**
* @author Alex Snaps
*/
public class XaTransactionContext implements TransactionContext {

    private final List<StoreWriteCommand> writeCommands = new ArrayList<StoreWriteCommand>();
    
    private final Transaction txn;
    
    public XaTransactionContext(Transaction txn) {
        this.txn = txn;
    }
    
    public Transaction getTransaction() {
        return this.txn;
    }
    

    public void addCommand(final StoreWriteCommand storeWriteCommand) {
        writeCommands.add(storeWriteCommand);
    }

    public List<StoreWriteCommand> getCommands() {
        return Collections.unmodifiableList(writeCommands);
    }
}
