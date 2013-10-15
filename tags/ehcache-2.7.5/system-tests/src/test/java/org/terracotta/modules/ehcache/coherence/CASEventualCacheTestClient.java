package org.terracotta.modules.ehcache.coherence;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;

/**
 * CASEventualCacheTestClient
 */
public class CASEventualCacheTestClient extends ClientBase {

    public static void main(String[] args) {
      try {
        new CacheCoherenceTestL1Client(args).run();
      } catch (Throwable e) {
        e.printStackTrace();
        System.err.println("Test FAILED");
        System.exit(1);
      }
    }

    public CASEventualCacheTestClient(String[] args) {
        super(args);
    }

    @Override
    protected void runTest(Cache cache, Toolkit myToolkit) throws Throwable {
        System.out.println("Running client");
        assertThat(cache.getCacheConfiguration().getTerracottaConfiguration().getConsistency(), is(TerracottaConfiguration.Consistency.EVENTUAL));

        try {
            cache.putIfAbsent(new Element("key", "value"));
            fail("Eventual consistent cache did not throw on CAS operation");
        } catch (CacheException e) {
            //No-op
        }
        try {
            cache.removeElement(new Element("key", "value"));
            fail("Eventual consistent cache did not throw on CAS operation");
        } catch (CacheException e) {
            //No-op
        }
        try {
            cache.replace(new Element("key", "value"), new Element("key", "otherValue"));
            fail("Eventual consistent cache did not throw on CAS operation");
        } catch (CacheException e) {
            //No-op
        }
        try {
            cache.replace(new Element("key", "value"));
            fail("Eventual consistent cache did not throw on CAS operation");
        } catch (CacheException e) {
            //No-op
        }
    }

    @Override
    protected Cache getCache() {
        return cacheManager.getCache("non-strict-Cache");
    }
}
