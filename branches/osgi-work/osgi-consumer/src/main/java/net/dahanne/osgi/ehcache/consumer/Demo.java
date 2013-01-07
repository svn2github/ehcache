package net.dahanne.osgi.ehcache.consumer;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
public class Demo {

	public void go() {
      System.out.println("Starting demo....");
//      System.out.println("URL: " + Demo.class.getResource("ehcache.xml"));
			CacheManager manager = CacheManager.newInstance(Demo.class.getResourceAsStream("/ehcache.xml"));
//			manager.addCache("sampleCache1");
			Cache cache = manager.getCache("sampleCache1");
			Element element = new Element("key1", "value1");
			cache.put(element);
			System.out.println("Successfully added element "+element.toString()+" into cache "+cache.toString());
			Element element1 = cache.get("key1");
			System.out.println("Successfully retrieved serialized element "+element1.toString()+" from cache "+cache.toString());
			int elementsInMemory = cache.getSize();
			System.out.println("The cache size is "+elementsInMemory);
	}

}
