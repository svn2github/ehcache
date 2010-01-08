package net.sf.ehcache.transaction.xa;

import net.sf.ehcache.transaction.StoreWriteCommand;
import net.sf.ehcache.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
* @author Alex Snaps
*/
public class XaTransactionContext implements TransactionContext {

    private List<StoreWriteCommand> writeCommands = new ArrayList<StoreWriteCommand>();

    public void addCommand(final StoreWriteCommand storeWriteCommand) {
        writeCommands.add(storeWriteCommand);
    }

    public List<StoreWriteCommand> getCommands() {
        return Collections.unmodifiableList(writeCommands);
    }
}
