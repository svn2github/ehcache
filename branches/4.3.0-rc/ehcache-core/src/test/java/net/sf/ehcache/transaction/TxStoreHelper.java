package net.sf.ehcache.transaction;

import net.sf.ehcache.store.Store;

/**
 * @author Ludovic Orban
 */
public class TxStoreHelper {

    public static Store getUnderlyingStore(AbstractTransactionStore abstractTransactionStore) {
        Store store = abstractTransactionStore.underlyingStore;
        while (store instanceof AbstractTransactionStore) {
            store = ((AbstractTransactionStore)store).underlyingStore;
        }
        return store;
    }
}
