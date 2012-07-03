/**
 *  Copyright Terracotta, Inc.
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

package net.sf.ehcache;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class DiskStorePathManagerTest {
    private DiskStorePathManager dspm1;
    private DiskStorePathManager dspm2;

    @After
    public void tearDown() {
        if (dspm2 != null) {
            dspm2.releaseLock();
        }
        if (dspm1 != null) {
            dspm1.releaseLock();
        }
    }

    @Test
    public void testDefault() throws Exception {
        dspm1 = new DiskStorePathManager();
        dspm1.getFile("foo");
        assertFalse(dspm1.isAutoCreated());
        assertTrue(dspm1.isDefault());

        dspm2 = new DiskStorePathManager();
        dspm2.getFile("foo");
        assertTrue(dspm2.isAutoCreated());
        assertFalse(dspm2.isDefault());

        Assert.assertFalse(getDiskStorePath(dspm1).equals(getDiskStorePath(dspm2)));
    }

    @Test
    public void testCollisionSameThread() throws Exception {
        String diskStorePath = getTempDir("testCollisionSameThread") + "/a/b/c";

        dspm1 = new DiskStorePathManager(diskStorePath);
        dspm1.getFile("foo");
        assertFalse(dspm1.isAutoCreated());
        assertFalse(dspm1.isDefault());

        dspm2 = new DiskStorePathManager(diskStorePath);
        dspm2.getFile("foo");
        assertTrue(dspm2.isAutoCreated());
        assertFalse(dspm2.isDefault());

        Assert.assertFalse(getDiskStorePath(dspm1).equals(getDiskStorePath(dspm2)));
    }

    @Test
    public void testCollisionDifferentThread() throws Exception {
        final String diskStorePath = getTempDir("testCollisionDifferentThread");
        dspm1 = new DiskStorePathManager(diskStorePath);
        dspm1.getFile("foo");
        Thread newThread = new Thread() {
            @Override
            public void run() {
                dspm2 = new DiskStorePathManager(diskStorePath);
                dspm2.getFile("foo");
            }
        };
        newThread.start();
        newThread.join(10 * 1000L);

        Assert.assertFalse(getDiskStorePath(dspm1).equals(getDiskStorePath(dspm2)));
    }

    @Test(expected = CacheException.class)
    public void testIllegalPath() {
        Assume.assumeTrue(System.getProperty("os.name").contains("Windows"));
        String diskStorePath = getTempDir("testIllegalPath") + "/com1";
        dspm1 = new DiskStorePathManager(diskStorePath);
        dspm1.getFile("foo");
    }

    private String getTempDir(String dirname) {
        String base = System.getProperty("basedir") != null ? System.getProperty("basedir") : ".";
        File target = new File(base, "target");
        File tempBase = new File(target, DiskStorePathManagerTest.class.getSimpleName());
        File tempDir = new File(tempBase, dirname);
        tempDir.mkdirs();
        Assert.assertTrue(tempDir.isDirectory());
        return tempDir.getAbsolutePath();
    }

    public static File getDiskStorePath(DiskStorePathManager manager) throws Exception {
        Field pathField = DiskStorePathManager.class.getDeclaredField("path");
        pathField.setAccessible(true);

        Object pathObject = pathField.get(manager);

        Field diskStorePathField = pathObject.getClass().getDeclaredField("diskStorePath");
        diskStorePathField.setAccessible(true);

        return (File) diskStorePathField.get(pathObject);
    }

}
