package net.sf.ehcache.openjpa.datacache;

import net.sf.ehcache.Ehcache;

import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.datacache.DataCacheManager;
import org.apache.openjpa.datacache.DataCacheManagerImpl;

public class EhCacheDataCacheManager extends DataCacheManagerImpl implements
		DataCacheManager {

	public DataCache getDataCache(String name, boolean create) {
		DataCache cache = super.getDataCache(name, create);
		if (cache == null) {
			cache = getSystemDataCache();
		}
		return cache;
	}

	public EhCacheDataCache getSystemDataCache() {
		return ((EhCacheDataCache) super.getSystemDataCache());
	}
	
    public Ehcache getEhCache(Class cls) {
        return getSystemDataCache().findCache(cls);
     }
}