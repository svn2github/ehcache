package net.sf.ehcache.transaction;

import net.sf.ehcache.store.Store;

/**
 * @author Alex Snaps
 */
public class StoreRemoveAllCommand implements StoreWriteCommand {
    public void execute(final Store store) {
        store.removeAll();
    }

    public boolean isPut(Object key) {
        return false;
    }

    public boolean isRemove(Object key) {
        return true;
    }
}
