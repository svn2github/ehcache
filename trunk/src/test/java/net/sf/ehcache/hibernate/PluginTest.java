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

import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;

/**
 * Tests for Hibernate Provider and Plugin
 * @version $Id: PluginTest.java,v 1.1 2006/03/09 06:38:20 gregluck Exp $
 * @author Greg Luck
 */
public class PluginTest extends TestCase {

    /** name for sample cache 1 */
    protected final String sampleCache1 = "sampleCache1";


    /**
     * Tests getting a plugin
     */
    public void testPlugin() throws Exception {
        Provider provider = new Provider();
        Plugin plugin = (Plugin) provider.buildCache("sampleCache1", null);
        assertNotNull(plugin);

    }

    /**
     * Tests getting a plugin
     */
    public void testPluginCreateRemoveCreate() throws Exception {
        Provider provider = new Provider();
        Plugin plugin = (Plugin) provider.buildCache("sampleCache1", null);
        plugin.get("test");
        plugin.destroy();
        try {
            plugin.get("test");
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().endsWith("Cache is not alive."));
        }
        plugin = (Plugin) provider.buildCache("sampleCache1", null);
        plugin.get("test");
        assertNotNull(plugin);

    }

    /**
     * Test size with put and remove.
     *  <cache name="sampleCache1"
     *   maxElementsInMemory="10000"
     *   eternal="false"
     *   timeToIdleSeconds="360"
     *   timeToLiveSeconds="1000"
     *   overflowToDisk="true"/>
     */
    public void testGetPutRemoveAndClear() throws Exception {
        Provider provider = new Provider();
        Plugin plugin = (Plugin) provider.buildCache("sampleCache1", null);
        plugin.put("key1", "value1");
        plugin.put("key2", "value2");
        assertEquals("value1", plugin.get("key1"));
        assertEquals("value2", plugin.get("key2"));
        plugin.remove("key1");
        plugin.remove("key2");
        assertNull(plugin.get("key1"));
        assertNull(plugin.get("key2"));

        plugin.put("key1", "value1");
        plugin.put("key2", "value2");
        assertEquals("value1", plugin.get("key1"));
        assertEquals("value2", plugin.get("key2"));
        plugin.clear();
        assertNull(plugin.get("key1"));
        assertNull(plugin.get("key2"));
    }

    /**
     * Tests getting a plugin. This will use settings from the default cache.
     */
    public void testPluginNoName() throws Exception {
        Provider provider = new Provider();
        try {
            Plugin plugin = (Plugin) provider.buildCache("sampleCacheNotThere", null);
            assertNotNull(plugin);
        } catch (net.sf.hibernate.cache.CacheException e) {
            assertTrue(e.getMessage().equals("No cache with name 'sampleCacheNotThere' found.  Check your cache configuration."));
        }




    }

    /** Tear down after test */
    protected void tearDown() throws Exception {
        CacheManager manager = CacheManager.getInstance();
        manager.shutdown();
    }
}
