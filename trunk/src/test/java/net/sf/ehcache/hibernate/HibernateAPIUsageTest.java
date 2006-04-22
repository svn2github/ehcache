/**
 *  Copyright 2003-2006 Greg Luck
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

package net.sf.ehcache.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;

import net.sf.ehcache.CacheTest;
import net.sf.ehcache.AbstractCacheTest;


/**
 * Tests for a Cache
 *
 * @author Greg Luck, Claus Ibsen
 * @version $Id$
 */
public class HibernateAPIUsageTest extends AbstractCacheTest {
    private static final Log LOG = LogFactory.getLog(CacheTest.class.getName());


    /**
     * teardown
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }


    /**
     * Make sure ehcache works with one of the main projects using it: Hibernate-2.1.8
     */
    public void testAPIAsUsedByHibernate2() throws net.sf.hibernate.cache.CacheException {
        net.sf.hibernate.cache.EhCacheProvider provider = new net.sf.hibernate.cache.EhCacheProvider();
        provider.start(null);
        net.sf.hibernate.cache.Cache ehcache = provider.buildCache("hibernate2cache", null);
        assertNotNull(manager.getCache("hibernate2cache"));

        Serializable key = "key";
        Serializable value = "value";
        ehcache.put(key, value);
        assertEquals(value, ehcache.get(key));

        ehcache.remove(key);
        assertEquals(null, ehcache.get(key));
    }


    /**
     * Make sure ehcache works with one of the main projects using it: Hibernate-3.1.3
     */
    public void testAPIAsUsedByHibernate3() {
        org.hibernate.cache.EhCacheProvider provider = new org.hibernate.cache.EhCacheProvider();
        provider.start(null);
        org.hibernate.cache.Cache ehcache = provider.buildCache("hibernate3cache", null);
        assertNotNull(manager.getCache("hibernate3cache"));

        assertEquals("hibernate3cache", ehcache.getRegionName());
        Serializable key = "key";
        Serializable value = "value";
        ehcache.put(key, value);
        assertTrue(213 == ehcache.getSizeInMemory());
        assertEquals(value, ehcache.get(key));

        ehcache.remove(key);
        assertEquals(null, ehcache.get(key));

    }

    /**
     * Make sure ehcache works with one of the main projects using it: Hibernate-3.1.3
     */
    public void testRecommendedAPIHibernate32Ehcache12() {

        /*Shutdown cache manager so that hibernate can start one using the same ehcache.xml disk path
          because it does not use the singleton CacheManager any more */
        manager.shutdown();

        net.sf.ehcache.hibernate.EhCacheProvider provider = new net.sf.ehcache.hibernate.EhCacheProvider();
        provider.start(null);
        org.hibernate.cache.Cache ehcache = provider.buildCache("hibernate3cache", null);
        assertNotNull(ehcache.getRegionName());

        assertEquals("hibernate3cache", ehcache.getRegionName());
        Serializable key = "key";
        Serializable value = "value";
        ehcache.put(key, value);
        assertTrue(213 == ehcache.getSizeInMemory());
        assertEquals(value, ehcache.get(key));

        ehcache.remove(key);
        assertEquals(null, ehcache.get(key));

    }
}
