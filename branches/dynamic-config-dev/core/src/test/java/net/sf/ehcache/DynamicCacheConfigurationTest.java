/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 * @author cdennis
 */
public class DynamicCacheConfigurationTest extends AbstractCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicCacheConfigurationTest.class.getName());
    
    @Test
    public void testTTIChange() throws InterruptedException {
        Cache cache = new Cache("testTTIChange", 10, false, false, 0, 5);

        manager.addCache(cache);

        cache.put(new Element("key1", new Object()));
        cache.put(new Element("key2", new Object()));

        SECONDS.sleep(3);

        cache.get("key2");

        SECONDS.sleep(3);

        Assert.assertNull(cache.get("key1"));
        Assert.assertNotNull(cache.get("key2"));

        cache.getCacheConfiguration().setTimeToIdleSeconds(10);

        cache.put(new Element("key1", new Object()));
        
        SECONDS.sleep(7);

        Assert.assertNotNull(cache.get("key1"));
        Assert.assertNotNull(cache.get("key2"));

        SECONDS.sleep(12);

        Assert.assertNull(cache.get("key1"));
        Assert.assertNull(cache.get("key2"));

        cache.getCacheConfiguration().setTimeToIdleSeconds(2);

        cache.put(new Element("key1", new Object()));
        cache.put(new Element("key2", new Object()));

        SECONDS.sleep(4);

        Assert.assertNull(cache.get("key1"));
        Assert.assertNull(cache.get("key2"));
    }

    @Test
    public void testTTLChange() throws InterruptedException {
        Cache cache = new Cache("testTTLChange", 10, false, false, 5, 0);

        manager.addCache(cache);

        cache.put(new Element("key1", new Object()));

        SECONDS.sleep(3);

        Assert.assertNotNull(cache.get("key1"));
        cache.put(new Element("key2", new Object()));

        SECONDS.sleep(3);

        Assert.assertNull(cache.get("key1"));
        Assert.assertNotNull(cache.get("key2"));

        cache.getCacheConfiguration().setTimeToLiveSeconds(10);

        cache.put(new Element("key1", new Object()));

        SECONDS.sleep(4);

        Assert.assertNotNull(cache.get("key1"));
        Assert.assertNotNull(cache.get("key2"));

        SECONDS.sleep(4);

        Assert.assertNotNull(cache.get("key1"));
        Assert.assertNull(cache.get("key2"));

        SECONDS.sleep(5);

        Assert.assertNull(cache.get("key1"));

        cache.getCacheConfiguration().setTimeToLiveSeconds(2);

        cache.put(new Element("key1", new Object()));
        cache.put(new Element("key2", new Object()));

        SECONDS.sleep(4);

        Assert.assertNull(cache.get("key1"));
        Assert.assertNull(cache.get("key2"));
    }

    @Test
    public void testTTIChangeWithCustomElements() throws InterruptedException {
        Cache cache = new Cache("testTTIChangeWithCustomElements", 10, false, false, 0, 5);

        manager.addCache(cache);

        cache.put(new Element("default", new Object()));
        cache.put(new Element("eternal", new Object(), true, 0, 0));
        cache.put(new Element("short", new Object(), false, 1, 1));
        cache.put(new Element("long", new Object(), true, 100, 100));

        SECONDS.sleep(3);

        Assert.assertNull(cache.get("short"));

        SECONDS.sleep(3);

        Assert.assertNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        cache.getCacheConfiguration().setTimeToIdleSeconds(10);

        cache.put(new Element("default", new Object()));
        cache.put(new Element("short", new Object(), false, 1, 1));

        SECONDS.sleep(7);

        Assert.assertNotNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        SECONDS.sleep(12);

        Assert.assertNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        cache.getCacheConfiguration().setTimeToIdleSeconds(2);

        cache.put(new Element("default", new Object()));
        cache.put(new Element("short", new Object(), false, 1, 1));

        SECONDS.sleep(4);

        Assert.assertNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));
    }

    @Test
    public void testTTLChangeWithCustomElement() throws InterruptedException {
        Cache cache = new Cache("testTTLChangeWithCustomElements", 10, false, false, 5, 0);

        manager.addCache(cache);

        cache.put(new Element("default", new Object()));
        cache.put(new Element("eternal", new Object(), true, 0, 0));
        cache.put(new Element("short", new Object(), false, 1, 1));
        cache.put(new Element("long", new Object(), true, 100, 100));

        SECONDS.sleep(3);

        Assert.assertNotNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        SECONDS.sleep(3);

        Assert.assertNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        cache.getCacheConfiguration().setTimeToLiveSeconds(10);

        cache.put(new Element("default", new Object()));
        cache.put(new Element("short", new Object(), false, 1, 1));

        SECONDS.sleep(3);

        Assert.assertNotNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        SECONDS.sleep(3);

        Assert.assertNotNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        SECONDS.sleep(5);

        Assert.assertNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));

        cache.getCacheConfiguration().setTimeToLiveSeconds(2);

        cache.put(new Element("default", new Object()));
        cache.put(new Element("short", new Object(), false, 1, 1));

        SECONDS.sleep(4);

        Assert.assertNull(cache.get("default"));
        Assert.assertNotNull(cache.get("eternal"));
        Assert.assertNull(cache.get("short"));
        Assert.assertNotNull(cache.get("long"));
    }

    @Test
    @Ignore
    public void testMemoryCapacityChange() {
        Cache cache = new Cache("testMemoryCapacityChange", 10, false, true, 0, 0);
        manager.addCache(cache);

        for (int i = 0; i < 20; i++) {
            cache.put(new Element("key" + i, new Object()));
            Assert.assertTrue(cache.getSize() <= 10);
            Assert.assertTrue(cache.getMemoryStore().getSize() <= 10);
        }

        cache.getCacheConfiguration().setMaxElementsInMemory(20);

        for (int i = 20; i < 40; i++) {
            cache.put(new Element("key" + i, new Object()));
            Assert.assertTrue(cache.getSize() <= 20);
            Assert.assertTrue(cache.getSize() > 10);
            Assert.assertTrue(cache.getMemoryStore().getSize() <= 20);
            Assert.assertTrue(cache.getMemoryStore().getSize() > 10);
        }

        cache.getCacheConfiguration().setMaxElementsInMemory(5);
        
        for (int i = 40; i < 60; i++) {
            cache.put(new Element("key" + i, new Object()));
        }

        Assert.assertEquals(5, cache.getSize());
        Assert.assertEquals(5, cache.getMemoryStore().getSize());
    }

    @Test
    public void testDiskCapacityChange() throws InterruptedException {
        Cache cache = new Cache("testDiskCapacityChange", 10, true, true, 0, 0);
        cache.getCacheConfiguration().setMaxElementsOnDisk(10);
        manager.addCache(cache);

        for (int i = 0; i < 40; i++) {
            cache.put(new Element("key" + i, new byte[0]));
            MILLISECONDS.sleep(400);
            Assert.assertTrue(cache.getSize() <= 20);
            Assert.assertTrue(cache.getMemoryStore().getSize() <= 10);
            Assert.assertTrue(cache.getDiskStore().getSize() <= 10);
        }

        cache.getCacheConfiguration().setMaxElementsOnDisk(20);

        for (int i = 40; i < 80; i++) {
            cache.put(new Element("key" + i, new byte[0]));
            MILLISECONDS.sleep(400);
            Assert.assertTrue(cache.getSize() <= 30);
            Assert.assertTrue(cache.getSize() > 20);
            Assert.assertTrue(cache.getMemoryStore().getSize() <= 10);
            Assert.assertTrue(cache.getDiskStore().getSize() <= 20);
            Assert.assertTrue(cache.getDiskStore().getSize() > 10);
        }

        cache.getCacheConfiguration().setMaxElementsOnDisk(5);

        for (int i = 80; i < 120; i++) {
            cache.put(new Element("key" + i, new byte[0]));
            MILLISECONDS.sleep(400);
        }

        Assert.assertEquals(15, cache.getSize());
        Assert.assertEquals(10, cache.getMemoryStore().getSize());
        Assert.assertEquals(5, cache.getDiskStore().getSize());
    }
}
