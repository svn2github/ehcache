package net.dahanne.osgi.ehcache.consumer;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import javax.transaction.TransactionManager;

public class Demo {
	private TransactionManager tm;
	private CacheManager       cacheManager;
	private Ehcache            cache1;
	private Ehcache            cache2;

	public void go() {
		System.out.println("Starting demo....");
		CacheManager manager = CacheManager.newInstance(Demo.class.getResourceAsStream("/ehcache.xml"));
		Cache cache = manager.getCache("sampleCache1");
		Element element = new Element("key1", "value1");
		cache.put(element);
		System.out.println("Successfully added element " + element.toString() + " into cache "
		    + cache.toString());
		Element element1 = cache.get("key1");
		System.out.println("Successfully retrieved serialized element " + element1.toString()
		    + " from cache " + cache.toString());
		int elementsInMemory = cache.getSize();
		System.out.println("The cache size is " + elementsInMemory);

		Cache clusteredCache = manager.getCache("sampleCache2");
		clusteredCache.put(new Element("key2", "value2"));

		System.out.println("sampleCache2: isClustered? " + clusteredCache.isTerracottaClustered());

		System.out.println("sampleCache2: " + clusteredCache);

		clusteredCache = manager.getCache("sampleCache3");
		System.out.println("Expected to be null: " + clusteredCache);
	}

	public void txEhcache() throws Exception {
		// set up
		final TransactionManagerLookup lookup = new DefaultTransactionManagerLookup();
		tm = lookup.getTransactionManager();
		cacheManager = new CacheManager(Demo.class.getResourceAsStream("/ehcache.xml"));

		cache1 = cacheManager.getEhcache("txCache1");
		cache2 = cacheManager.getEhcache("txCache2");
		tm.begin();

		cache1.removeAll();
		cache2.removeAll();

		tm.commit();

		tm.begin();
		cache1.get(1);
		cache1.put(new Element(1, "one"));
		tm.commit();

		tm.begin();
		Element e = cache1.get(1);
		System.out.println("should equal to 'one' = " + e.getObjectValue());
		cache1.remove(1);
		e = cache1.get(1);
		System.out.println("should be null: " + e);
		int size = cache1.getSize();
		System.out.println("should equal to '0' = " + size);
		tm.rollback();

		tm.begin();
		e = cache1.get(1);
		System.out.println("should equal to 'one' = " + e.getObjectValue());

		// tear down
		tm.rollback();

		cacheManager.shutdown();
	}

}
