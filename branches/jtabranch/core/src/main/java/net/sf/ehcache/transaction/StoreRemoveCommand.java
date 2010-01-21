package net.sf.ehcache.transaction;

import net.sf.ehcache.store.Store;

/**
 * @author Alex Snaps
 */
public class StoreRemoveCommand implements StoreWriteCommand {

    private final Object key;

    public StoreRemoveCommand(final Object key) {
        this.key = key;
    }

    public boolean execute(final Store store) {
        return store.remove(key) != null;
    }

    public boolean isPut(Object key) {
        return false;
    }

    public boolean isRemove(Object key) {
        return this.key.equals(key);
    }

    public String getCommandName() {
        return Command.REMOVE;
    }
    
    
}
