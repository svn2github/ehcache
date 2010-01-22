/**
 *  Copyright 2003-2009 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.writer;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.event.CountingCacheEventListener;
import net.sf.ehcache.event.RegisteredEventListeners;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests for a CacheWriters
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class CacheWriterTest extends AbstractCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(CacheWriterTest.class.getName());

    protected CacheManager manager;

    @Before
    public void setUp() throws Exception {
        manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-writer.xml");
    }

    @After
    public void tearDown() throws Exception {
        if (!manager.getStatus().equals(Status.STATUS_SHUTDOWN)) {
            manager.shutdown();
        }
    }

    @Test
    public void testWriteThroughXml() {
        Cache cache = manager.getCache("writeThroughCacheXml");
        assertNotNull(cache.getRegisteredCacheWriter());

        WriteThroughTestCacheWriter writer = (WriteThroughTestCacheWriter) cache.getRegisteredCacheWriter();

        Element el1 = new Element("key1", "value1");
        Element el2 = new Element("key2", "value2");
        Element el3 = new Element("key3", "value3");
        assertEquals(0, writer.getWrittenElements().size());
        cache.putWithWriter(el1);
        assertEquals(1, writer.getWrittenElements().size());
        cache.putWithWriter(el2);
        assertEquals(2, writer.getWrittenElements().size());
        cache.putWithWriter(el3);
        assertEquals(3, writer.getWrittenElements().size());
        assertEquals("value1", writer.getWrittenElements().get("key1").getValue());
        assertEquals("value2", writer.getWrittenElements().get("key2").getValue());
        assertEquals("value3", writer.getWrittenElements().get("key3").getValue());
        cache.removeWithWriter(el2.getKey());
        assertEquals(2, writer.getWrittenElements().size());
        assertNotNull(writer.getWrittenElements().get("key1"));
        assertNull(writer.getWrittenElements().get("key2"));
        assertNotNull(writer.getWrittenElements().get("key3"));
    }

    @Test
    public void testWriteThroughXmlProperties() {
        Cache cache = manager.getCache("writeThroughCacheXmlProperties");
        assertNotNull(cache.getRegisteredCacheWriter());

        WriteThroughTestCacheWriter writer = (WriteThroughTestCacheWriter) cache.getRegisteredCacheWriter();

        Element el1 = new Element("key1", "value1");
        Element el2 = new Element("key2", "value2");
        Element el3 = new Element("key3", "value3");
        assertEquals(0, writer.getWrittenElements().size());
        cache.putWithWriter(el1);
        assertEquals(1, writer.getWrittenElements().size());
        cache.putWithWriter(el2);
        assertEquals(2, writer.getWrittenElements().size());
        cache.putWithWriter(el3);
        assertEquals(3, writer.getWrittenElements().size());
        assertNull(writer.getWrittenElements().get("key1"));
        assertNull(writer.getWrittenElements().get("key2"));
        assertNull(writer.getWrittenElements().get("key3"));
        assertEquals("value1", writer.getWrittenElements().get("prekey1suff").getValue());
        assertEquals("value2", writer.getWrittenElements().get("prekey2suff").getValue());
        assertEquals("value3", writer.getWrittenElements().get("prekey3suff").getValue());
        cache.removeWithWriter(el1.getKey());
        assertEquals(2, writer.getWrittenElements().size());
        assertNull(writer.getWrittenElements().get("prekey1suff"));
        assertNotNull(writer.getWrittenElements().get("prekey2suff"));
        assertNotNull(writer.getWrittenElements().get("prekey3suff"));
    }

    @Test
    public void testWriteThroughJavaRegistration() {
        Cache cache = manager.getCache("writeThroughCacheJavaRegistration");
        assertNull(cache.getRegisteredCacheWriter());

        WriteThroughTestCacheWriter writer = new WriteThroughTestCacheWriter(new Properties());
        cache.registerCacheWriter(writer);

        Element el1 = new Element("key1", "value1");
        Element el2 = new Element("key2", "value2");
        Element el3 = new Element("key3", "value3");
        Element el4 = new Element("key4", "value4");
        assertEquals(0, writer.getWrittenElements().size());
        cache.putWithWriter(el1);
        assertEquals(1, writer.getWrittenElements().size());
        cache.putWithWriter(el2);
        assertEquals(2, writer.getWrittenElements().size());
        cache.putWithWriter(el3);
        assertEquals(3, writer.getWrittenElements().size());
        cache.putWithWriter(el4);
        assertEquals(4, writer.getWrittenElements().size());
        assertEquals("value1", writer.getWrittenElements().get("key1").getValue());
        assertEquals("value2", writer.getWrittenElements().get("key2").getValue());
        assertEquals("value3", writer.getWrittenElements().get("key3").getValue());
        assertEquals("value4", writer.getWrittenElements().get("key4").getValue());
        cache.removeWithWriter(el1.getKey());
        cache.removeWithWriter(el2.getKey());
        cache.removeWithWriter(el3.getKey());
        assertEquals(1, writer.getWrittenElements().size());
        assertNull(writer.getWrittenElements().get("key1"));
        assertNull(writer.getWrittenElements().get("key2"));
        assertNull(writer.getWrittenElements().get("key3"));
        assertNotNull(writer.getWrittenElements().get("key4"));
    }

    @Test
    public void testWriteThroughNotifyListeners() {
        Cache cache = new Cache(new CacheConfiguration("writeThroughCacheOnly", 10)
                .cacheEventListenerFactory(new CacheConfiguration.CacheEventListenerFactoryConfiguration().className("net.sf.ehcache.event.CountingCacheEventListenerFactory")));
        assertNull(cache.getRegisteredCacheWriter());

        CacheManager.getInstance().addCache(cache);

        WriteThroughTestCacheWriterException writer = new WriteThroughTestCacheWriterException();
        cache.registerCacheWriter(writer);

        // without listeners notification
        cache.getCacheConfiguration().getCacheWriterConfiguration().setNotifyListenersOnException(false);

        CountingCacheEventListener.resetCounters();

        try {
            cache.putWithWriter(new Element("key1", "value1"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
            assertEquals(0, CountingCacheEventListener.getCacheElementsPut(cache).size());
        }

        CountingCacheEventListener.resetCounters();

        try {
            cache.putWithWriter(new Element("key1", "value1"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
            assertEquals(0, CountingCacheEventListener.getCacheElementsUpdated(cache).size());
        }

        CountingCacheEventListener.resetCounters();

        try {
            cache.removeWithWriter("key1");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
            assertEquals(0, CountingCacheEventListener.getCacheElementsRemoved(cache).size());
        }

        // with listeners notification
        cache.getCacheConfiguration().getCacheWriterConfiguration().setNotifyListenersOnException(true);

        CountingCacheEventListener.resetCounters();

        try {
            cache.putWithWriter(new Element("key1", "value1"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
            assertEquals(1, CountingCacheEventListener.getCacheElementsPut(cache).size());
        }

        CountingCacheEventListener.resetCounters();

        try {
            cache.putWithWriter(new Element("key1", "value1"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
            assertEquals(1, CountingCacheEventListener.getCacheElementsUpdated(cache).size());
        }

        CountingCacheEventListener.resetCounters();

        try {
            cache.removeWithWriter("key1");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
            assertEquals(1, CountingCacheEventListener.getCacheElementsRemoved(cache).size());
        }
    }
/*
    @Test
    public void testWriteBehindSolelyJava() {
    Cache cache = new Cache(
        new CacheConfiguration("writeBehindSolelyJava", 10)
            .cacheWriter(new CacheWriterConfiguration()
                .writeMode(CacheWriterConfiguration.WriteMode.WRITE_BEHIND)
                .maxWriteDelay(8)
                .rateLimitPerSecond(5)
                .writeCoalescing(true)
                .writeBatching(true)
                .writeBatchSize(20)
                .retryAttempts(2)
                .retryAttemptDelaySeconds(2)
                .cacheWriterFactory(new CacheWriterConfiguration.CacheWriterFactoryConfiguration()
                    .className("com.company.MyCacheWriterFactory")
                    .properties("just.some.property=test; another.property=test2")
                    .propertySeparator(";"))));
        assertNull(cache.getRegisteredCacheWriter());

        CacheManager.getInstance().addCache(cache);
    }
*/
}