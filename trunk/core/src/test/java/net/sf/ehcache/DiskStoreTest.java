/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.store.DiskStore;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.Primitive;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

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
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final Logger LOG = Logger.getLogger(DiskStoreTest.class.getName());
    private static final int ELEMENT_ON_DISK_SIZE = 1340;
    private CacheManager manager2;

    /**
     * teardown
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (manager2 != null) {
            manager2.shutdown();
        }
        deleteFile("persistentLongExpiryIntervalCache");
        deleteFile("fileTest");
        deleteFile("testPersistent");
    }

    /**
     * Creates a store which is non-expiring so that we can check for
     * size-related characteristics without elements being deleted under us.
     */
    private DiskStore createNonExpiringDiskStore() {
        Cache cache = new Cache("test/NonPersistent", 10000, true, true, 2, 1, false, 1);
        manager.addCache(cache);
        DiskStore diskStore = (DiskStore) cache.getDiskStore();
        return diskStore;
    }

    private DiskStore createDiskStore() {
        Cache cache = new Cache("test/NonPersistent", 10000, true, false, 2, 1, false, 1);
        manager.addCache(cache);
        DiskStore diskStore = (DiskStore) cache.getDiskStore();
        return diskStore;
    }

    private DiskStore createPersistentDiskStore(String cacheName) {
        Cache cache = new Cache(cacheName, 10000, true, true, 5, 1, true, 600);
        manager.addCache(cache);
        DiskStore diskStore = (DiskStore) cache.getDiskStore();
        return diskStore;
    }

    private DiskStore createAutoPersistentDiskStore(String cacheName) {
        Cache cache = new Cache(cacheName, 10000, true, true, 5, 1, true, 600);
        manager2 = new CacheManager();
        //manager.setDiskStorePath(System.getProperty("java.io.tmpdir") + File.separator + DiskStore.generateUniqueDirectory());
        manager2.addCache(cache);
        DiskStore diskStore = (DiskStore) cache.getDiskStore();
        return diskStore;
    }

    private DiskStore createPersistentDiskStoreFromCacheManager() {
        Cache cache = manager.getCache("persistentLongExpiryIntervalCache");
        return (DiskStore) cache.getDiskStore();
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
        DiskStore diskStore = createNonExpiringDiskStore();
        File dataFile = new File(diskStore.getDataFilePath() + File.separator + diskStore.getDataFileName());

        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[1024];
            diskStore.put(new Element("key" + (i + 100), data));
        }
        waitShorter();
        assertEquals(ELEMENT_ON_DISK_SIZE * 100, diskStore.getDataFileSize());

        assertEquals(100, diskStore.getSize());
        diskStore.dispose();
        Thread.sleep(1);
        assertFalse("File exists", dataFile.exists());
    }

    /**
     * Tests that the Disk Store can be changed
     */
    @Test
    public void testSetDiskStorePath() throws IOException, InterruptedException {
        Cache cache = new Cache("testChangePath", 10000, true, true, 5, 1, true, 600);
        manager2 = new CacheManager();
        cache.setDiskStorePath(System.getProperty("java.io.tmpdir") + File.separator + "changedDiskStorePath");
        manager2.addCache(cache);
        DiskStore diskStore = (DiskStore) cache.getDiskStore();
        File dataFile = new File(diskStore.getDataFilePath() + File.separator + diskStore.getDataFileName());
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
        DiskStore diskStore = createPersistentDiskStoreFromCacheManager();
        diskStore.removeAll();

        File dataFile = new File(diskStore.getDataFilePath() + File.separator + diskStore.getDataFileName());

        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[1024];
            diskStore.put(new Element("key" + (i + 100), data));
        }
        waitShorter();
        assertEquals(100, diskStore.getSize());
        diskStore.dispose();

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
        assertEquals(100, cache.getSize());
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
     * Tests that the spool thread dies on dispose.
     */
    @Test
    public void testSpoolThreadDiesOnDispose() throws IOException, InterruptedException {
        Cache cache = new Cache("testNonPersistent", 10000, true, false, 5, 1, false, 100);
        cache.initialise();
        DiskStore diskStore = (DiskStore) cache.getDiskStore();

        //Put in some data
        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[1024];
            diskStore.put(new Element("key" + (i + 100), data));
        }
        waitShorter();

        diskStore.dispose();
        //Give the spool thread time to be interrupted and die
        Thread.sleep(100);
        assertTrue(!diskStore.isSpoolThreadAlive());


    }

    /**
     * Tests that we can save and load a persistent store in a repeatable way
     */
    @Test
    public void testLoadPersistentStore() throws IOException, InterruptedException {
        //initialise
        String cacheName = "testLoadPersistent";
        DiskStore diskStore = createPersistentDiskStore(cacheName);
        diskStore.removeAll();


        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[1024];
            diskStore.put(new Element("key" + (i + 100), data));
        }
        waitShorter();
        assertEquals(ELEMENT_ON_DISK_SIZE * 100, diskStore.getDataFileSize());
        assertEquals(100, diskStore.getSize());
        manager.removeCache(cacheName);
        Thread.sleep(3000);
        //check that we can create and dispose several times with no problems and no lost data
        for (int i = 0; i < 10; i++) {
            diskStore = createPersistentDiskStore(cacheName);
            File dataFile = new File(diskStore.getDataFilePath() + File.separator + diskStore.getDataFileName());
            assertTrue("File exists", dataFile.exists());
            assertEquals(100 * ELEMENT_ON_DISK_SIZE, dataFile.length());
            assertEquals(100, diskStore.getSize());

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
        DiskStore diskStore = createAutoPersistentDiskStore(cacheName);
        diskStore.removeAll();


        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[1024];
            diskStore.put(new Element("key" + (i + 100), data));
        }
        waitLonger();
        assertEquals(ELEMENT_ON_DISK_SIZE * 100, diskStore.getDataFileSize());
        assertEquals(100, diskStore.getSize());
        manager2.removeCache(cacheName);
        Thread.sleep(1000);

        Cache cache = new Cache(cacheName, 10000, true, true, 5, 1, true, 600);
        manager2.addCache(cache);

        File dataFile = new File(diskStore.getDataFilePath() + File.separator + diskStore.getDataFileName());
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
        DiskStore diskStore = createPersistentDiskStore(cacheName);
        diskStore.removeAll();


        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[1024];
            diskStore.put(new Element("key" + (i + 100), data));
        }
        waitShorter();
        assertEquals(ELEMENT_ON_DISK_SIZE * 100, diskStore.getDataFileSize());
        assertEquals(100, diskStore.getSize());
        manager.removeCache(cacheName);

        diskStore = createPersistentDiskStore(cacheName);
        File dataFile = new File(diskStore.getDataFilePath() + File.separator + diskStore.getDataFileName());
        assertTrue("File exists", dataFile.exists());
        assertEquals(100 * ELEMENT_ON_DISK_SIZE, dataFile.length());
        assertEquals(100, diskStore.getSize());

        diskStore.remove("key100");
        assertEquals(100 * ELEMENT_ON_DISK_SIZE, dataFile.length());
        assertEquals(99, diskStore.getSize());

        manager.removeCache(cacheName);

        assertTrue("File exists", dataFile.exists());
        assertEquals(100 * ELEMENT_ON_DISK_SIZE, dataFile.length());
    }

    /**
     * Tests that we can load a store after the index has been corrupted
     */
    @Test
    public void testLoadPersistentStoreAfterCorruption() throws IOException, InterruptedException {
        //initialise
        String cacheName = "testPersistent";
        DiskStore diskStore = createPersistentDiskStore(cacheName);
        diskStore.removeAll();


        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[1024];
            diskStore.put(new Element("key" + (i + 100), data));
        }
        waitShorter();
        assertEquals(ELEMENT_ON_DISK_SIZE * 100, diskStore.getDataFileSize());
        assertEquals(100, diskStore.getSize());
        manager.removeCache(cacheName);

        File indexFile = new File(diskStore.getDataFilePath() + File.separator + diskStore.getIndexFileName());
        FileOutputStream fout = new FileOutputStream(indexFile);
        //corrupt the index file
        fout.write(new byte[]{'q', 'w', 'e', 'r', 't', 'y'});
        fout.close();
        diskStore = createPersistentDiskStore(cacheName);
        File dataFile = new File(diskStore.getDataFilePath() + File.separator + diskStore.getDataFileName());
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
        DiskStore diskStore = createPersistentDiskStore(cacheName);
        diskStore.removeAll();

        byte[] data = new byte[1024];
        for (int i = 0; i < 100; i++) {
            diskStore.put(new Element("key" + (i + 100), data));
        }
        waitShorter();
        assertEquals(ELEMENT_ON_DISK_SIZE * 100, diskStore.getDataFileSize());
        assertEquals(100, diskStore.getSize());
        manager.removeCache(cacheName);

        diskStore = createPersistentDiskStore(cacheName);
        File dataFile = new File(diskStore.getDataFilePath() + File.separator + diskStore.getDataFileName());
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
        final DiskStore diskStore = createDiskStore();
        Element element = diskStore.get("key");
        assertNull(element);
        diskStore.put(new Element("key", "value"));
        element = diskStore.getQuiet("key");
        assertNotNull(element);
    }

    /**
     * Tests looking up an entry that does not exist.
     */
    @Test
    public void testGetQuietUnknownThenKnown() throws Exception {
        final DiskStore diskStore = createDiskStore();
        Element element = diskStore.getQuiet("key");
        assertNull(element);
        diskStore.put(new Element("key", "value"));
        element = diskStore.getQuiet("key");
        assertNotNull(element);
    }

    /**
     * Tests adding an entry.
     */
    @Test
    public void testPut() throws Exception {
        final DiskStore diskStore = createDiskStore();

        // Make sure the element is not found
        assertEquals(0, diskStore.getSize());
        Element element = diskStore.get("key");
        assertNull(element);

        // Add the element
        final String value = "value";
        element = new Element("key", value);
        diskStore.put(element);

        // Get the element
        assertEquals(1, diskStore.getSize());
        element = diskStore.get("key");
        assertNotNull(element);
        assertEquals(value, element.getObjectValue());
    }


    /**
     *
     */
    @Test
    public void testLFUEvictionFromDiskStore() throws IOException, InterruptedException {
        Cache cache = new Cache("testNonPersistent", 0, MemoryStoreEvictionPolicy.LFU, true,
                null, false, 2000, 1000, false, 1, null, null, 10);
        manager.addCache(cache);
        DiskStore store = (DiskStore) cache.getDiskStore();

        Element element;

        assertEquals(0, store.getSize());

        for (int i = 0; i < 10; i++) {
            element = new Element("key" + i, "value" + i);
            cache.put(element);
        }

        //allow to move through spool
        Thread.sleep(220);
        assertEquals(10, store.getSize());


        for (int i = 1; i < 10; i++) {
            cache.get("key" + i);
            cache.get("key" + i);
            cache.get("key" + i);
            cache.get("key" + i);
        }
        //allow to move through spool
        Thread.sleep(220);
        assertEquals(10, store.getSize());

        element = new Element("keyNew", "valueNew");
        store.put(element);

        //allow to get out of spool
        Thread.sleep(220);
        assertEquals(10, store.getSize());
        //check new element not evicted
        assertNotNull(store.get(element.getObjectKey()));
        //check evicted honours LFU policy
        assertNull(store.get("key0"));

        for (int i = 0; i < 2000; i++) {
            store.put(new Element("" + i, new Date()));
        }
        //wait for spool to empty
        waitLonger();

        assertEquals(10, store.getSize());
    }

    /**
     * Tests the loading of classes
     */
    @Test
    public void testClassloading() throws Exception {
        final DiskStore diskStore = createDiskStore();

        Long value = new Long(123L);
        Element element = new Element("key", value);
        diskStore.put(element);
        Thread.sleep(1000);
        Element elementOut = diskStore.get("key");
        assertEquals(value, elementOut.getObjectValue());


        Primitive primitive = new Primitive();
        primitive.integerPrimitive = 123;
        primitive.longPrimitive = 456L;
        primitive.bytePrimitive = "a".getBytes()[0];
        primitive.charPrimitive = 'B';
        primitive.booleanPrimitive = false;

        //test Serializability
        ByteArrayOutputStream outstr = new ByteArrayOutputStream();
        ObjectOutputStream objstr = new ObjectOutputStream(outstr);
        objstr.writeObject(element);
        objstr.close();


        Element primitiveElement = new Element("primitive", primitive);
        diskStore.put(primitiveElement);
        Thread.sleep(1000);
        elementOut = diskStore.get("primitive");
        assertEquals(primitive, elementOut.getObjectValue());

    }


    /**
     * Tests adding an entry and waiting for it to be written.
     */
    @Test
    public void testPutSlow() throws Exception {
        final DiskStore diskStore = createDiskStore();

        // Make sure the element is not found
        assertEquals(0, diskStore.getSize());
        Element element = diskStore.get("key");
        assertNull(element);

        // Add the element
        final String value = "value";
        element = new Element("key", value);
        diskStore.put(element);

        // Wait
        waitShorter();

        // Get the element
        assertEquals(1, diskStore.getSize());
        element = diskStore.get("key");
        assertNotNull(element);
        assertEquals(value, element.getObjectValue());
    }

    /**
     * Tests removing an entry.
     */
    @Test
    public void testRemove() throws Exception {
        final DiskStore diskStore = createDiskStore();

        // Add the entry
        final String value = "value";
        Element element = new Element("key", value);
        diskStore.put(element);

        // Check the entry is there
        assertEquals(1, diskStore.getSize());
        element = diskStore.get("key");
        assertNotNull(element);

        // Remove it
        diskStore.remove("key");

        // Check the entry is not there
        assertEquals(0, diskStore.getSize());
        element = diskStore.get("key");
        assertNull(element);
    }

    /**
     * Tests removing an entry, after it has been written
     */
    @Test
    public void testRemoveSlow() throws Exception {
        final DiskStore diskStore = createDiskStore();

        // Add the entry
        final String value = "value";
        Element element = new Element("key", value);
        diskStore.put(element);

        // Wait for the entry
        waitShorter();

        // Check the entry is there
        assertEquals(1, diskStore.getSize());
        element = diskStore.get("key");
        assertNotNull(element);

        // Remove it
        diskStore.remove("key");

        // Check the entry is not there
        assertEquals(0, diskStore.getSize());
        element = diskStore.get("key");
        assertNull(element);
    }

    /**
     * Tests removing all the entries.
     */
    @Test
    public void testRemoveAll() throws Exception {
        final DiskStore diskStore = createDiskStore();

        // Add the entry
        final String value = "value";
        Element element = new Element("key", value);
        diskStore.put(element);

        // Check the entry is there
        element = diskStore.get("key");
        assertNotNull(element);

        // Remove it
        diskStore.removeAll();

        // Check the entry is not there
        assertEquals(0, diskStore.getSize());
        element = diskStore.get("key");
        assertNull(element);
    }

    /**
     * Tests removing all the entries, after they have been written to disk.
     */
    @Test
    public void testRemoveAllSlow() throws Exception {
        final DiskStore diskStore = createDiskStore();

        // Add the entry
        final String value = "value";
        Element element = new Element("key", value);
        diskStore.put(element);

        // Wait
        waitShorter();

        // Check the entry is there
        element = diskStore.get("key");
        assertNotNull(element);

        // Remove it
        diskStore.removeAll();

        // Check the entry is not there
        assertEquals(0, diskStore.getSize());
        element = diskStore.get("key");
        assertNull(element);
    }

    /**
     * Tests bulk load.
     */
    @Test
    public void testBulkLoad() throws Exception {
        final DiskStore diskStore = createDiskStore();

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
    public void testExpiry() throws Exception {
        // Create a diskStore with a cranked up expiry thread
        final DiskStore diskStore = createDiskStore();

        // Add an element that will expire.
        Element element = new Element("key", "value");
        diskStore.put(element);
        assertEquals(1, diskStore.getSize());

        assertNotNull(diskStore.get("key"));

        // Wait a couple of seconds
        Thread.sleep(3000);

        assertNull(diskStore.get("key"));

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
     * Using a key of new Integer(i * outer) the size stays constant at 140800.
     *
     * @throws InterruptedException
     */
    @Test
    public void testExpiryWithSize() throws InterruptedException {
        DiskStore diskStore = createDiskStore();
        diskStore.removeAll();

        byte[] data = new byte[1024];
        for (int outer = 1; outer <= 10; outer++) {
            for (int i = 0; i < 100; i++) {
                Element element = new Element(new Integer(i * outer), data);
                element.setTimeToLive(1);
                diskStore.put(element);
            }
            waitLonger();
            int predictedSize = 140800;
            long actualSize = diskStore.getDataFileSize();
            LOG.log(Level.INFO, "Predicted Size: " + predictedSize + " Actual Size: " + actualSize);
            assertEquals(predictedSize, actualSize);
            LOG.log(Level.INFO, "Memory Use: " + measureMemoryUse());
        }


    }

    /**
     * Waits for all spooled elements to be written to disk.
     */
    private static void waitShorter() throws InterruptedException {
        Thread.sleep((long) (300 + 100 * StopWatch.getSpeedAdjustmentFactor()));
    }


    /**
     * Waits for all spooled elements to be written to disk.
     */
    private static void waitLonger() throws InterruptedException {
        Thread.sleep((long) (300 + 500 * StopWatch.getSpeedAdjustmentFactor()));
    }


    /**
     * Multi-thread read-only test. Will fail on memory constrained VMs
     */
    @Test
    public void testReadOnlyMultipleThreads() throws Exception {
        final DiskStore diskStore = createNonExpiringDiskStore();

        // Add a couple of elements
        diskStore.put(new Element("key0", "value"));
        diskStore.put(new Element("key1", "value"));

        // Wait for the elements to be written
        waitShorter();

        // Run a set of threads, that attempt to fetch the elements
        final List executables = new ArrayList();
        for (int i = 0; i < 10; i++) {
            final String key = "key" + (i % 2);
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
        final DiskStore diskStore = createDiskStore();

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
     * Test overflow to disk = true, using 100000 records.
     * 15 seconds v1.38 DiskStore
     * 2 seconds v1.42 DiskStore
     * Adjusted for change to laptop
     */
    @Test
    public void testOverflowToDiskWithLargeNumberofCacheEntries() throws Exception {

        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1000, MemoryStoreEvictionPolicy.LRU, true, null, true, 500, 500, false, 1, null);
        manager.addCache(cache);
        int i = 0;
        StopWatch stopWatch = new StopWatch();
        for (; i < 100000; i++) {
            cache.put(new Element("" + i,
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }
        long time = stopWatch.getElapsedTime();
        LOG.log(Level.INFO, "time: " + time);
        assertTrue(4 < time);
    }


    /**
     * This test is designed to be used with a profiler to explore the ways in which DiskStore
     * uses memory. It does not do much on its own.
     */
    @Test
    public void testOutOfMemoryErrorOnOverflowToDisk() throws Exception {

        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1000, MemoryStoreEvictionPolicy.LRU, true, null, true, 500, 500, false, 1, null);
        manager.addCache(cache);
        int i = 0;

        Random random = new Random();
        for (; i < 5500; i++) {
            byte[] bytes = new byte[10000];
            random.nextBytes(bytes);
            cache.put(new Element("" + i, bytes));
        }
        LOG.log(Level.INFO, "Elements written: " + i);
        //Thread.sleep(100000);
    }

    /**
     * Test overflow to disk = true, using 100000 records.
     * 35 seconds v1.38 DiskStore
     * 26 seconds v1.42 DiskStore
     */
    @Test
    public void testOverflowToDiskWithLargeNumberofCacheEntriesAndGets() throws Exception {

        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1000, MemoryStoreEvictionPolicy.LRU, true, null, true, 500, 500, false, 60, null);
        manager.addCache(cache);
        Random random = new Random();
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 100000; i++) {
            cache.put(new Element("" + i,
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            cache.get("" + random.nextInt(100000));
        }


        long elapsed = stopWatch.getElapsedTime();
        LOG.log(Level.INFO, "Elapsed time: " + elapsed / 1000);
        Thread.sleep(500);
        assertEquals(100000, cache.getSize());
        assertTrue(23 < elapsed);
        //Some entries may be in the Memory Store and Disk Store. cache.getSize removes dupes. a look at the
        //disk store size directly does not.
        assertTrue(99000 <= cache.getDiskStore().getSize());
    }

    /**
     * Runs out of memory at 5,099,999 elements with the standard 64MB VM size on 32 bit architectures.
     * Around 3,099,999 for AMD64. Why? See abstract citation below from
     * http://www3.interscience.wiley.com/cgi-bin/abstract/111082816/ABSTRACT?CRETRY=1&SRETRY=0
     * <pre>
     * By running the PowerPC machine in both 32-bit and 64-bit mode we are able to compare 32-bit and 64-bit VMs.
     * We conclude that the space an object takes in the heap in 64-bit mode is 39.3% larger on average than in
     * 32-bit mode. We identify three reasons for this: (i) the larger pointer size, (ii) the increased header
     * and (iii) the increased alignment. The minimally required heap size is 51.1% larger on average in 64-bit
     * than in 32-bit mode. From our experimental setup using hardware performance monitors, we observe that 64-bit
     * computing typically results in a significantly larger number of data cache misses at all levels of the memory
     * hierarchy. In addition, we observe that when a sufficiently large heap is available, the IBM JDK 1.4.0 VM is
     * 1.7% slower on average in 64-bit mode than in 32-bit mode. Copyright © 2005 John Wiley & Sons, Ltd.
     * </pre>
     * The reason that it is not infinite is because of a small amount of memory used (about 12 bytes) used for
     * the disk store index in this case.
     * <p/>
     * Slow tests
     */
    @Test
    public void testMaximumCacheEntriesIn64MBWithOverflowToDisk() throws Exception {

        Cache cache = new Cache("test", 1000, MemoryStoreEvictionPolicy.LRU, true, null, true, 500, 500, false, 1, null);
        manager.addCache(cache);
        StopWatch stopWatch = new StopWatch();
        int i = 0;
        int j = 0;
        Integer index = null;
        try {
            for (; i < 100; i++) {
                for (j = 0; j < 100000; j++) {
                    index = new Integer(((1000000 * i) + j));
                    cache.put(new Element(index,
                            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
                //wait to write entries
                int size = cache.getSize();
                Thread.sleep(2000);
            }
            long elapsed = stopWatch.getElapsedTime();
            LOG.log(Level.INFO, "Elapsed time: " + elapsed / 1000);
            fail();
        } catch (OutOfMemoryError e) {
            LOG.log(Level.INFO, "All heap consumed after " + index + " entries created.");
            int expectedMax = 3090000;
            assertTrue("Achieved " + index.intValue() + " which was less than the expected value of " + expectedMax,
                    index.intValue() >= expectedMax);
        }
    }

    /**
     * Perf test used by Venkat Subramani
     * Get took 119s with Cache svn21
     * Get took 42s
     * The change was to stop adding DiskStore retrievals into the MemoryStore. This made sense when the only
     * policy was LRU. In the new version an Elment, once evicted from the MemoryStore, stays in the DiskStore
     * until expiry or removal. This avoids a lot of serialization overhead.
     * <p/>
     * Slow tests
     * 235 with get. 91 for 1.2.3. 169 with remove.
     *
     * put 14, get 32
     * put 13, get 19
     *
     */
    //@Test
    public void testLargePutGetPerformanceWithOverflowToDisk() throws Exception {

        Cache cache = new Cache("test", 1000, MemoryStoreEvictionPolicy.LRU, true, null, true, 500, 500, false, 10000, null);
        manager.addCache(cache);
        StopWatch stopWatch = new StopWatch();
        int i = 0;
        int j = 0;
        Integer index;
        for (; i < 5; i++) {
            for (j = 0; j < 100000; j++) {
                index = new Integer(((1000000 * i) + j));
                cache.put(new Element(index,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }
        }
        long elapsed = stopWatch.getElapsedTime();
        long putTime = ((elapsed / 1000) - 10);
        LOG.log(Level.INFO, "Put Elapsed time: " + putTime);
        assertTrue(putTime < 20);

        //wait for Disk Store to finish spooling
        while (cache.getDiskStore().bufferFull()) {
            Thread.sleep(2000);
        }
        Random random = new Random();
        StopWatch getStopWatch = new StopWatch();
        long getStart = stopWatch.getElapsedTime();

        for (int k = 0; k < 1000000; k++) {
            Integer key = new Integer(random.nextInt(500000));
            cache.get(key);
        }

        long getElapsedTime = getStopWatch.getElapsedTime();
        int time = (int) ((getElapsedTime - getStart) / 1000);
        LOG.log(Level.INFO, "Get Elapsed time: " + time);

        assertTrue(time < 180);


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
}
