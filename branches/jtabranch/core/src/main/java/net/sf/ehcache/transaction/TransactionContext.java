package net.sf.ehcache.transaction;

import java.util.List;

import javax.transaction.Transaction;

/**
 * @author Alex Snaps
 */
public interface TransactionContext {

    void addCommand(StoreWriteCommand storeWriteCommand);

    List<StoreWriteCommand> getCommands();
    
    Transaction getTransaction();

}
