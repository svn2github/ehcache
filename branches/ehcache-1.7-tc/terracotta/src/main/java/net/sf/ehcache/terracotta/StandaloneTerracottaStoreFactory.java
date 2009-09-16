package net.sf.ehcache.terracotta;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.StoreFactory;

public class StandaloneTerracottaStoreFactory implements StoreFactory {

	public Store create(Ehcache cache) {
		throw new UnsupportedOperationException();
	}

}
