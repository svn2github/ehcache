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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.Primitive;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.compound.CompoundStore;
import net.sf.ehcache.store.compound.impl.DiskPersistentStore;
import net.sf.ehcache.store.compound.impl.OverflowToDiskStore;
import net.sf.ehcache.util.PropertyUtil;
import net.sf.ehcache.util.RetryAssert;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test cases for the DiskStore.
 *
 * @author <a href="mailto:amurdoch@thoughtworks.com">Adam Murdoch</a>
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 *          <p/>
 *          total time 149 old i/o
 *          total time 133, 131, 130 nio
 */
public class DiskStoreTest extends AbstractCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(DiskStoreTest.class.getName());
    private static final int ELEMENT_ON_DISK_SIZE = 1270;
    private CacheManager manager2;

    /**
     * teardown
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (manager2 != null) {
            manager2.shutdown();
        }
        deleteFile("persistentLongExpiryIntervalCache");
        deleteFile("fileTest");
        deleteFile("testPersistent");
        deleteFile("testLoadPersistent");
        deleteFile("testPersistentWithDelete");
    }

    /**
     * Creates a store which is non-expiring so that we can check for
     * size-related characteristics without elements being deleted under us.
     */
    private Store createNonExpiringDiskStore() {
        Cache cache = new Cache("test/NonPersistent", 1, true, false, 2, 1, false, 1);
        manager.addCache(cache);
        return cache.getStore();
    }

    private Store createDiskStore() {
        Cache cache = new Cache("test/NonPersistent", 1, true, false, 2, 1, false, 1);
        manager.addCache(cache);
        return cache.getStore();
    }

    private Store createPersistentDiskStore(String cacheName) {
        Cache cache = new Cache(cacheName, 10000, true, false, 5, 1, true, 600);
        manager.addCache(cache);
        return cache.getStore();
    }

    private Store createAutoPersistentDiskStore(String cacheName) {
        Cache cache = new Cache(cacheName, 10000, true, false, 5, 1, true, 600);
        manager2 = new CacheManager();
        //manager.setDiskStorePath(System.getProperty("java.io.tmpdir") + File.separator + DiskStore.generateUniqueDirectory());
        manager2.addCache(cache);
        return cache.getStore();
    }

    private Store createPersistentDiskStoreFromCacheManager() {
        Cache cache = manager.getCache("persistentLongExpiryIntervalCache");
        return cache.getStore();
    }

    private Store createCapacityLimitedDiskStore() {
        Cache cache = new Cache("test/CapacityLimited", 1, MemoryStoreEvictionPolicy.LRU, true, null, true,
                 0, 0, false, 600, null, null, 50);
        manager.addCache(cache);
        return cache.getStore();
    }

    private Store createStripedDiskStore(int stripes) {
        CacheConfiguration config = new CacheConfiguration("test/NonPersistentStriped_" + stripes, 10000).overflowToDisk(true).eternal(false)
                .timeToLiveSeconds(2).timeToIdleSeconds(1).diskPersistent(false).diskExpiryThreadIntervalSeconds(1).diskAccessStripes(stripes);
        Cache cache = new Cache(config);
        manager.addCache(cache);
        return cache.getStore();
    }

    /**
     * Test to help debug DiskStore test
     */
    @Test
    public void testNothing() {
        //just tests setup and teardown
    }

    /**
     * Tests that a file is created with the right size after puts, and that the file is
     * deleted on disposal
     */
    @Test
    public void testNonPersistentStore() throws IOException, InterruptedException {
        Store diskStore = createNonExpiringDiskStore();
        File dataFile = ((OverflowToDiskStore) diskStore).getDataFile();

        //100 + 1 for the in-memory capacity
        for (int i = 0; i < 101; i++) {
            byte[] data = new byte[1024];
            diskStore.put(new Element("key" + (i + 100), data));
        }
        waitShorter();
        assertEquals(ELEMENT_ON_DISK_SIZE * 100, diskStore.getOnDiskSizeInBytes());

        assertEquals(100, diskStore.getOnDiskSize());
        assertEquals(101, diskStore.getSize());
        diskStore.dispose();
        Thread.sleep(1);
        assertFalse("File exists", dataFile.exists());
    }


    /**
     * Tests the auto generated directories are deleted
     */
    @Test
    public void testDeleteAutoGenerated() {
        manager2 = new CacheManager();
        String diskPath = manager2.getDiskStorePath();
        File dataDirectory = new File(diskPath);
        assertTrue(dataDirectory.exists());
        assertTrue(dataDirectory.isDirectory());
        manager2.shutdown();
        //should now be deleted
        assertTrue(!dataDirectory.exists());
        assertTrue(!dataDirectory.isDirectory());
    }


    /**
     * Tests that the Disk Store can be changed
     */
    @Test
    public void testSetDiskStorePath() throws IOException, InterruptedException {
        Cache cache = new Cache("testChangePath", 10000, true, false, 5, 1, true, 600);
        manager2 = new CacheManager();
        cache.setDiskStorePath(System.getProperty("java.io.tmpdir") + File.separator + "changedDiskStorePath");
        manager2.addCache(cache);
        DiskPersistentStore diskStore = (DiskPersistentStore) cache.getStore();
        File dataFile = diskStore.getDataFile();
        assertTrue("File exists", dataFile.exists());
    }

    /**
     * Tests that a file is created with the right size after puts, and that the file is not
     * deleted on disposal
     * <p/>
     * This test uses a preconfigured cache from the test cache.xml. Note that teardown causes
     * an exception because the disk store is being shut down twice.
     */
    @Test
    public void testPersistentStore() throws IOException, InterruptedException, CacheException {
        //initialise
        Store store = createPersistentDiskStoreFromCacheManager();
        store.removeAll();

        File dataFile = ((DiskPersistentStore) store).getDataFile();

        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[1024];
            store.put(new Element("key" + (i + 100), data));
        }
        waitShorter();
        assertEquals(100, store.getSize());
        store.dispose();

        assertTrue("File exists", dataFile.exists());
        assertEquals(100 * ELEMENT_ON_DISK_SIZE, dataFile.length());
    }

    /**
     * An integration test, at the CacheManager level, to make sure persistence works
     */
    @Test
    public void testPersistentStoreFromCacheManager() throws IOException, InterruptedException, CacheException {
        //initialise with an instance CacheManager so that the following line actually does something
        CacheManager manager = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-disk.xml");
        Ehcache cache = manager.getCache("persistentLongExpiryIntervalCache");

        LOG.info("DiskStore path: {}", manager.getDiskStorePath());

        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[1024];
            cache.put(new Element("key" + (i + 100), data));
        }
        assertEquals(100, cache.getSize());

        manager.shutdown();

        manager = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-disk.xml");
        cache = manager.getCache("persistentLongExpiryIntervalCache");
        assertEquals(100, cache.getSize());

        manager.shutdown();

    }

    /**
     * An integration test, at the CacheManager level, to make sure persistence works
     * This test checks the config where a cache is not configured overflow to disk, but is disk persistent.
     * It should work by putting elements in the DiskStore initially and then loading them into memory as they
     * are called.
     */
    @Test
    public void testPersistentNonOverflowToDiskStoreFromCacheManager() throws IOException, InterruptedException, CacheException {
        //initialise with an instance CacheManager so that the following line actually does something
        CacheManager manager = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-disk.xml");
        Ehcache cache = manager.getCache("persistentLongExpiryIntervalNonOverflowCache");

        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[1024];
            cache.put(new Element("key" + (i + 100), data));
        }
        assertEquals(100, cache.getSize());

        manager.shutdown();

        manager = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-disk.xml");
        cache = manager.getCache("persistentLongExpiryIntervalNonOverflowCache");

        //Now check that the DiskStore is involved in Cache methods it needs to be involved in.
        RetryAssert.assertEquals(100, cache.getSize(), 5);

        assertEquals(100, cache.getDiskStoreSize());
        assertEquals(100, cache.getKeysNoDuplicateCheck().size());
        assertEquals(100, cache.getKeys().size());
        assertEquals(100, cache.getKeysWithExpiryCheck().size());

        //now check some of the Cache methods work
        assertNotNull(cache.get("key100"));
        assertNotNull(cache.getQuiet("key100"));
        cache.remove("key100");
        assertNull(cache.get("key100"));
        assertNull(cache.getQuiet("key100"));
        cache.removeAll();
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getDiskStoreSize());

        manager.shutdown();
    }

    /**
     * Tests that we can save and load a persistent store in a repeatable way
     */
    @Test
    public void testLoadPersistentStore() throws IOException, InterruptedException {
        //initialise
        String cacheName = "testLoadPersistent";
        Store store = createPersistentDiskStore(cacheName);
        store.removeAll();
        waitShorter();


        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[1024];
            store.put(new Element("key" + (i + 100), data));
        }
        store.flush();
        waitShorter();
        assertEquals(ELEMENT_ON_DISK_SIZE * 100, store.getOnDiskSizeInBytes());
        assertEquals(100, store.getSize());
        manager.removeCache(cacheName);
        Thread.sleep(3000);
        //check that we can create and dispose several times with no problems and no lost data
        for (int i = 0; i < 10; i++) {
            store = createPersistentDiskStore(cacheName);
            File dataFile = ((DiskPersistentStore) store).getDataFile();
            assertTrue("File exists", dataFile.exists());
            assertEquals(100 * ELEMENT_ON_DISK_SIZE, dataFile.length());
            assertEquals(100, store.getSize());

            manager.removeCache(cacheName);

            assertTrue("File exists", dataFile.exists());
            assertEquals(100 * ELEMENT_ON_DISK_SIZE, dataFile.length());
        }
    }

    /**
     * Any disk store with an auto generated random directory should not be able to be loaded.
     */
    @Test
    public void testCannotLoadPersistentStoreWithAutoDir() throws IOException, InterruptedException {
        //initialise
        String cacheName = "testPersistent";
        Store diskStore = createAutoPersistentDiskStore(cacheName);
        diskStore.removeAll();


        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[1024];
            diskStore.put(new Element("key" + (i + 100), data));
        }
        waitLonger();
        assertEquals(ELEMENT_ON_DISK_SIZE * 100, diskStore.getOnDiskSizeInBytes());
        assertEquals(100, diskStore.getSize());
        manager2.removeCache(cacheName);
        Thread.sleep(1000);

        Cache cache = new Cache(cacheName, 10000, true, false, 5, 1, true, 600);
        manager2.addCache(cache);

        File dataFile = ((DiskPersistentStore) diskStore).getDataFile();
        assertTrue("File exists", dataFile.exists());
        assertEquals(0, dataFile.length());
        assertEquals(0, cache.getSize());
        manager.removeCache(cacheName);
        assertTrue("File exists", dataFile.exists());
        assertEquals(0, dataFile.length());
    }

    /**
     * Tests that we can save and load a persistent store in a repeatable way,
     * and delete and add data.
     */
    @Test
    public void testLoadPersistentStoreWithDelete() throws IOException, InterruptedException {
        //initialise
        String cacheName = "testPersistentWithDelete";
        Store diskStore = createPersistentDiskStore(cacheName);
        diskStore.removeAll();


        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[1024];
            diskStore.put(new Element("key" + (i + 100), data));
        }
        waitShorter();
        assertEquals(ELEMENT_ON_DISK_SIZE * 100, diskStore.getOnDiskSizeInBytes());
        assertEquals(100, diskStore.getSize());
        manager.removeCache(cacheName);

        diskStore = createPersistentDiskStore(cacheName);
        File dataFile = ((DiskPersistentStore) diskStore).getDataFile();
        assertTrue("File exists", dataFile.exists());
        assertEquals(100 * ELEMENT_ON_DISK_SIZE, dataFile.length());
        assertEquals(100, diskStore.getSize());

        diskStore.remove("key100");
        assertEquals(100 * ELEMENT_ON_DISK_SIZE, dataFile.length());
        assertEquals(99, diskStore.getSize());

        manager.removeCache(cacheName);

        diskStore = createPersistentDiskStore(cacheName);
        assertTrue("File exists", dataFile.exists());
        assertEquals(100 * ELEMENT_ON_DISK_SIZE, dataFile.length());
        assertEquals(99, diskStore.getSize());

        diskStore.put(new Element("key100", new byte[1024]));
        diskStore.flush();
        waitShorter();
        assertEquals(100 * ELEMENT_ON_DISK_SIZE, dataFile.length());
        assertEquals(100, diskStore.getSize());
    }

    /**
     * Tests that we can load a store after the index has been corrupted
     */
    @Test
    public void testLoadPersistentStoreAfterCorruption() throws IOException, InterruptedException {
        //initialise
        String cacheName = "testPersistent";
        Store diskStore = createPersistentDiskStore(cacheName);
        diskStore.removeAll();


        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[1024];
            diskStore.put(new Element("key" + (i + 100), data));
        }
        waitShorter();
        assertEquals(ELEMENT_ON_DISK_SIZE * 100, diskStore.getOnDiskSizeInBytes());
        assertEquals(100, diskStore.getSize());
        manager.removeCache(cacheName);

        File indexFile = ((DiskPersistentStore) diskStore).getIndexFile();
        FileOutputStream fout = new FileOutputStream(indexFile);
        //corrupt the index file
        fout.write(new byte[]{'q', 'w', 'e', 'r', 't', 'y'});
        fout.close();
        diskStore = createPersistentDiskStore(cacheName);
        File dataFile = ((DiskPersistentStore) diskStore).getDataFile();
        assertTrue("File exists", dataFile.exists());

        //Make sure the data file got recreated since the index was corrupt
        assertEquals("Data file was not recreated", 0, dataFile.length());
        assertEquals(0, diskStore.getSize());
    }

    /**
     * Tests that we can save and load a persistent store in a repeatable way,
     * and delete and add data.
     */
    @Test
    public void testFreeSpaceBehaviour() throws IOException, InterruptedException {
        //initialise
        String cacheName = "testPersistent";
        Store diskStore = createPersistentDiskStore(cacheName);
        diskStore.removeAll();

        byte[] data = new byte[1024];
        for (int i = 0; i < 100; i++) {
            diskStore.put(new Element("key" + (i + 100), data));
        }
        waitShorter();
        assertEquals(ELEMENT_ON_DISK_SIZE * 100, diskStore.getOnDiskSizeInBytes());
        assertEquals(100, diskStore.getSize());
        manager.removeCache(cacheName);

        diskStore = createPersistentDiskStore(cacheName);
        File dataFile = ((DiskPersistentStore) diskStore).getDataFile();
        assertTrue("File exists", dataFile.exists());
        assertEquals(100 * ELEMENT_ON_DISK_SIZE, dataFile.length());
        assertEquals(100, diskStore.getSize());

        diskStore.remove("key100");
        diskStore.remove("key101");
        diskStore.remove("key102");
        diskStore.remove("key103");
        diskStore.remove("key104");

        diskStore.put(new Element("key100", data));
        diskStore.put(new Element("key101", data));
        waitShorter();

        //The file does not shrink.
        assertEquals(100 * ELEMENT_ON_DISK_SIZE, dataFile.length());
        assertEquals(97, diskStore.getSize());

        diskStore.put(new Element("key102", data));
        diskStore.put(new Element("key103", data));
        diskStore.put(new Element("key104", data));
        diskStore.put(new Element("key201", data));
        diskStore.put(new Element("key202", data));
        waitShorter();
        assertEquals(102 * ELEMENT_ON_DISK_SIZE, dataFile.length());
        assertEquals(102, diskStore.getSize());
        manager.removeCache(cacheName);
        assertTrue("File exists", dataFile.exists());
        assertEquals(102 * ELEMENT_ON_DISK_SIZE, dataFile.length());
    }

    /**
     * Tests looking up an entry that does not exist.
     */
    @Test
    public void testGetUnknownThenKnown() throws Exception {
        final Store diskStore = createDiskStore();
        assertNull(diskStore.get("key1"));
        diskStore.put(new Element("key1", "value"));
        diskStore.put(new Element("key2", "value"));
        assertNotNull(diskStore.get("key1"));
        assertNotNull(diskStore.get("key2"));
    }

    /**
     * Tests looking up an entry that does not exist.
     */
    @Test
    public void testGetQuietUnknownThenKnown() throws Exception {
        final Store diskStore = createDiskStore();
        assertNull(diskStore.getQuiet("key1"));
        diskStore.put(new Element("key1", "value"));
        diskStore.put(new Element("key2", "value"));
        assertNotNull(diskStore.getQuiet("key1"));
        assertNotNull(diskStore.getQuiet("key2"));
    }

    /**
     * Tests adding an entry.
     */
    @Test
    public void testPut() throws Exception {
        final Store diskStore = createDiskStore();

        // Make sure the element is not found
        assertEquals(0, diskStore.getSize());
        assertNull(diskStore.get("key"));

        // Add the element
        final String value = "value";
        diskStore.put(new Element("key1", value));
        diskStore.put(new Element("key2", value));

        // Get the element
        assertEquals(2, diskStore.getSize());
        assertEquals(1, diskStore.getOnDiskSize());

        Element element1 = diskStore.get("key1");
        Element element2 = diskStore.get("key2");
        assertNotNull(element1);
        assertNotNull(element2);
        assertEquals(value, element1.getObjectValue());
        assertEquals(value, element2.getObjectValue());
    }


    /**
     *
     */
    @Test
    public void testLFUEvictionFromDiskStore() throws IOException, InterruptedException {
        Cache cache = new Cache("testNonPersistent", 1, MemoryStoreEvictionPolicy.LFU, true,
                null, false, 2000, 1000, false, 1, null, null, 10);
        manager.addCache(cache);
        Store store = cache.getStore();

        Element element;

        assertEquals(0, store.getSize());

        for (int i = 0; i < 11; i++) {
            element = new Element("key" + i, "value" + i);
            cache.put(element);
            if (i > 0) {
                cache.get("key" + i);
                cache.get("key" + i);
                cache.get("key" + i);
                cache.get("key" + i);
            }
        }

        //allow to move through spool
        Thread.sleep(220);
        assertEquals(10, store.getOnDiskSize());

        element = new Element("keyNew", "valueNew");
        store.put(element);

        //allow to get out of spool
        Thread.sleep(220);
        assertEquals(10, store.getOnDiskSize());
        //check new element not evicted
        assertNotNull(store.get(element.getObjectKey()));
        //check evicted honours LFU policy
        assertNull(store.get("key0"));

        for (int i = 0; i < 2000; i++) {
            store.put(new Element("" + i, new Date()));
        }
        //wait for spool to empty
        waitLonger();

        RetryAssert.assertEquals(10, store.getOnDiskSize(), 5);
    }

    /**
     * Tests the loading of classes
     */
    @Test
    public void testClassloading() throws Exception {
        final Store diskStore = createDiskStore();

        Long value = Long.valueOf(123L);
        diskStore.put(new Element("key1", value));
        diskStore.put(new Element("key2", value));
        Thread.sleep(1000);
        Element element1 = diskStore.get("key1");
        Element element2 = diskStore.get("key2");
        assertEquals(value, element1.getObjectValue());
        assertEquals(value, element2.getObjectValue());


        Primitive primitive = new Primitive();
        primitive.integerPrimitive = 123;
        primitive.longPrimitive = 456L;
        primitive.bytePrimitive = "a".getBytes()[0];
        primitive.charPrimitive = 'B';
        primitive.booleanPrimitive = false;

        //test Serializability
        ByteArrayOutputStream outstr = new ByteArrayOutputStream();
        ObjectOutputStream objstr = new ObjectOutputStream(outstr);
        objstr.writeObject(new Element("key", value));
        objstr.close();


        diskStore.put(new Element("primitive1", primitive));
        diskStore.put(new Element("primitive2", primitive));
        Thread.sleep(1000);
        Element primitive1 = diskStore.get("primitive1");
        Element primitive2 = diskStore.get("primitive2");
        assertEquals(primitive, primitive1.getObjectValue());
        assertEquals(primitive, primitive2.getObjectValue());
    }


    /**
     * Tests adding an entry and waiting for it to be written.
     */
    @Test
    public void testPutSlow() throws Exception {
        final CompoundStore diskStore = (CompoundStore) createDiskStore();

        // Make sure the element is not found
        assertEquals(0, diskStore.getSize());
        assertNull(diskStore.get("key"));

        // Add the element
        final String value = "value";
        diskStore.put(new Element("key1", value));
        diskStore.put(new Element("key2", value));

        // Wait
        waitShorter();

        // Get the element
        assertEquals(1, diskStore.getOnDiskSize());
        assertEquals(2, diskStore.getSize());

        Object o1 = diskStore.unretrievedGet("key1");
        Object o2 = diskStore.unretrievedGet("key2");
        if (o1 instanceof Element) {
            assertEquals("DiskMarker", o2.getClass().getSimpleName());
        } else if (o2 instanceof Element) {
            assertEquals("DiskMarker", o1.getClass().getSimpleName());
        } else {
            fail("One of the elements should be in memory");
        }

        Element element1 = diskStore.get("key1");
        Element element2 = diskStore.get("key2");
        assertNotNull(element1);
        assertNotNull(element2);
        assertEquals(value, element1.getObjectValue());
        assertEquals(value, element2.getObjectValue());
    }

    /**
     * Tests removing an entry.
     */
    @Test
    public void testRemove() throws Exception {
        final Store diskStore = createDiskStore();

        // Add the entry
        final String value = "value";
        diskStore.put(new Element("key1", value));
        diskStore.put(new Element("key2", value));

        // Check the entry is there
        assertEquals(1, diskStore.getOnDiskSize());
        assertEquals(2, diskStore.getSize());

        assertNotNull(diskStore.get("key1"));
        assertNotNull(diskStore.get("key2"));

        // Remove it
        diskStore.remove("key1");
        diskStore.remove("key2");

        // Check the entry is not there
        assertEquals(0, diskStore.getOnDiskSize());
        assertEquals(0, diskStore.getSize());
        assertNull(diskStore.get("key1"));
        assertNull(diskStore.get("key2"));
    }

    /**
     * Tests removing an entry, after it has been written
     */
    @Test
    public void testRemoveSlow() throws Exception {
        final CompoundStore diskStore = (CompoundStore) createDiskStore();

        // Add the entry
        final String value = "value";
        diskStore.put(new Element("key1", value));
        diskStore.put(new Element("key2", value));

        // Wait for the entry
        waitShorter();

        // Check the entry is there
        assertEquals(1, diskStore.getOnDiskSize());
        assertEquals(2, diskStore.getSize());
        Object o1 = diskStore.unretrievedGet("key1");
        Object o2 = diskStore.unretrievedGet("key2");
        if (o1 instanceof Element) {
            assertEquals("DiskMarker", o2.getClass().getSimpleName());
        } else if (o2 instanceof Element) {
            assertEquals("DiskMarker", o1.getClass().getSimpleName());
        } else {
            fail("One of the elements should be in memory");
        }

        // Remove it
        diskStore.remove("key1");
        diskStore.remove("key2");

        // Check the entry is not there
        assertEquals(0, diskStore.getSize());
        assertEquals(0, diskStore.getOnDiskSize());
        assertNull(diskStore.get("key1"));
        assertNull(diskStore.get("key2"));
    }

    /**
     * Tests removing all the entries.
     */
    @Test
    public void testRemoveAll() throws Exception {
        final Store diskStore = createDiskStore();

        // Add the entry
        final String value = "value";
        diskStore.put(new Element("key1", value));
        diskStore.put(new Element("key2", value));

        // Check the entry is there
        assertNotNull(diskStore.get("key1"));
        assertNotNull(diskStore.get("key2"));

        // Remove it
        diskStore.removeAll();

        // Check the entry is not there
        assertEquals(0, diskStore.getSize());
        assertEquals(0, diskStore.getOnDiskSize());
        assertNull(diskStore.get("key1"));
        assertNull(diskStore.get("key2"));
    }

    /**
     * Tests removing all the entries, after they have been written to disk.
     */
    @Test
    public void testRemoveAllSlow() throws Exception {
        final CompoundStore diskStore = (CompoundStore) createDiskStore();

        // Add the entry
        final String value = "value";
        diskStore.put(new Element("key1", value));
        diskStore.put(new Element("key2", value));

        // Wait
        waitShorter();

        // Check the entry is there
        Object o1 = diskStore.unretrievedGet("key1");
        Object o2 = diskStore.unretrievedGet("key2");
        if (o1 instanceof Element) {
            assertEquals("DiskMarker", o2.getClass().getSimpleName());
        } else if (o2 instanceof Element) {
            assertEquals("DiskMarker", o1.getClass().getSimpleName());
        } else {
            fail("One of the elements should be in memory");
        }

        // Remove it
        diskStore.removeAll();

        // Check the entry is not there
        assertEquals(0, diskStore.getSize());
        assertEquals(0, diskStore.getOnDiskSize());
        assertNull(diskStore.get("key1"));
        assertNull(diskStore.get("key2"));
    }

    /**
     * Tests bulk load.
     */
    @Test
    public void testBulkLoad() throws Exception {
        final Store diskStore = createDiskStore();

        final Random random = new Random();

        // Add a bunch of entries
        for (int i = 0; i < 500; i++) {
            // Use a random length value
            final String key = "key" + i;
            final String value = "This is a value" + random.nextInt(1000);

            // Add an element, and make sure it is present
            Element element = new Element(key, value);
            diskStore.put(element);
            element = diskStore.get(key);
            assertNotNull(element);

            // Chuck in a delay, to give the spool thread a chance to catch up
            Thread.sleep(2);

            // Remove the element
            diskStore.remove(key);
            element = diskStore.get(key);
            assertNull(element);

            element = new Element(key, value);
            diskStore.put(element);
            element = diskStore.get(key);
            assertNotNull(element);

            // Chuck in a delay
            Thread.sleep(2);
        }
    }

    /**
     * Tests for element expiry.
     */
    @Test
    @Ignore("This is fixed in trunk, but fails racily here, as the required fixes have not been merged across")
    public void testExpiry() throws Exception {
        // Create a diskStore with a cranked up expiry thread
        final Store diskStore = createDiskStore();

        // Add an element that will expire.
        diskStore.put(new Element("key1", "value", false, 1, 1));
        diskStore.put(new Element("key2", "value", false, 1, 1));
        assertNotNull(diskStore.get("key1"));
        assertNotNull(diskStore.get("key2"));
        assertEquals(2, diskStore.getSize());
        assertEquals(1, diskStore.getOnDiskSize());

        // Wait a couple of seconds
        Thread.sleep(3000);

        Element e1 = diskStore.get("key1");
        Element e2 = diskStore.get("key2");

        if (e1 != null) {
            assertNull(e2);
        } else if (e2 != null) {
            assertNull(e1);
        } else {
            fail("One of the Elements should have been in memory - and hence not be expired");
        }
    }

    /**
     * Checks that the expiry thread runs and expires elements which has the effect
     * of preventing the disk store from continously growing.
     * Ran for 6 hours through 10000 outer loops. No memory use increase.
     * Using a key of "key" + i * outer) you get early slots that cannot be reused. The DiskStore
     * actual size therefore starts at 133890 and ends at 616830. There is quite a lot of space
     * that cannot be used because of fragmentation. Question? Should an effort be made to coalesce
     * fragmented space? Unlikely in production to get contiguous fragments as in the first form
     * of this test.
     * <p/>
     * Using a key of Integer.valueOf(i * outer) the size stays constant at 140800.
     *
     * @throws InterruptedException
     */
    @Test
    public void testExpiryWithSize() throws InterruptedException {
        Store diskStore = createDiskStore();
        diskStore.removeAll();

        byte[] data = new byte[1024];
        for (int outer = 1; outer <= 10; outer++) {
            for (int i = 0; i < 101; i++) {
                Element element = new Element(Integer.valueOf(i * outer), data);
                element.setTimeToLive(1);
                diskStore.put(element);
            }
            waitLonger();
            int predictedSize = (ELEMENT_ON_DISK_SIZE + 68) * 100;
            long actualSize = diskStore.getOnDiskSizeInBytes();
            LOG.info("Predicted Size: " + predictedSize + " Actual Size: " + actualSize);
            assertEquals(predictedSize, actualSize);
            LOG.info("Memory Use: " + measureMemoryUse());
        }
    }

    /**
     * Waits for all spooled elements to be written to disk.
     */
    private static void waitShorter() throws InterruptedException {
        Thread.sleep((long) (300 + 100 * getSpeedAdjustmentFactor()));
    }

    private static float getSpeedAdjustmentFactor() {
        final String speedAdjustmentFactorString = PropertyUtil.extractAndLogProperty("net.sf.ehcache.speedAdjustmentFactor", System.getProperties());
        if(speedAdjustmentFactorString != null) {
            return Float.parseFloat(speedAdjustmentFactorString);
        } else {
            return 1;
        }
    }


    /**
     * Waits for all spooled elements to be written to disk.
     */
    private static void waitLonger() throws InterruptedException {
        Thread.sleep((long) (300 + 500 * getSpeedAdjustmentFactor()));
    }


    /**
     * Multi-thread read-only test. Will fail on memory constrained VMs
     */
    @Test
    public void testReadOnlyMultipleThreads() throws Exception {
        final Store diskStore = createNonExpiringDiskStore();

        // Add a couple of elements
        diskStore.put(new Element("key0", "value"));
        diskStore.put(new Element("key1", "value"));
        diskStore.put(new Element("key2", "value"));
        diskStore.put(new Element("key3", "value"));

        // Wait for the elements to be written
        waitShorter();

        // Run a set of threads, that attempt to fetch the elements
        final List executables = new ArrayList();
        for (int i = 0; i < 20; i++) {
            final String key = "key" + (i % 4);
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    final Element element = diskStore.get(key);
                    assertNotNull(element);
                    assertEquals("value", element.getObjectValue());
                }
            };
            executables.add(executable);
        }
        runThreads(executables);
    }

    /**
     * Multi-thread concurrent read remove test.
     */
    @Test
    public void testReadRemoveMultipleThreads() throws Exception {
        final Random random = new Random();
        final Store diskStore = createDiskStore();

        diskStore.put(new Element("key", "value"));

        // Run a set of threads that get, put and remove an entry
        final List executables = new ArrayList();
        for (int i = 0; i < 5; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    for (int i = 0; i < 100; i++) {
                        diskStore.put(new Element("key" + random.nextInt(100), "value"));
                    }
                }
            };
            executables.add(executable);
        }
        for (int i = 0; i < 5; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    for (int i = 0; i < 100; i++) {
                        diskStore.remove("key" + random.nextInt(100));
                    }
                }
            };
            executables.add(executable);
        }

        runThreads(executables);
    }

    @Test
    public void testReadRemoveMultipleThreadsMultipleStripes() throws Exception {
        for (int stripes = 0; stripes < 10; stripes++) {
            final Random random = new Random();
            final Store diskStore = createStripedDiskStore(stripes);

            diskStore.put(new Element("key", "value"));

            // Run a set of threads that get, put and remove an entry
            final List executables = new ArrayList();
            for (int i = 0; i < 5; i++) {
                final Executable executable = new Executable() {
                    public void execute() throws Exception {
                        for (int i = 0; i < 100; i++) {
                            diskStore.put(new Element("key" + random.nextInt(100), "value"));
                        }
                    }
                };
                executables.add(executable);
            }
            for (int i = 0; i < 5; i++) {
                final Executable executable = new Executable() {
                    public void execute() throws Exception {
                        for (int i = 0; i < 100; i++) {
                            diskStore.remove("key" + random.nextInt(100));
                        }
                    }
                };
                executables.add(executable);
            }

            runThreads(executables);
        }
    }

    /**
     * Tests how data is written to a random access file.
     * <p/>
     * It makes sure that bytes are immediately written to disk after a write.
     */
    @Test
    public void testWriteToFile() throws IOException {
        // Create and set up file
        String dataFileName = "fileTest";
        RandomAccessFile file = getRandomAccessFile(dataFileName);

        //write data to the file
        byte[] buffer = new byte[1024];
        for (int i = 0; i < 100; i++) {
            file.write(buffer);
        }

        assertEquals(1024 * 100, file.length());

    }

    private RandomAccessFile getRandomAccessFile(String name) throws FileNotFoundException {
        String diskPath = System.getProperty("java.io.tmpdir");
        final File diskDir = new File(diskPath);
        File dataFile = new File(diskDir, name + ".data");
        return new RandomAccessFile(dataFile, "rw");
    }



    /**
     * This test is designed to be used with a profiler to explore the ways in which DiskStore
     * uses memory. It does not do much on its own.
     */
    @Test
    public void testOutOfMemoryErrorOnOverflowToDisk() throws Exception {

        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1000, MemoryStoreEvictionPolicy.LRU, true, null, false, 500, 500, false, 1, null);
        manager.addCache(cache);
        int i = 0;

        Random random = new Random();
        for (; i < 5500; i++) {
            byte[] bytes = new byte[10000];
            random.nextBytes(bytes);
            cache.put(new Element("" + i, bytes));
        }
        LOG.info("Elements written: " + i);
        //Thread.sleep(100000);
    }

    /**
     * Java is not consistent with trailing file separators, believe it or not!
     * http://www.rationalpi.com/blog/replyToComment.action?entry=1146628709626&comment=1155660875090
     * Can we fix c:\temp\\greg?
     */
    @Test
    public void testWindowsAndSolarisTempDirProblem() throws InterruptedException {

        String originalPath = "c:" + File.separator + "temp" + File.separator + File.separator + "greg";
        //Fix dup separator
        String translatedPath = DiskStoreConfiguration.replaceToken(File.separator + File.separator,
                File.separator, originalPath);
        assertEquals("c:" + File.separator + "temp" + File.separator + "greg", translatedPath);
        //Ignore single separators
        translatedPath = DiskStoreConfiguration.replaceToken(File.separator + File.separator, File.separator, originalPath);
        assertEquals("c:" + File.separator + "temp" + File.separator + "greg", translatedPath);

        Thread.sleep(500);
    }

    @Test
    public void testShrinkingAndGrowingDiskStore() throws InterruptedException {
        OverflowToDiskStore store = (OverflowToDiskStore) createCapacityLimitedDiskStore();
        int i = 0;
        store.put(new Element(Integer.valueOf(i++), new byte[100]));
        while (true) {
            int beforeSize = store.getOnDiskSize();
            store.put(new Element(Integer.valueOf(i++), new byte[100]));
            MILLISECONDS.sleep(500);
            int afterSize = store.getOnDiskSize();
            LOG.info(beforeSize + " ==> " + afterSize);
            if (afterSize <= beforeSize) {
                LOG.info("Hit Threshold : Terminating");
                break;
            }
        }

        LOG.info("Wait For Spool Thread To Finish");
        SECONDS.sleep(2);

        final int initialSize = store.getOnDiskSize();
        final int shrinkSize = initialSize / 2;
        store.diskCapacityChanged(initialSize, shrinkSize);
        LOG.info("Resized : " + initialSize + " ==> " + shrinkSize);

        LOG.info("Wait For Spool Thread To Finish");
        SECONDS.sleep(2);

        for (;; i++) {
            int beforeSize = store.getOnDiskSize();
            store.put(new Element(Integer.valueOf(i), new byte[100]));
            MILLISECONDS.sleep(500);
            int afterSize = store.getOnDiskSize();
            LOG.info(beforeSize + " ==> " + afterSize);
            if (afterSize >= beforeSize && afterSize <= shrinkSize * 1.1) {
                LOG.info("Hit Threshold : Terminating");
                break;
            }
        }

        LOG.info("Wait For Spool Thread To Finish");
        SECONDS.sleep(2);

        {
            int size = store.getOnDiskSize();
            assertTrue(size < (shrinkSize * 1.1));
            assertTrue(size > (shrinkSize * 0.9));
        }

        final int growSize = initialSize * 2;
        store.diskCapacityChanged(shrinkSize, growSize);
        LOG.info("Resized : " + shrinkSize + " ==> " + growSize);

        LOG.info("Wait For Spool Thread To Finish");
        SECONDS.sleep(2);

        for (;; i++) {
            int beforeSize = store.getOnDiskSize();
            store.put(new Element(Integer.valueOf(i), new byte[100]));
            MILLISECONDS.sleep(500);
            int afterSize = store.getOnDiskSize();
            LOG.info(beforeSize + " ==> " + afterSize);
            if (afterSize <= beforeSize && afterSize > 0.9 * growSize) {
                LOG.info("Hit Threshold : Terminating");
                break;
            }
        }

        LOG.info("Wait For Spool Thread To Finish");
        SECONDS.sleep(2);

        {
            int size = store.getOnDiskSize();
            assertTrue(size < (growSize * 1.1));
            assertTrue(size > (growSize * 0.9));
        }
    }
}
