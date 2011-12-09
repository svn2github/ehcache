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

package net.sf.ehcache.config;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheManager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests programmatically constructed Configuration instances
 *
 * @author Greg Luck
 * @version $Id$
 */
public class ConfigurationHelperTest extends AbstractCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationHelperTest.class.getName());


    /**
     * Should not give exceptions
     */
    @Test
    public void testValidParameters() {
        Configuration configuration = new Configuration();
        CacheConfiguration defaultCache = new CacheConfiguration()
                .eternal(false);

        ConfigurationHelper configurationHelper =
                new ConfigurationHelper(manager, configuration);
        assertNotNull(configurationHelper);
    }

    /**
     * Will fail if all params null
     */
    @Test
    public void testNullParameters() {
        try {
            new ConfigurationHelper((CacheManager) null, null);
            fail();
        } catch (Exception e) {
            //expected
            LOG.debug("Expected exception " + e.getMessage() + ". Initial cause was " + e.getMessage(), e);
        }
    }

    /**
     * Test the expansion of Java system properties.
     * These can be mixed in with other path information, in which case they should be expanded and the other
     * path information catenatated.
     *
     * @throws IOException
     */
    @Test
    public void testDiskStorePathExpansion() throws IOException {
        DiskStoreConfiguration diskStore = new DiskStoreConfiguration();

        specificPathTest(diskStore, "java.io.tmpdir", "java.io.tmpdir");
        specificPathTest(diskStore, "java.io.tmpdir/cacheManager1", "java.io.tmpdir");
        specificPathTest(diskStore, "java.io.tmpdir/cacheManager1/", "java.io.tmpdir");
        specificPathTest(diskStore, "user.dir", "user.dir");
        specificPathTest(diskStore, "user.dir/cacheManager1", "user.dir");
        specificPathTest(diskStore, "user.dir/cacheManager1/", "user.dir");
        specificPathTest(diskStore, "user.home", "user.home");
        specificPathTest(diskStore, "user.home/cacheManager1", "user.home");
        specificPathTest(diskStore, "user.home/cacheManager1/", "user.home");
        specificPathTest(diskStore, "user.home/cacheManager1/dir1", "user.home");

        System.setProperty("ehcache.disk.store.dir", "/tmp");
        specificPathTest(diskStore, "ehcache.disk.store.dir/cacheManager1/dir1", "user.home");


    }

    private void specificPathTest(DiskStoreConfiguration diskStoreConfiguration, String specifiedPath, String systemProperty) {
        diskStoreConfiguration.setPath(specifiedPath);
        String expandedPath = diskStoreConfiguration.getPath();
        assertTrue(expandedPath.indexOf(systemProperty) == -1);

        File diskDir = null;
        try {
            diskDir = new File(expandedPath);
            diskDir.mkdirs();
            assertTrue(diskDir.exists());
            assertTrue(diskDir.isDirectory());
        } finally {
            //delete only paths we created, not existing system paths, for repeatability
            while (diskDir.getPath().indexOf("cacheManager1") != -1) {
                diskDir.delete();
                diskDir = diskDir.getParentFile();
            }
        }
    }
}
