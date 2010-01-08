package net.sf.ehcache.transaction;

import java.util.List;

import javax.transaction.Transaction;
import javax.transaction.xa.Xid;

/**
 * @author Alex Snaps
 */
public interface TransactionContext {

    void addCommand(StoreWriteCommand storeWriteCommand);

    List<StoreWriteCommand> getCommands();

}
