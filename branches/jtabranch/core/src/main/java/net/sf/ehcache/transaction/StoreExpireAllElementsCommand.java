package net.sf.ehcache.transaction;

import net.sf.ehcache.store.Store;

/**
 * @author Alex Snaps
 */
public class StoreExpireAllElementsCommand implements StoreWriteCommand {
    
    public void execute(final Store store) {
        store.expireElements();
    }
   
}
