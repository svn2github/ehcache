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

package net.sf.ehcache;

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
    public void testCollisionSameThread() throws Exception {
        String diskStorePath = getTempDir("testCollisionSameThread") + "/a/b/c";
        dspm1 = DiskStorePathManager.createInstance(diskStorePath);
        dspm2 = DiskStorePathManager.createInstance(diskStorePath);

        Assert.assertFalse(getDiskStorePath(dspm1).equals(getDiskStorePath(dspm2)));
    }

    @Test
    public void testCollisionDifferentThread() throws Exception {
        final String diskStorePath = getTempDir("testCollisionDifferentThread");
        dspm1 = DiskStorePathManager.createInstance(diskStorePath);
        Thread newThread = new Thread() {
            @Override
            public void run() {
                dspm2 = DiskStorePathManager.createInstance(diskStorePath);
            }
        };
        newThread.start();
        newThread.join(10 * 1000L);

        Assert.assertFalse(getDiskStorePath(dspm1).equals(getDiskStorePath(dspm2)));
    }

    @Test(expected=CacheException.class)
    public void testIllegalPath() {
        Assume.assumeTrue(System.getProperty("os.name").contains("Windows"));
        String diskStorePath = getTempDir("testIllegalPath") + "/com1";
        dspm1 = DiskStorePathManager.createInstance(diskStorePath);
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

    private File getDiskStorePath(DiskStorePathManager manager) throws Exception {
        Field diskStorePathField = DiskStorePathManager.class.getDeclaredField("diskStorePath");
        diskStorePathField.setAccessible(true);
        return (File)diskStorePathField.get(manager);
    }


}
