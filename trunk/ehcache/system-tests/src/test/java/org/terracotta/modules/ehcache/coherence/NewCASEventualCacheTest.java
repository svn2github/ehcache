package org.terracotta.modules.ehcache.coherence;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

/**
 * NewCASEventualCacheTest
 */
public class NewCASEventualCacheTest extends AbstractCacheTestBase {

    public NewCASEventualCacheTest(TestConfig testConfig) {
        super("cache-coherence-test.xml", testConfig, NewCASEventualCacheTestClient.class, NewCASEventualCacheTestClient.class);
    }


    public static class NewCASEventualCacheTestClient extends ClientBase {

        public NewCASEventualCacheTestClient(String[] args) {
            super(args);
        }

        @Override
        protected void runTest(Cache cache, Toolkit myToolkit) throws Throwable {
            int i = waitForAllClients();

            Element elem = new Element("key", 0);
            boolean wonPutIfAbsent = cache.putIfAbsent(elem) == null;
            if (wonPutIfAbsent) {
                System.out.println("Won putIfAbsent");
            } else {
                System.out.println("Lost putIfAbsent");
            }


            int count = 0;
            while (count < 50) {
                Element currentElem = cache.get("key");
                Element newElem = new Element("key", ((Integer)currentElem.getObjectValue() + 1));
                while (!cache.replace(currentElem, newElem)) {
                    System.out.println("Lost replace race - getting value");
                    currentElem = cache.get("key");
                    newElem = new Element("key", ((Integer)currentElem.getObjectValue() + 1));
                }
                count++;
            }

            waitForAllClients();

            Element endElem;
            cache.acquireReadLockOnKey("key");
            try {
                endElem = cache.get("key");
                assertEquals(endElem.getObjectValue(), 100);
            } finally {
                cache.releaseReadLockOnKey("key");
            }

            waitForAllClients();

            boolean wonRemove = cache.removeElement(endElem);
            if (wonRemove) {
                System.out.println("Won removeElement");
            } else {
                System.out.println("Lost removeElement");
            }

        }

        @Override
        protected Cache getCache() {
          return cacheManager.getCache("non-strict-Cache");
        }
    }
}
