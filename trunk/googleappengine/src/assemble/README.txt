This Ehcache module is an integration layer to Google AppEngine.

References
==========
http://ehcache.org/documentation/googleappengine.html
http://code.google.com/appengine/docs/java/memcache/


Migration: Ehcache (standalone) to Ehcache (Google AppEngine)
=========
Apply the steps "ehcache.xml" and "web.xml" below.


Migration: Google AppEngine (MemcacheService or javax.cache) to Ehcache
=========
If you are using the MemcacheService API, you are (nearly) on your own. First migrate to javax.cache, or directly to Ehcache API (in which case you can skip to the next paragraph).
That said, the MemcacheService API is quite similar to the javax.cache API.

Is you are using the javax.cache API, migration is very simple:
* replace all
import javax.cache.
with
import net.sf.jsr107cache.

Cache creation: from javax.cache to jsr107cache
--------------
javax.cache does not work with Ehcache + JRS107.
Cache creation goes from:

	public static Cache createOrRegisterCache(String cacheName) throws CacheException {
		return createOrRegisterCache(cacheName, getDefaultEnv());
	}
	public static synchronized Cache createOrRegisterCache(String cacheName, Map props) throws CacheException {
		CacheManager cacheManager = CacheManager.getInstance();
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			cache = cacheManager.getCacheFactory().createCache(props);
			cacheManager.registerCache(cacheName, cache);
		}
		return cache;
	}
	/**
	 * @see "http://code.google.com/appengine/docs/java/memcache/usingjcache.html"
	 */
	private static Map getDefaultEnv() {
		//Map<Object, Object> props = new HashMap<Object, Object>();//Collections.emptyMap()
		//props.put(GCacheFactory.EXPIRATION_DELTA, Integer.valueOf(3600));//seconds
		//props.put(MemcacheService.SetPolicy.ADD_ONLY_IF_NOT_PRESENT, Boolean.TRUE);
		//return props;
		return Collections.emptyMap();
	}

to:
(see CacheService.java)

ehcache.xml
-----------
* Remove all <diskStore> elements
* Remove all <cacheManagerPeerProviderFactory> elements
* Remove all <cacheManagerPeerListenerFactory> elements
* Remove all Cache Replication elements
* Remove all Cluster Bootstrapping elements
* Add to all distributed <cache> elements:
	<cacheEventListenerFactory
		class="net.sf.ehcache.googleappengine.AppEngineCacheEventListenerFactory"
		properties="param1=200"/>
		<cacheLoaderFactory class="net.sf.ehcache.googleappengine.AppEngineCacheLoaderFactory"
			properties="param1=value1,param2=10"/>
TODO

web.xml
-------
See web.xml for the code to copy in your application.
* Add the ehcache-web ShutdownListener, if not already present
* (Optional) add the CacheStatisticsServlet if you want to display some cache statistics


TODO
====
Implement a CacheManager Event Listeners that will add a AppEngineCacheLoader on newly created caches (if it does not exist).
From JavaDoc:
	caches that are part of the initial configuration are not considered "changes".
	It is only caches added or removed beyond the initial config.
http://ehcache.org/documentation/cachemanager_event_listeners.html
cache API:
public List<CacheLoader> getRegisteredCacheLoaders()
public void registerCacheLoader(CacheLoader cacheLoader)

See also CacheExtension.
From JavaDoc:
	Because a CacheExtension holds a reference to a Cache, the CacheExtension can do things
	such as registering a CacheEventListener or even a CacheManagerEventListener, all from
	within a CacheExtension, creating more opportunities for customisation.


TODO
====
Asynchronous calls for updates (I don't think this is possible)


TODO
====
Allow to pass parameters to MemCache (e.g. via the "properties" XML attribute of the cache*Factory).
