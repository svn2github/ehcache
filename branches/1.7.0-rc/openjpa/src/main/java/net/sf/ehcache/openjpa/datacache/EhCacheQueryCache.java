package net.sf.ehcache.openjpa.datacache;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.openjpa.datacache.QueryKey;
import org.apache.openjpa.datacache.QueryResult;
import org.apache.openjpa.datacache.AbstractQueryCache;
import org.apache.openjpa.datacache.QueryCache;

public class EhCacheQueryCache extends AbstractQueryCache implements QueryCache {

    protected boolean useDefaultForUnnamedCaches = true;
    protected String cacheName = "openjpa-querycache";
    protected ReentrantLock lock = new ReentrantLock();
    
	@Override
	protected void clearInternal() {
		getOrCreateCache(cacheName).removeAll();
	}

	@Override
	protected QueryResult getInternal(QueryKey qk) {
		Ehcache cache = getOrCreateCache(cacheName);
		Element result = cache.get(qk);
		if(result==null){
			return null;
		}else{
			return (QueryResult) result.getValue();
		}
	}

	@Override
	protected Collection keySet() {
		Ehcache cache = getOrCreateCache(cacheName);
		return cache.getKeys();
	}

	@Override
	protected boolean pinInternal(QueryKey qk) {
		return false;
	}

	@Override
	protected QueryResult putInternal(QueryKey qk, QueryResult oids) {
		Ehcache cache = getOrCreateCache(cacheName);
		Element element = new Element(qk,oids);
		cache.put(element);
		return oids;
	}

	@Override
	protected QueryResult removeInternal(QueryKey qk) {
		Ehcache cache = getOrCreateCache(cacheName);
		QueryResult queryResult  = getInternal(qk);
		cache.remove(qk);
		return queryResult;
	}

	@Override
	protected boolean unpinInternal(QueryKey qk) {
		return false;
	}

	public void writeLock() {
        lock.lock();
	}

	public void writeUnlock() {
        lock.unlock();
	}
	
    protected synchronized Ehcache getOrCreateCache(String name){
    	CacheManager cacheManager = CacheManager.getInstance();
        Ehcache ehCache = cacheManager.getEhcache(name);
        if(ehCache==null){
        	Cache cache = new Cache(name,1000,false,true,600,600);
        	cacheManager.addCache(cache);
        	ehCache = cacheManager.getEhcache(name);
        }
        return ehCache;
    }
}
