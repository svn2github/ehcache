/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertThat;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.store.disk.DiskStoreHelper;

import org.hamcrest.core.CombinableMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cdennis
 */
public class DynamicCacheConfigurationTest extends AbstractCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicCacheConfigurationTest.class.getName());

    @Test
    public void testTimeToIdleChange() throws InterruptedException {
        Cache cache = new Cache("testTimeToIdleChange", 10, false, false, 0, 10);

        manager.addCache(cache);

        cache.put(new Element("key1", new Object()));
        cache.put(new Element("key2", new Object()));

        SECONDS.sleep(6);

        cache.get("key2");

        SECONDS.sleep(6);

        Assert.assertNull(cache.get("key1"));
        Assert.assertNotNull(cache.get("key2"));

        cache.getCacheConfiguration().setTimeToIdleSeconds(20);

        cache.put(new Element("key1", new Object()));

        SECONDS.sleep(15);

        Assert.assertNotNull(cache.get("key1"));
        Assert.assertNotNull(cache.get("key2"));

        SECONDS.sleep(25);

        Assert.assertNull(cache.get("key1"));
        Assert.assertNull(cache.get("key2"));

        cache.getCacheConfiguration().setTimeToIdleSeconds(4);

        cache.put(new Element("key1", new Object()));
        cache.put(new Element("key2", new Object()));

        SECONDS.sleep(8);

        Assert.assertNull(cache.get("key1"));
        Assert.assertNull(cache.get("key2"));
    }

    @Test
    public void testTTLChange() throws InterruptedException {
        Cache cache = new Cache("testTTLChange", 10, false, false, 10, 0);

        manager.addCache(cache);

        cache.put(new Element("key1", new Object()));

        SECONDS.sleep(6);

        Assert.assertNotNull(cache.get("key1"));
        cache.put(new Element("key2", new Object()));

        SECONDS.sleep(6);

        Assert.assertNull(cache.get("key1"));
        Assert.assertNotNull(cache.get("key2"));

        cache.getCacheConfiguration().setTimeToLiveSeconds(20);

        cache.put(new Element("key1", new Object()));

        SECONDS.sleep(8);

        Assert.assertNotNull(cache.get("key1"));
        Assert.assertNotNull(cache.get("key2"));

        SECONDS.sleep(8);

        Assert.assertNotNull(cache.get("key1"));
        Assert.assertNull(cache.get("key2"));

        SECONDS.sleep(10);

        Assert.assertNull(cache.get("key1"));

        cache.getCacheConfiguration().setTimeToLiveSeconds(4);

        cache.put(new Element("key1", new Object()));
        cache.put(new Element("key2", new Object()));

        SECONDS.sleep(8);

        Assert.assertNull(cache.get("key1"));
        Assert.assertNull(cache.get("key2"));
    }

    @Test
    public void testTimeToIdleChangeWithCustomElements() throws InterruptedException {
        Cache cache = new Cache("testTimeToIdleChangeWithCustomElements", 10, false, false, 0, 10);

        manager.addCache(cache);

        cache.put(new Element("default", new Object()));
        cache.put(new Element("eternal", new Object(), true, 0, 0));
        cache.put(new Element("short", new Object(), false, 1, 1));
        cache.put(new Element("long", new Object(), false, 100, 100));

        SECONDS.sleep(6);

        Assert.assertNull(cache.get("short"));

        SECONDS.sleep(6);

        Assert.assertNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        cache.getCacheConfiguration().setTimeToIdleSeconds(20);

        cache.put(new Element("default", new Object()));
        cache.put(new Element("short", new Object(), false, 1, 1));

        SECONDS.sleep(15);

        Assert.assertNotNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        SECONDS.sleep(25);

        Assert.assertNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        cache.getCacheConfiguration().setTimeToIdleSeconds(4);

        cache.put(new Element("default", new Object()));
        cache.put(new Element("short", new Object(), false, 1, 1));

        SECONDS.sleep(8);

        Assert.assertNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));
    }

    @Test
    public void testTTLChangeWithCustomElement() throws InterruptedException {
        Cache cache = new Cache("testTTLChangeWithCustomElements", 10, false, false, 10, 0);

        manager.addCache(cache);

        cache.put(new Element("default", new Object()));
        cache.put(new Element("eternal", new Object(), true, 0, 0));
        cache.put(new Element("short", new Object(), false, 1, 1));
        cache.put(new Element("long", new Object(), false, 100, 100));

        SECONDS.sleep(6);

        Assert.assertNotNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        SECONDS.sleep(6);

        Assert.assertNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        cache.getCacheConfiguration().setTimeToLiveSeconds(20);

        cache.put(new Element("default", new Object()));
        cache.put(new Element("short", new Object(), false, 1, 1));

        SECONDS.sleep(6);

        Assert.assertNotNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        SECONDS.sleep(6);

        Assert.assertNotNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        SECONDS.sleep(10);

        Assert.assertNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        cache.getCacheConfiguration().setTimeToLiveSeconds(4);

        cache.put(new Element("default", new Object()));
        cache.put(new Element("short", new Object(), false, 1, 1));

        SECONDS.sleep(8);

        Assert.assertNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));
    }

    @Test
    public void testMemoryCapacityChange() {
        Cache cache = new Cache("testMemoryCapacityChange", 10, false, true, 0, 0);
        manager.addCache(cache);

        for (int i = 0; i < 20; i++) {
            cache.put(new Element("key" + i, new Object()));
            Assert.assertTrue(cache.getSize() <= 10);
            Assert.assertTrue(cache.getStatistics().getLocalHeapSize() <= 10);
        }

        cache.getCacheConfiguration().setMaxElementsInMemory(20);

        for (int i = 20; i < 40; i++) {
            cache.put(new Element("key" + i, new Object()));
            Assert.assertTrue(cache.getSize() <= 20);
            Assert.assertTrue(cache.getSize() > 10);
            Assert.assertTrue(cache.getStatistics().getLocalHeapSize() <= 20);
            Assert.assertTrue(cache.getStatistics().getLocalHeapSize() > 10);
        }

        cache.getCacheConfiguration().setMaxElementsInMemory(5);

        for (int i = 40; i < 60; i++) {
            cache.put(new Element("key" + i, new Object()));
        }

        Assert.assertEquals(5, cache.getSize());
        Assert.assertEquals(5, cache.getStatistics().getLocalHeapSize());
    }

    @Test
    public void testDiskCapacityChange() throws Exception {
        final int DISK_WIGGLE = 2;

        Cache cache = new Cache("testDiskCapacityChange", 10, true, true, 0, 0);
        cache.getCacheConfiguration().setMaxElementsOnDisk(20);
        manager.addCache(cache);

        for (int i = 0; i < 40; i++) {
            cache.put(new Element("key" + i, new byte[0]));
            DiskStoreHelper.flushAllEntriesToDisk(cache).get();
            assertThat(cache.getSize(), lessThanOrEqualTo(20));
            assertThat(cache.getStatistics().getLocalHeapSize(), lessThanOrEqualTo(10L));
            assertThat(cache.getStatistics().getLocalDiskSize(), lessThanOrEqualTo(20L + DISK_WIGGLE));
        }

        cache.getCacheConfiguration().setMaxElementsOnDisk(20);

        for (int i = 40; i < 80; i++) {
            cache.put(new Element("key" + i, new byte[0]));
            DiskStoreHelper.flushAllEntriesToDisk(cache).get();
            assertThat(cache.getSize(), CombinableMatcher.<Integer>both(lessThanOrEqualTo(30)).and(greaterThan(10)));
            assertThat(cache.getStatistics().getLocalHeapSize(), lessThanOrEqualTo(10L));
            assertThat(cache.getStatistics().getLocalDiskSize(), CombinableMatcher.both(lessThanOrEqualTo(20L + DISK_WIGGLE)).and(greaterThan(10L)));
        }

        cache.getCacheConfiguration().setMaxElementsOnDisk(10);

        for (int i = 80; i < 120; i++) {
            cache.put(new Element("key" + i, new byte[0]));
            DiskStoreHelper.flushAllEntriesToDisk(cache).get();
        }

        assertThat(cache.getSize(), lessThanOrEqualTo(10));
        assertThat(cache.getStatistics().getLocalHeapSize(), lessThanOrEqualTo(10L));
        Assert.assertEquals(10, cache.getStatistics().getLocalDiskSize());
    }

    @Test
    public void testCacheWithFrozenConfig() {
        Configuration managerConfig = new Configuration()
                .dynamicConfig(false)
                .defaultCache(new CacheConfiguration("definedCache1", 20))
                .cache(new CacheConfiguration("definedCache", 10).eternal(true)).name("new-cm");

        CacheManager manager = new CacheManager(managerConfig);

        Cache defined = manager.getCache("definedCache");
        try {
            defined.getCacheConfiguration().setTimeToIdleSeconds(99);
            Assert.fail();
        } catch (CacheException e) {
            // expected
        }

        try {
            defined.setDisabled(true);
            Assert.fail();
        } catch (CacheException e) {
            // expected
        }

        defined.put(new Element("key", "value"));
        Assert.assertNotNull(defined.get("key"));

        Cache programmatic = new Cache("programmatic", 10, false, true, 0, 0);
        manager.addCache(programmatic);
        try {
            programmatic.getCacheConfiguration().setTimeToIdleSeconds(99);
            Assert.fail();
        } catch (CacheException e) {
            // expected
        }

        try {
            programmatic.setDisabled(true);
            Assert.fail();
        } catch (CacheException e) {
            // expected
        }

        programmatic.put(new Element("key", "value"));
        Assert.assertNotNull(programmatic.get("key"));
        manager.shutdown();
    }

    @Test
    public void testConfiguringClonedCache() throws CloneNotSupportedException {
        Cache cache = new Cache("testConfiguringClonedCache", 10, false, true, 0, 0);
        Cache clone = cache.clone();
        clone.setName("testConfiguringClonedCacheCloned");

        manager.addCache(cache);
        manager.addCache(clone);

        Assert.assertEquals(10, cache.getCacheConfiguration().getMaxElementsInMemory());
        Assert.assertEquals(10, clone.getCacheConfiguration().getMaxElementsInMemory());

        for (int i = 0; i < 20; i++) {
            cache.put(new Element("key" + i, new Object()));
            Assert.assertTrue(cache.getSize() <= 10);
            Assert.assertTrue(cache.getStatistics().getLocalHeapSize() <= 10);
        }

        for (int i = 0; i < 20; i++) {
            clone.put(new Element("key" + i, new Object()));
            Assert.assertTrue(clone.getSize() <= 10);
            Assert.assertTrue(clone.getStatistics().getLocalHeapSize() <= 10);
        }

        cache.getCacheConfiguration().setMaxElementsInMemory(20);
        clone.getCacheConfiguration().setMaxElementsInMemory(5);

        for (int i = 20; i < 40; i++) {
            cache.put(new Element("key" + i, new Object()));
            Assert.assertTrue(cache.getSize() <= 20);
            Assert.assertTrue(cache.getSize() > 10);
            Assert.assertTrue(cache.getStatistics().getLocalHeapSize() <= 20);
            Assert.assertTrue(cache.getStatistics().getLocalHeapSize() > 10);
        }

        for (int i = 20; i < 40; i++) {
            clone.put(new Element("key" + i, new Object()));
        }

        Assert.assertEquals(5, clone.getSize());
        Assert.assertEquals(5, clone.getStatistics().getLocalHeapSize());

        cache.getCacheConfiguration().setMaxElementsInMemory(5);
        clone.getCacheConfiguration().setMaxElementsInMemory(20);

        for (int i = 40; i < 60; i++) {
            cache.put(new Element("key" + i, new Object()));
        }

        Assert.assertEquals(5, cache.getSize());
        Assert.assertEquals(5, cache.getStatistics().getLocalHeapSize());

        for (int i = 40; i < 60; i++) {
            clone.put(new Element("key" + i, new Object()));
            Assert.assertTrue(clone.getSize() <= 20);
            Assert.assertTrue(clone.getSize() > 5);
            Assert.assertTrue(clone.getStatistics().getLocalHeapSize() <= 20);
            Assert.assertTrue(clone.getStatistics().getLocalHeapSize() > 5);
        }
    }
}
