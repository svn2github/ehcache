/**
 *
 */
package net.sf.ehcache.googleappengine;

import net.sf.ehcache.jcache.JCache;
import net.sf.ehcache.jcache.JCacheManager;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation notes:
 *
 * We can not _get_ a Cache with the JSR 107 implementation, since it has no knowledge
 * of the underlying cache existance. Hence get() returns null, but when we want to create() we
 * get an exception since the cache does in fact already exist.
 *
 * JCacheManager is broken in that it can not get a JCacheFactory.
 * We must got the JSR107 route to create a Cache.
 *
 * Now, if it wasn't for near-API-compatibility with GAE, we should really go the Ehcache way...
 *
 * @author C&eacute;drik LIME
 */
public class CacheService {

	private CacheService() {
	}

	public static synchronized Cache createOrRegisterCache(String cacheName) throws CacheException {
		return createOrRegisterCache(cacheName, getDefaultEnv());
	}
	public static synchronized Cache createOrRegisterCache(String cacheName, Map env) throws CacheException {
		//CacheManager cacheManager = CacheManager.getInstance();
		JCacheManager cacheManager = JCacheManager.getInstance();
		//Cache cache = cacheManager.getCache(cacheName);
		Cache cache = cacheManager.getJCache(cacheName);
		if (cache == null) {
			env.put("name", cacheName);
			//FIXME the following code does not work. I suspect a Ehcache/Jcache lifecycle issue... Workaround: name all your caches in ehcache.xml
			//cache = cacheManager.getCacheFactory().createCache(env);
			cache = CacheManager.getInstance().getCacheFactory().createCache(env);
			//cacheManager.registerCache(cacheName, cache);
			cacheManager.addCache((JCache)cache);
		}
		return cache;
	}

	private static Map getDefaultEnv() {
		// Google App Engine does not need any environment, but ehcache 1.6 crashes if none given...
		Map env = new HashMap();
//		env.put("name", cacheName);
		env.put("maxElementsInMemory", "32760");
		env.put("memoryStoreEvictionPolicy", "LRU");//LRU, LFU, FIFO
		env.put("overflowToDisk", "false");
		env.put("eternal", "true");
		env.put("timeToLiveSeconds", "0");
		env.put("timeToIdleSeconds", "0");
		env.put("diskPersistentSeconds", "false");
		return env;
	}
}
