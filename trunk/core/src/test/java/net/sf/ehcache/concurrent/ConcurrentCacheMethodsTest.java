/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.concurrent;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConcurrentCacheMethodsTest {

    private volatile CacheManager manager;
    private volatile Ehcache cache;
    
    @Before
    public void setup() {
        manager = CacheManager.create();
        cache = new Cache(new CacheConfiguration("testCache", 0));
        manager.addCache(cache);
    }
    
    @After
    public void clearup() {
        manager.removalAll();
        manager.shutdown();
    }
    
    @Test
    public void testPutIfAbsent() {
        Element e = new Element("key", "value");
        Assert.assertNull(cache.putIfAbsent(e));
        Assert.assertEquals(e, cache.putIfAbsent(new Element("key", "value2")));
        
        try {
            cache.putIfAbsent(null);
            Assert.fail("putIfAbsent with null Element should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
        
        try {
            cache.putIfAbsent(new Element(null, "value"));
            Assert.fail("putIfAbsent with null key should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
    }
    
    @Test
    public void testRemoveElement() {
        Element e = new Element("key", "value");
        cache.put(e);
        
        Assert.assertFalse(cache.removeElement(new Element("key", "value2")));
        Assert.assertFalse(cache.removeElement(new Element("key2", "value")));
        Assert.assertTrue(cache.removeElement(new Element("key", "value")));
        
        try {
            cache.removeElement(null);
            Assert.fail("removeElement with null Element should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }
        
        try {
            cache.removeElement(new Element(null, "value"));
            Assert.fail("removeElement with null key should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }
    }
    
    @Test
    public void testTwoArgReplace() {
        Assert.assertFalse(cache.replace(new Element("key", "value1"), new Element("key", "value2")));
        cache.put(new Element("key", "value1"));
        Assert.assertTrue(cache.replace(new Element("key", "value1"), new Element("key", "value2")));
        Assert.assertFalse(cache.replace(new Element("key", "value1"), new Element("key", "value2")));

        try {
            cache.replace(null, new Element("key", "value2"));
            Assert.fail("replace with null key should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }

        try {
            cache.replace(new Element("key", "value1"), null);
            Assert.fail("replace with null key should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }
        
        try {
            cache.replace(null, null);
            Assert.fail("replace with null key should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }

        try {
            cache.replace(new Element(null, "value1"), new Element("key", "value2"));
            Assert.fail("replace with null key should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }

        try {
            cache.replace(new Element("key", "value1"), new Element(null, "value2"));
            Assert.fail("replace with null key should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }
        
        try {
            cache.replace(new Element(null, "value1"), new Element(null, "value2"));
            Assert.fail("replace with null keys should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }
        
        try {
            cache.replace(new Element("key", "value1"), new Element("different", "value2"));
            Assert.fail("replace with non-matching keys should throw IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            //expected
        }
    }

    @Test
    public void testOneArgReplace() {
        
        Assert.assertNull(cache.replace(new Element("key", "value")));
        Assert.assertNull(cache.replace(new Element("key", "value2")));

        Element e = new Element("key", "value");
        cache.put(e);
        
        Element e2 = new Element("key", "value2");
        Assert.assertEquals(e, cache.replace(e2));

        Assert.assertEquals(cache.get("key").getObjectValue(), e2.getObjectValue());
        
        try {
            cache.replace(null);
            Assert.fail("replace with null Element should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }
        
        try {
            cache.replace(new Element(null, "value1"));
            Assert.fail("replace with null keys should throw NPE");
        } catch (NullPointerException npe) {
            //expected
        }
    }
}
