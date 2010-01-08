package net.sf.ehcache.transaction;

import net.sf.ehcache.store.Store;

/**
 * @author Alex Snaps
 */
public interface StoreWriteCommand {

    void execute(Store store);
}
