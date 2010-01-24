package net.sf.ehcache.transaction;

import net.sf.ehcache.store.Store;

/**
 * @author Alex Snaps
 */
public class StoreExpireAllElementsCommand implements StoreWriteCommand {
    
    public boolean execute(final Store store) {
        store.expireElements();
        return true;
    }

    public boolean isPut(Object key) {
        return false;
    }

    public boolean isRemove(Object key) {
        return false;
    }

    public String getCommandName() {
        return Command.EXPIRE_ALL_ELEMENTS;
    }
}
