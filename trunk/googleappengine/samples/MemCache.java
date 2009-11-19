/**
 *
 */
package net.sf.ehcache.googleappengine;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;

import javax.cache.Cache;
import javax.cache.CacheManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author C&eacute;drik LIME
 * @deprecated this implementation uses javax.cache, which does not work with Ehcache + JRS107
 * It is here only to serve as an example on how to use {@code javax.cache}.
 */
@Deprecated
public final class MemCache {

	private MemCache() {
	}

	public static String getCacheName(Ehcache cache) {
		return cache.getName();
	}

	public static Cache createOrRegisterCache(String cacheName) throws CacheException {
		return createOrRegisterCache(cacheName, getDefaultEnv());
	}
	public static synchronized Cache createOrRegisterCache(String cacheName, Map props) throws CacheException {
		CacheManager cacheManager = CacheManager.getInstance();
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			try {
			fixProps(props);
			cache = cacheManager.getCacheFactory().createCache(props);
			cacheManager.registerCache(cacheName, cache);
			} catch (javax.cache.CacheException e) {
				throw new CacheException(e);
			}
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

	/**
	 * TODO: this could use some conversion framework love...
	 * Right now, only convert "true" and "false" to Boolean, and Integer keys
	 * @param props
	 */
	private static void fixProps(Map props) {
		Map override = new HashMap(props);
		Iterator<Map.Entry> iter = props.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			Object key = entry.getKey();
			Object value = entry.getValue();
			try {
				value = Integer.valueOf((String)value);
				override.put(key, value);
			} catch (NumberFormatException e) {
			} catch (ClassCastException e) {
			}
			if ("true".equals(value) || "false".equals(value)) {
				value = Boolean.valueOf((String)value);
				override.put(key, value);
			}
			if ("true".equals(key) || "false".equals(key)) {
				key = Boolean.valueOf((String)key);
				override.put(key, value);
			}
			try {
				key = Integer.valueOf((String)key);
				override.put(key, value);
			} catch (NumberFormatException e) {
			} catch (ClassCastException e) {
			}
		}
		props.putAll(override);
	}
}
