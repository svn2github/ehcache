package net.sf.ehcache.terracotta;

import net.sf.ehcache.Cache;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.StoreFactory;

public class StandaloneTerracottaStoreFactory implements StoreFactory {

	public Store create(Cache cache) {
		throw new UnsupportedOperationException();
	}

}
