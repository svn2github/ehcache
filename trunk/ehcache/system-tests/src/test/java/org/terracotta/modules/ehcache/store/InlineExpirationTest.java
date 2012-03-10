/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import static java.util.concurrent.TimeUnit.SECONDS;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class InlineExpirationTest extends AbstractCacheTestBase {

  public InlineExpirationTest(TestConfig testConfig) {
    super("inline-expiration-test.xml", testConfig, InlineExpirationTestApp.class);
    testConfig.getClientConfig().setMaxHeap(256);
    testConfig.getClientConfig().setMinHeap(256);
  }

  public static class InlineExpirationTestApp extends ClientBase {
    public InlineExpirationTestApp(String[] args) {
      super("tti", args);
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      Cache ttiCache = cacheManager.getCache("tti");
      testSimpleTTI(ttiCache);
      testTouchBeforeHalfTTI(ttiCache);
      testNoTouchTTI(ttiCache);
      testCustomTTI(ttiCache);
      testChangeTTI(ttiCache);

      Cache eternalCache = cacheManager.getCache("eternal");
      testEternalWithMortalElement(eternalCache);
      testEternalCacheTurnedMortal(eternalCache);

      Cache ttlCache = cacheManager.getCache("ttl");
      testSimpleTTL(ttlCache);
      testChangeTTL(ttlCache);
      testCustomTTL(ttlCache);

      Cache ttiTtlCache = cacheManager.getCache("ttittl");
      testSimpleTTITTL(ttiTtlCache);
      testCustomTTITTL(ttiTtlCache);
      testTTITTLChange(ttiTtlCache);
    }

    private void testTTITTLChange(Cache cache) throws Exception {
      final String key1 = "ttittlChangekey1";
      final String key2 = "ttittlChangekey2";
      final String key3 = "ttittlChangekey3";
      final String key4 = "ttittlChangekey4";
      final String value = "ttittlchangevalue";
      final int oldTTI = (int) cache.getCacheConfiguration().getTimeToIdleSeconds();
      final int oldTTL = (int) cache.getCacheConfiguration().getTimeToLiveSeconds();

      final int newTTI = oldTTI / 2;
      final int newTTL = oldTTL * 2;

      cache.put(new Element(key1, value));
      assertElement(cache.get(key1), key1, value);

      cache.getCacheConfiguration().setTimeToIdleSeconds(newTTI);
      cache.getCacheConfiguration().setTimeToLiveSeconds(newTTL);

      SECONDS.sleep(newTTI + 1);
      Assert.assertNull(cache.get(key1));

      cache.put(new Element(key2, value));
      assertElement(cache.get(key2), key2, value);

      for (int slept = 0; slept < newTTL; slept += (newTTI / 2 + 1)) {
        assertElement(cache.get(key2), key2, value);
        SECONDS.sleep(newTTI / 2 + 1);
      }
      Assert.assertNull(cache.get(key2));

      cache.put(new Element(key3, value));
      assertElement(cache.get(key3), key3, value);
      cache.getCacheConfiguration().setTimeToIdleSeconds(oldTTI);

      SECONDS.sleep(oldTTI + 1);
      Assert.assertNull(cache.get(key3));

      cache.put(new Element(key4, value));
      assertElement(cache.get(key4), key4, value);
      for (int slept = 0; slept < newTTL; slept += (oldTTI / 2 + 1)) {
        assertElement(cache.get(key4), key4, value);
        SECONDS.sleep(oldTTI / 2 + 1);
      }
      Assert.assertNull(cache.get(key4));
    }

    private void testCustomTTITTL(Cache cache) throws Exception {
      final String key1 = "customTTITTLKey1";
      final String key2 = "customTTITTLKey2";
      final String value = "customTTITTLValue";
      final int tti = (int) cache.getCacheConfiguration().getTimeToIdleSeconds();
      final int ttl = (int) cache.getCacheConfiguration().getTimeToLiveSeconds();
      final int customTTI = tti / 2;
      final int customTTL = ttl * 2;

      Element e = new Element(key1, value);
      e.setTimeToIdle(customTTI);
      cache.put(e);
      assertElement(cache.get(key1), key1, value);

      SECONDS.sleep(tti / 2 + 1);
      Assert.assertNull(cache.get(key1));

      Element e2 = new Element(key2, value);
      e2.setTimeToLive(customTTL);
      cache.put(e2);
      for (int slept = 0; slept < customTTL; slept += (tti / 2 + 1)) {
        assertElement(cache.get(key2), key2, value);
        SECONDS.sleep(tti / 2 + 1);
      }
      Assert.assertNull(cache.get(key2));
    }

    private void testSimpleTTITTL(Cache cache) throws Exception {
      final String key = "ttiTTLKey";
      final String value = "ttiTTLValue";
      final int tti = (int) cache.getCacheConfiguration().getTimeToIdleSeconds();
      final int ttl = (int) cache.getCacheConfiguration().getTimeToLiveSeconds();

      cache.put(new Element(key, value));

      for (int slept = 0; slept < ttl; slept += (tti / 2 + 1)) {
        assertElement(cache.get(key), key, value);
        SECONDS.sleep(tti / 2 + 1);
      }

      Assert.assertNull(cache.get(key));
    }

    private void testCustomTTL(Cache cache) throws Exception {
      final String key = "customTTL";
      final String value = "customTTLValue";
      final int ttl = (int) cache.getCacheConfiguration().getTimeToLiveSeconds();
      final int customTTL = ttl * 2;

      Element e = new Element(key, value);
      e.setTimeToLive(customTTL);
      cache.put(e);
      assertElement(cache.get(key), key, value);

      SECONDS.sleep(ttl + 1);
      assertElement(cache.get(key), key, value);

      SECONDS.sleep(ttl + 1);
      Assert.assertNull(cache.get(key));
    }

    private void testChangeTTL(Cache cache) throws Exception {
      final String key1 = "changeTTLKey1";
      final String key2 = "changeTTLKey2";
      final String value = "changeTTLValue";
      final int oldTTL = (int) cache.getCacheConfiguration().getTimeToLiveSeconds();
      final int newTTL = oldTTL * 2;

      cache.put(new Element(key1, value));
      assertElement(cache.get(key1), key1, value);

      cache.getCacheConfiguration().setTimeToLiveSeconds(newTTL);

      SECONDS.sleep(oldTTL + 1);
      assertElement(cache.get(key1), key1, value);

      cache.put(new Element(key2, value));
      assertElement(cache.get(key2), key2, value);

      SECONDS.sleep(oldTTL + 1);
      Assert.assertNull(cache.get(key1));
      assertElement(cache.get(key2), key2, value);

      cache.getCacheConfiguration().setTimeToLiveSeconds(oldTTL);
      Assert.assertNull(cache.get(key2));
    }

    private void testSimpleTTL(Cache cache) throws Exception {
      final String key = "simpleTTLKey";
      final String value = "simpleTTLValue";
      final int ttl = (int) cache.getCacheConfiguration().getTimeToLiveSeconds();
      final int halfTTL = ttl / 2;

      cache.put(new Element(key, value));
      assertElement(cache.get(key), key, value);

      SECONDS.sleep(halfTTL);
      assertElement(cache.get(key), key, value);

      SECONDS.sleep(ttl - halfTTL + 1);
      Assert.assertNull(cache.get(key));
    }

    private void testEternalCacheTurnedMortal(Cache cache) throws Exception {
      final String key1 = "eternalTurnedMortal";
      final String key2 = "eternalTurnedMortal2";
      final String value = "eternalTurnedMortalValue";
      final int tti = 10;

      cache.put(new Element(key1, value));
      cache.put(new Element(key2, value));
      assertElement(cache.get(key1), key1, value);
      assertElement(cache.get(key2), key2, value);

      cache.getCacheConfiguration().setEternal(false);
      cache.getCacheConfiguration().setTimeToIdleSeconds(tti);

      SECONDS.sleep(tti + 1);
      Assert.assertNull(cache.get(key1));
      cache.getCacheConfiguration().setTimeToIdleSeconds(0);
      cache.getCacheConfiguration().setEternal(true);
      assertElement(cache.get(key2), key2, value);
    }

    private void testEternalWithMortalElement(Cache cache) throws Exception {
      final String key = "mortal";
      final String value = "mortalValue";
      final int tti = 10;

      Element e = new Element(key, value);
      e.setTimeToIdle(tti);
      cache.put(e);
      assertElement(cache.get(key), key, value);

      SECONDS.sleep(tti + 1);
      Assert.assertNull(cache.get(key));
    }

    private void testChangeTTI(Cache cache) throws Exception {
      final String key1 = "testChange";
      final String key2 = "testChange2";
      final String value = "testChangeValue";
      final int initialTTI = (int) cache.getCacheConfiguration().getTimeToIdleSeconds();

      cache.put(new Element(key1, value));
      cache.put(new Element(key2, value));
      assertElement(cache.get(key1), key1, value);
      assertElement(cache.get(key2), key2, value);

      SECONDS.sleep(initialTTI / 2 + 1);
      cache.getCacheConfiguration().setTimeToIdleSeconds(initialTTI / 2);
      Assert.assertNull(cache.get(key1)); // Should get inline expired right away now.

      cache.getCacheConfiguration().setTimeToIdleSeconds(initialTTI);
      assertElement(cache.get(key2), key2, value);

      SECONDS.sleep(initialTTI + 1);
      Assert.assertNull(cache.get(key2));
    }

    private void testNoTouchTTI(Cache cache) throws Exception {
      final String key = "noTouch";
      final String value = "noTouchValue";
      final int tti = (int) cache.getCacheConfiguration().getTimeToIdleSeconds();

      cache.put(new Element(key, value));
      assertElement(cache.get(key), key, value);

      SECONDS.sleep(tti + 1);
      Assert.assertNull(cache.get(key));
    }

    private void testCustomTTI(Cache cache) throws Exception {
      final String key = "customTTI";
      final String value = "customTTIVAlue";
      final int tti = (int) cache.getCacheConfiguration().getTimeToIdleSeconds();
      final int customTTI = tti * 2;

      Element e = new Element(key, value);
      e.setTimeToIdle(customTTI);
      cache.put(e);
      assertElement(cache.get(key), key, value);

      SECONDS.sleep(tti + 1);
      assertElement(cache.get(key), key, value);

      SECONDS.sleep(customTTI + 1);
      Assert.assertNull(cache.get(key));
    }

    private void testTouchBeforeHalfTTI(Cache cache) throws Exception {
      final String key = "touchBeforeHalfTTI";
      final String value = "touchBeforeHalfTTI value";
      final int tti = (int) cache.getCacheConfiguration().getTimeToIdleSeconds();

      cache.put(new Element(key, value));

      SECONDS.sleep(tti / 3); // sleep for the first third of TTI
      assertElement(cache.get(key), key, value);

      SECONDS.sleep(tti - (tti / 3) + 1); // sleep the remaining 2/3's of TTI
      Assert.assertNull(cache.get(key));
    }

    private void testSimpleTTI(Cache cache) throws Exception {
      final String key = "simpleKey";
      final String value = "simpleValue";
      final int tti = (int) cache.getCacheConfiguration().getTimeToIdleSeconds();

      cache.put(new Element(key, value));
      assertElement(cache.get(key), key, value);

      int halfTTI = tti / 2;
      SECONDS.sleep(halfTTI);
      assertElement(cache.get(key), key, value);

      SECONDS.sleep(tti + 1);
      Assert.assertNull(cache.get(key));
    }

    private void assertElement(Element e, Object key, Object value) {
      Assert.assertNotNull(e);
      Assert.assertEquals(key, e.getObjectKey());
      Assert.assertEquals(value, e.getObjectValue());
    }
  }
}
