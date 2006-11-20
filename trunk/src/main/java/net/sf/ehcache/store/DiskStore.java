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

package net.sf.ehcache.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.store.policies.LfuMap;
import net.sf.ehcache.store.policies.PolicyMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A disk store implementation.
 * <p/>
 * As of ehcache-1.2 (v1.41 of this file) DiskStore has been changed to a mix of finer grained locking using synchronized collections
 * and synchronizing on the whole instance, as was the case with earlier versions.
 * <p/>
 * The DiskStore, as of ehcache-1.2.4, supports policy based eviction. When elements are retrieved from the DiskStore
 * they are removed from there to the MemoryStore. With this limitation, the FIFO policy works fully. LRU does not really
 * work because a get will remove the element from the map. The LFU policy stores uses the Element hit count, and works
 * fully. Accordingly LFU is the preferred and default policy for DiskStores with maximum sizes set. 
 *
 * @author Adam Murdoch
 * @author Greg Luck
 * @author patches contributed: Ben Houston
 * @version $Id$
 */
public class DiskStore implements Store {

    /**
     * If the CacheManager needs to resolve a conflict with the disk path, it will create a
     * subdirectory in the given disk path with this prefix followed by a number. The presence of this
     * name is used to determined whether it makes sense for a persistent DiskStore to be loaded. Loading
     * persistent DiskStores will only have useful semantics where the diskStore path has not changed.
     */
    public static final String AUTO_DISK_PATH_DIRECTORY_PREFIX = "ehcache_auto_created";

    private static final Log LOG = LogFactory.getLog(DiskStore.class.getName());
    private static final int MS_PER_SECOND = 1000;
    private static final int SPOOL_THREAD_INTERVAL = 200;
    private static final int ESTIMATED_MINIMUM_PAYLOAD_SIZE = 512;
    private long expiryThreadInterval;

    private final String name;
    private boolean active;
    private RandomAccessFile randomAccessFile;

    /**
     * This cannot use synchronizedMap because it needs to be  subtype of Map. Make sure all
     * access is synchronized.
     */
    private PolicyMap diskElements;

    private List freeSpace = Collections.synchronizedList(new ArrayList());
    private Map spool = Collections.synchronizedMap(new HashMap());
    private Object spoolLock = new Object();

    private Ehcache cache;
    private final ThreadPoolManager threadPoolManager;

    /**
     * If persistent, the disk file will be kept
     * and reused on next startup. In addition the
     * memory store will flush all contents to spool,
     * and spool will flush all to disk.
     */
    private final boolean persistent;

    private final String diskPath;

    private File dataFile;

    /**
     * Used to persist elements
     */
    private File indexFile;

    private Status status;

    /**
     * The size in bytes of the disk elements
     */
    private long totalSize;

    /**
     * The maximum elements to allow in the disk file.
     */
    private long maxElementsOnDisk;

    /**
     * Creates a disk store.
     *
     * @param cache    the {@link net.sf.ehcache.Cache} that the store is part of
     * @param diskPath the directory in which to create data and index files
     */
    public DiskStore(Ehcache cache, String diskPath) {
        this.status = Status.STATUS_UNINITIALISED;
        this.cache = cache;
        this.name = cache.getName();
        this.diskPath = diskPath;
        this.expiryThreadInterval = cache.getDiskExpiryThreadIntervalSeconds();
        this.persistent = cache.isDiskPersistent();

        configureDiskElementStoreAndEvictionPolicy();

        threadPoolManager = cache.getCacheManager().getThreadPoolManager();

        try {
            initialiseFiles();

            active = true;

            threadPoolManager.scheduleTask(new SpoolTimer(), SPOOL_THREAD_INTERVAL);

            // Start up the expiry thread if not eternal
            if (!cache.isEternal()) {
                threadPoolManager.scheduleTask(new ExpiryTimer(), expiryThreadInterval * MS_PER_SECOND);
            }

            status = Status.STATUS_ALIVE;
        } catch (final Exception e) {
            // Cleanup on error
            dispose();
            LOG.error(name + "Cache: Could not create disk store. Initial cause was " + e.getMessage(), e);
        }
    }

    private void initialiseFiles() throws Exception {
        // Make sure the cache directory exists
        final File diskDir = new File(diskPath);
        if (diskDir.exists() && !diskDir.isDirectory()) {
            throw new Exception("Store directory \"" + diskDir.getCanonicalPath() + "\" exists and is not a directory.");
        }
        if (!diskDir.exists() && !diskDir.mkdirs()) {
            throw new Exception("Could not create cache directory \"" + diskDir.getCanonicalPath() + "\".");
        }

        dataFile = new File(diskDir, getDataFileName());
        indexFile = new File(diskDir, getIndexFileName());

        deleteIndexIfNoData();

        if (persistent) {
            //if diskpath contains auto generated string
            if (diskPath.indexOf(AUTO_DISK_PATH_DIRECTORY_PREFIX) != -1) {
                LOG.warn("Data in persistent disk stores is ignored for stores from automatically created directories"
                        + " (they start with " + AUTO_DISK_PATH_DIRECTORY_PREFIX + ").\n"
                        + "Remove diskPersistent or resolve the conflicting disk paths in cache configuration.\n"
                        + "Deleting data file " + getDataFileName());
                dataFile.delete();
            } else if (!readIndex()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Index file dirty or empty. Deleting data file " + getDataFileName());
                }
                dataFile.delete();
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Deleting data file " + getDataFileName());
            }
            dataFile.delete();
            indexFile = null;
        }

        // Open the data file as random access. The dataFile is created if necessary.
        randomAccessFile = new RandomAccessFile(dataFile, "rw");
    }

    private void deleteIndexIfNoData() {
        boolean dataFileExists = dataFile.exists();
        boolean indexFileExists = indexFile.exists();
        if (!dataFileExists && indexFileExists) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Matching data file missing for index file. Deleting index file " + getIndexFileName());
            }
            indexFile.delete();
        }
    }

    /**
     * Asserts that the store is active.
     */
    private void checkActive() throws CacheException {
        if (!active) {
            throw new CacheException(name + " Cache: The Disk store is not active.");
        }
    }

    /**
     * Gets an {@link Element} from the Disk Store.
     *
     * @return The element
     */
    public final synchronized Element get(final Object key) {
        try {
            checkActive();

            // Check in the spool.  Remove if present
            Element element;
            synchronized (spoolLock) {
                element = (Element) spool.remove(key);
            }
            if (element != null) {
                element.updateAccessStatistics();
                return element;
            }

            // Check if the element is on disk
            final DiskElement diskElement = (DiskElement) diskElements.get(key);
            if (diskElement == null) {
                // Not on disk
                return null;
            }

            element = loadElementFromDiskElement(diskElement);
            element.updateAccessStatistics();
            return element;
        } catch (Exception exception) {
            LOG.error(name + "Cache: Could not read disk store element for key " + key + ". Error was " + exception.getMessage(),
                exception);
        }
        return null;
    }

    /**
     * An unsynchronized and very low cost check to see if a key is in the Store. No check is made to see if the Element is expired.
     *
     * @param key The Element key
     * @return true if found. If this method return false, it means that an Element with the given key is definitely not in the MemoryStore.
     *         If it returns true, there is an Element there. An attempt to get it may return null if the Element has expired.
     */
    public final boolean containsKey(Object key) {
        boolean diskElementsContainsKey = false;
        synchronized (diskElements) {
            diskElementsContainsKey = diskElements.containsKey(key);
        }
        return diskElementsContainsKey || spool.containsKey(key);
    }

    private Element loadElementFromDiskElement(DiskElement diskElement) throws IOException, ClassNotFoundException {
        Element element;
        // Load the element
        randomAccessFile.seek(diskElement.position);
        final byte[] buffer = new byte[diskElement.payloadSize];
        randomAccessFile.readFully(buffer);
        final ByteArrayInputStream instr = new ByteArrayInputStream(buffer);

        final ObjectInputStream objstr = new ObjectInputStream(instr) {
            /**
             * Overridden because of:
             * Bug 1324221 ehcache DiskStore has issues when used in Tomcat
             */
            protected Class resolveClass(ObjectStreamClass clazz) throws ClassNotFoundException, IOException {
                try {
                    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                    return Class.forName(clazz.getName(), false, classLoader);
                } catch (ClassNotFoundException e) {
                    // Use the default as a fallback because of
                    // bug 1517565 - DiskStore loadElementFromDiskElement
                    return super.resolveClass(clazz);
                }
            }
        };
        element = (Element) objstr.readObject();
        objstr.close();
        return element;
    }

    /**
     * Gets an {@link Element} from the Disk Store, without updating statistics
     *
     * @return The element
     */
    public final synchronized Element getQuiet(final Object key) {
        try {
            checkActive();

            // Check in the spool.  Remove if present
            Element element;
            synchronized (spoolLock) {
                element = (Element) spool.remove(key);
            }
            if (element != null) {
                //element.updateAccessStatistics(); Don't update statistics
                return element;
            }

            // Check if the element is on disk
            final DiskElement diskElement = (DiskElement) diskElements.get(key);
            if (diskElement == null) {
                // Not on disk
                return null;
            }

            element = loadElementFromDiskElement(diskElement);
            //element.updateAccessStatistics(); Don't update statistics
            return element;
        } catch (Exception e) {
            LOG.error(name + "Cache: Could not read disk store element for key " + key + ". Initial cause was " + e.getMessage(),
                e);
        }
        return null;
    }

    /**
     * Gets an Array of the keys for all elements in the disk store.
     *
     * @return An Object[] of {@link Serializable} keys
     * @noinspection SynchronizeOnNonFinalField
     */
    public final synchronized Object[] getKeyArray() {
        Set elementKeySet;
        synchronized (diskElements) {
            elementKeySet = diskElements.keySet();
        }
        Set spoolKeySet;
        synchronized (spoolLock) {
            spoolKeySet = spool.keySet();
        }
        Set allKeysSet = new HashSet(elementKeySet.size() + spoolKeySet.size());
        allKeysSet.addAll(elementKeySet);
        allKeysSet.addAll(spoolKeySet);
        return allKeysSet.toArray();
    }

    /**
     * Returns the current store size.
     *
     * @noinspection SynchronizeOnNonFinalField
     */
    public final synchronized int getSize() {
        try {
            checkActive();
            int spoolSize;
            synchronized (spoolLock) {
                spoolSize = spool.size();
            }
            int diskSize;
            synchronized (diskElements) {
                diskSize = diskElements.size();
            }
            return spoolSize + diskSize;
        } catch (Exception e) {
            LOG.error(name + "Cache: Could not determine size of disk store.. Initial cause was " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Returns the store status.
     */
    public final Status getStatus() {
        return status;
    }

    /**
     * Puts an element into the disk store.
     * <p/>
     * This method is not synchronized. It is however threadsafe. It uses fine-grained
     * synchronization on the spool
     */
    public final void put(final Element element) {
        try {
            checkActive();

            // Spool the element
            synchronized (spoolLock) {
                spool.put(element.getObjectKey(), element);
            }
        } catch (Exception e) {
            LOG.error(name + "Cache: Could not write disk store element for " + element.getObjectKey() + ". Initial cause was "
                    + e.getMessage(), e);
        }
    }

    /**
     * Removes an item from the disk store.
     *
     * @noinspection SynchronizeOnNonFinalField
     */
    public final synchronized Element remove(final Object key) {
        Element element;
        try {
            checkActive();

            // Remove the entry from the spool
            synchronized (spoolLock) {
                element = (Element) spool.remove(key);
            }

            // Remove the entry from the file. Could be in both places.
            synchronized (diskElements) {
                final DiskElement diskElement = (DiskElement) diskElements.remove(key);
                if (diskElement != null) {
                    element = loadElementFromDiskElement(diskElement);
                    freeBlock(diskElement);
                }
            }
        } catch (Exception exception) {
            String message = name + "Cache: Could not remove disk store entry for " + key + ". Error was "
                    + exception.getMessage();
            LOG.error(message, exception);
            throw new CacheException(message);
        }
        return element;
    }

    /**
     * Marks a block as free.
     */
    private void freeBlock(final DiskElement element) {
        totalSize -= element.payloadSize;
        element.payloadSize = 0;
        element.hitcount = 0;
        freeSpace.add(element);
    }

    /**
     * Remove all of the elements from the store.
     * <p/>
     * If there are registered <code>CacheEventListener</code>s they are notified of the expiry or removal
     * of the <code>Element</code> as each is removed.
     */
    public final synchronized void removeAll() {
        try {
            checkActive();

            // Ditch all the elements, and truncate the file
            spool = Collections.synchronizedMap(new HashMap());
            diskElements = createDiskElementMap();
            freeSpace = Collections.synchronizedList(new ArrayList());
            totalSize = 0;
            randomAccessFile.setLength(0);
            if (persistent) {
                indexFile.delete();
                indexFile.createNewFile();
            }
        } catch (Exception e) {
            // Clean up
            LOG.error(name + " Cache: Could not rebuild disk store. Initial cause was " + e.getMessage(), e);
            dispose();
        }
    }

    /**
     * Shuts down the disk store in preparation for cache shutdown
     * <p/>
     * If a VM crash happens, the shutdown hook will not run. The data file and the index file
     * will be out of synchronisation. At initialisation we always delete the index file
     * after we have read the elements, so that it has a zero length. On a dirty restart, it still will have
     * and the data file will automatically be deleted, thus preserving safety.
     */
    public final synchronized void dispose() {

        if (!active) {
            return;
        }

        // Close the cache
        try {
            flush();

            //Clear in-memory data structures
            spool.clear();
            synchronized (diskElements) {
                diskElements.clear();
            }
            freeSpace.clear();
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
            if (!persistent) {
                LOG.debug("Deleting file " + dataFile.getName());
                dataFile.delete();
            }
        } catch (Exception e) {
            LOG.error(name + "Cache: Could not shut down disk cache. Initial cause was " + e.getMessage(), e);
        } finally {
            active = false;
            randomAccessFile = null;
            notifyAll();

            //release reference to cache
            cache = null;
        }
    }

    /**
     * Flush the spool if persistent, so we don't lose any data.
     *
     * @throws IOException
     */
    public final void flush() throws IOException {
        if (persistent) {
            flushSpool();
            writeIndex();
        }
    }

    /**
     * Whether there are any elements waiting to be spooled to disk.
     *
     * @return false if there are elements waiting, otherwise true
     */
    public final synchronized boolean isSpoolEmpty() {
        return (!active || spool.size() == 0);
    }

    /**
     * Flushes all spooled elements to disk.
     * Note that the cache is locked for the entire time that the spool is being flushed.
     *
     * @noinspection SynchronizeOnNonFinalField
     */
    private synchronized void flushSpool() throws IOException {
        if (spool.size() == 0) {
            return;
        }

        Map copyOfSpool = swapSpoolReference();
        
        //does not guarantee insertion order
        Iterator valuesIterator = copyOfSpool.values().iterator();
        while (valuesIterator.hasNext()) {
            writeOrReplaceEntry(valuesIterator.next());
            valuesIterator.remove();
        }
    }

    private Map swapSpoolReference() {
        Map copyOfSpool = null;
        synchronized (spoolLock) {
            // Copy the reference of the old spool, not the contents. Avoid potential spike in memory usage
            copyOfSpool = spool;

            // use a new map making the reference swap above SAFE
            spool = Collections.synchronizedMap(new HashMap());
        }
        return copyOfSpool;
    }

    private void writeOrReplaceEntry(Object object) throws IOException {
        Element element = (Element) object;
        if (element == null) {
            return;
        }
        final Serializable key = (Serializable) element.getObjectKey();
        removeOldEntryIfAny(key);
        findAndEvictDiskElement(element);
        writeElement(element, key);
    }

    private void writeElement(Element element, Serializable key) throws IOException {
        try {
            int bufferLength;
            long expirationTime = element.getExpirationTime();

            MemoryEfficientByteArrayOutputStream buffer = null;
            try {
                buffer = serializeEntry(element);
            } catch (OutOfMemoryError e) {
                LOG.error("OutOfMemoryError on serialize: " + key);

            }
            bufferLength = buffer.size();
            DiskElement diskElement = checkForFreeBlock(bufferLength);

            // Write the record
            randomAccessFile.seek(diskElement.position);
            randomAccessFile.write(buffer.toByteArray(), 0, bufferLength);
            buffer = null;

            // Add to index, update stats
            diskElement.payloadSize = bufferLength;
            diskElement.expiryTime = expirationTime;
            totalSize += bufferLength;
            synchronized (diskElements) {
                // Copy the hit count to support Lfu eviction policy - kludge? probably
                diskElement.hitcount = element.getHitCount();
                diskElements.put(key, diskElement);
            }

        } catch (Exception e) {
            // Catch any exception that occurs during serialization
            LOG.error(name + "Cache: Failed to write element to disk '" + key + "'. Initial cause was " + e.getMessage(), e);
        }

    }

    /**
     * This class is designed to minimse the number of System.arraycopy(); methods
     * required to complete.
     */
    class MemoryEfficientByteArrayOutputStream extends ByteArrayOutputStream {

        /**
         * Creates a new byte array output stream, with a buffer capacity of
         * the specified size, in bytes.
         *
         * @param size the initial size.
         */
        public MemoryEfficientByteArrayOutputStream(int size) {
            super(size);
        }

        /**
         * Gets the bytes. Not all may be valid. Use only up to getSize()
         *
         * @return the underlying byte[]
         */
        public synchronized byte getBytes()[] {
            return buf;
        }
    }

    private MemoryEfficientByteArrayOutputStream serializeEntry(Element element) throws IOException {
        MemoryEfficientByteArrayOutputStream outstr = new MemoryEfficientByteArrayOutputStream(estimatedPayloadSize());
        ObjectOutputStream objstr = new ObjectOutputStream(outstr);
        objstr.writeObject(element);
        objstr.close();
        return outstr;
    }

    private int estimatedPayloadSize() {
        int size = 0;
        try {
            size = (int) (totalSize / diskElements.size());
        } catch (Exception e) {
            //
        }
        if (size <= 0) {
            size = ESTIMATED_MINIMUM_PAYLOAD_SIZE;
        }
        return size;
    }

    /**
     * Remove the old entry, if any
     *
     * @param key
     */
    private void removeOldEntryIfAny(Serializable key) {

        final DiskElement oldBlock;
        synchronized (diskElements) {
            oldBlock = (DiskElement) diskElements.remove(key);
        }
        if (oldBlock != null) {
            freeBlock(oldBlock);
        }
    }

    private DiskElement checkForFreeBlock(int bufferLength) throws IOException {
        DiskElement diskElement = findFreeBlock(bufferLength);
        if (diskElement == null) {
            diskElement = new DiskElement();
            diskElement.position = randomAccessFile.length();
            diskElement.blockSize = bufferLength;
        }
        return diskElement;
    }

    /**
     * Writes the Index to disk on shutdown
     * <p/>
     * The index consists of the elements Map and the freeSpace List
     * <p/>
     * Note that the cache is locked for the entire time that the index is being written
     */
    private synchronized void writeIndex() throws IOException {

        ObjectOutputStream objectOutputStream = null;
        try {
            FileOutputStream fout = new FileOutputStream(indexFile);
            objectOutputStream = new ObjectOutputStream(fout);
            objectOutputStream.writeObject(diskElements);
            objectOutputStream.writeObject(freeSpace);
        } finally {
            if (objectOutputStream != null) {
                objectOutputStream.close();
            }
        }
    }

    /**
     * Reads Index to disk on startup.
     * <p/>
     * if the index file does not exist, it creates a new one.
     * <p/>
     * Note that the cache is locked for the entire time that the index is being written
     *
     * @return True if the index was read successfully, false otherwise
     */
    private synchronized boolean readIndex() throws IOException {
        ObjectInputStream objectInputStream = null;
        FileInputStream fin = null;
        boolean success = false;
        if (indexFile.exists()) {
            try {
                fin = new FileInputStream(indexFile);
                objectInputStream = new ObjectInputStream(fin);
                diskElements = (PolicyMap) objectInputStream.readObject();
                freeSpace = (List) objectInputStream.readObject();
                success = true;
            } catch (StreamCorruptedException e) {
                LOG.error("Corrupt index file. Creating new index.");
            } catch (IOException e) {
                //normal when creating the cache for the first time
                if (LOG.isDebugEnabled()) {
                    LOG.debug("IOException reading index. Creating new index. ");
                }
            } catch (ClassNotFoundException e) {
                LOG.error("Class loading problem reading index. Creating new index. Initial cause was " + e.getMessage(), e);
            } finally {
                try {
                    if (objectInputStream != null) {
                        objectInputStream.close();
                    } else if (fin != null) {
                        fin.close();
                    }
                } catch (IOException e) {
                    LOG.error("Problem closing the index file.");
                }

                //Always zero out file. That way if there is a dirty shutdown, the file will still be empty
                //the next time we start up and readIndex will automatically fail.
                //If there was a problem reading the index this time we also want to zero it out.
                createNewIndexFile();
            }
        } else {
            createNewIndexFile();
        }

        //Return the success flag
        return success;
    }

    private void createNewIndexFile() throws IOException {
        if (indexFile.exists()) {
            indexFile.delete();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Index file " + indexFile + " deleted.");
            }
        }
        if (indexFile.createNewFile()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Index file " + indexFile + " created successfully");
            }
        } else {
            throw new IOException("Index file " + indexFile + " could not created.");
        }
    }

    /**
     * TimerTask to trigger the scheduling of the expiry task
     *
     * @author Jody Brownell
     * @version $Id$
     * @since 1.2.4
     */
    private class ExpiryTimer extends TimerTask {
        public void run() {
            if (active) {
                try {
                    threadPoolManager.executeExpiry(new ExpiryTask());
                } catch (InterruptedException e) {
                    LOG.warn("Failed submit expiry task for processing", e);
                } catch (IllegalStateException e) {
                    LOG.warn(e.getMessage(), e);
                }
            } else {
                this.cancel();
            }
        }
    }

    /**
     * TimerTask to trigger the scheduling of the spooling task
     *                                                                    
     * @author Jody Brownell
     * @version $Id$
     * @since 1.2.4
     */
    private class SpoolTimer extends TimerTask {
        public void run() {
            if (active) {
                try {
                    threadPoolManager.executeSpool(new SpoolTask());
                } catch (InterruptedException e) {
                    LOG.warn("Failed to submit expiry task for processing", e);
                } catch (IllegalStateException e) {
                    LOG.warn(e.getMessage(), e);
                }
            } else {
                this.cancel();
            }
        }
    }

    /**
     * TimerTask to trigger the removal of expired elements from disk
     *
     * @author Jody Brownell
     * @version $Id$
     * @since 1.2.4
     */
    private abstract class BaseTask implements Runnable {

        public abstract void run();

        // enforce uniqueness
        public int hashCode() {
            return name.hashCode();
        }

        public boolean equals(Object obj) {
            return name.equals(obj);
        }
    }

    /**
     * Task executed in the thread pool to remove expired elements.
     *
     * @author Jody Brownell
     * @version $Id$
     * @since 1.2.4
     */
    private class ExpiryTask extends BaseTask {
        public void run() {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Expiring elements...");
                }
                if (active) {
                    expireElements();
                    threadPoolManager.scheduleTask(new ExpiryTimer(), expiryThreadInterval * DiskStore.MS_PER_SECOND);
                }
            } catch (Throwable t) {
                LOG.warn(name + "Cache: Expiry thread throwable caught. Message was: " + t.getMessage() + ". Continuing...", t);
            }
        }
    }

    /**
     * Task executed in the thread pool to flush the spool.
     *
     * @author Jody Brownell
     * @version $Id$
     * @since 1.2.4
     */
    private class SpoolTask extends BaseTask {
        public void run() {
            if (active) {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Flushing spool with element count: " + spool.size());
                    }

                    flushSpool();

                    // schedule the next flush
                    //if (threadPoolManager.isAlive()) {
                        threadPoolManager.scheduleTask(new SpoolTimer(), DiskStore.SPOOL_THREAD_INTERVAL);
                    //}

                } catch (Throwable e) {
                    LOG.error(name + "Cache: Could not flush elements to disk due to " + e.getMessage() + ". Continuing...", e);
                }
            }
        }
    }

    /**
     * Removes expired elements.
     * <p/>
     * Note that the DiskStore cannot efficiently expire based on TTI. It does it on TTL. However any gets out
     * of the DiskStore are check for both before return.
     *
     * @noinspection SynchronizeOnNonFinalField
     */
    public void expireElements() {
        final long now = System.currentTimeMillis();

        // Clean up the spool
        synchronized (spoolLock) {
            for (Iterator iterator = spool.values().iterator(); iterator.hasNext();) {
                final Element element = (Element) iterator.next();
                if (element.isExpired()) {
                    // An expired element
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(name + "Cache: Removing expired spool element " + element.getObjectKey());
                    }
                    iterator.remove();
                    notifyExpiryListeners(element);
                }
            }
        }

        Element element = null;
        RegisteredEventListeners listeners = cache.getCacheEventNotificationService();
        synchronized (diskElements) {
            // Clean up disk elements
            for (Iterator iterator = diskElements.entrySet().iterator(); iterator.hasNext();) {
                final Map.Entry entry = (Map.Entry) iterator.next();
                final DiskElement diskElement = (DiskElement) entry.getValue();

                if (now >= diskElement.expiryTime) {
                    // An expired element
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(name + "Cache: Removing expired spool element " + entry.getKey() + " from Disk Store");
                    }

                    iterator.remove();

                    // only load the element from the file if there is a listener interested in hearing about its expiration 
                    if (listeners.hasCacheEventListeners()) {
                        try {
                            element = loadElementFromDiskElement(diskElement);
                            notifyExpiryListeners(element);
                        } catch (Exception exception) {
                            LOG.error(name + "Cache: Could not remove disk store entry for " + entry.getKey() + ". Error was "
                                    + exception.getMessage(), exception);
                        }
                    }
                    freeBlock(diskElement);
                }
            }
        }
    }

    /**
     * It is enough that an element is expiring here. Notify even though there might be another
     * element with the same key elsewhere in the stores.
     *
     * @param element
     */
    private void notifyExpiryListeners(Element element) {
        cache.getCacheEventNotificationService().notifyElementExpiry(element, false);
    }

    /**
     * It is enough that an element is evicted here. Notify even though there might be another
     * element with the same key elsewhere in the stores.
     *
     * @param element
     */
    private void notifyEvictionListeners(Element element) {
        cache.getCacheEventNotificationService().notifyElementEvicted(element, false);
    }

    /**
     * Allocates a free block.
     */
    private DiskElement findFreeBlock(final int length) {
        for (int i = 0; i < freeSpace.size(); i++) {
            final DiskElement element = (DiskElement) freeSpace.get(i);
            if (element.blockSize >= length) {
                freeSpace.remove(i);
                return element;
            }
        }
        return null;
    }

    /**
     * Returns a {@link String} representation of the {@link DiskStore}
     */
    public final String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[ dataFile = ").append(dataFile.getAbsolutePath())
                .append(", active=").append(active)
                .append(", totalSize=")
                .append(totalSize)
                .append(", status=")
                .append(status)
                .append(", expiryThreadInterval = ")
                .append(expiryThreadInterval).append(" ]");
        return sb.toString();
    }

    /**
     * Generates a unique directory name for use in automatically creating a diskStorePath where there is a conflict.
     *
     * @return a path consisting of {@link #AUTO_DISK_PATH_DIRECTORY_PREFIX} followed by "_" followed by the current
     *         time as a long e.g. ehcache_auto_created_1149389837006
     */
    public static String generateUniqueDirectory() {
        return DiskStore.AUTO_DISK_PATH_DIRECTORY_PREFIX + "_" + System.currentTimeMillis();
    }

    /**
     * A reference to an on-disk elements.
     *
     * @noinspection SerializableHasSerializationMethods
     */
    private static final class DiskElement implements Serializable {

        private static final long serialVersionUID = -717310932566592289L;

        /**
         * the file pointer
         */
        private long position;

        /**
         * The size used for data.
         */
        private int payloadSize;

        /**
         * the size of this element.
         */
        private int blockSize;

        /**
         * The expiry time in milliseconds
         */
        private long expiryTime;
        
        /**
         * The numbe of times the element has been requested and found in the cache.
         */
        private long hitcount;
    }

    /**
     * @return the total size of the data file and the index file, in bytes.
     */
    public final long getTotalFileSize() {
        return getDataFileSize() + getIndexFileSize();
    }

    /**
     * @return the size of the data file in bytes.
     */
    public final long getDataFileSize() {
        return dataFile.length();
    }

    /**
     * The design of the layout on the data file means that there will be small gaps created when DiskElements
     * are reused.
     *
     * @return the sparseness, measured as the percentage of space in the Data File not used for holding data
     */
    public final float calculateDataFileSparseness() {
        return 1 - ((float) getUsedDataSize() / (float) getDataFileSize());
    }

    /**
     * When elements are deleted, spaces are left in the file. These spaces are tracked and are reused
     * when new elements need to be written.
     * <p/>
     * This method indicates the actual size used for data, excluding holes. It can be compared with
     * {@link #getDataFileSize()} as a measure of fragmentation.
     */
    public final long getUsedDataSize() {
        return totalSize;
    }

    /**
     * @return the size of the index file, in bytes.
     */
    public final long getIndexFileSize() {
        if (indexFile == null) {
            return 0;
        } else {
            return indexFile.length();
        }
    }

    /**
     * @return the file name of the data file where the disk store stores data, without any path information.
     */
    public final String getDataFileName() {
        return name + ".data";
    }

    /**
     * @return the disk path, which will be dependent on the operating system
     */
    public final String getDataFilePath() {
        return diskPath;
    }

    /**
     * @return the file name of the index file, which maintains a record of elements and their addresses
     *         on the data file, without any path information.
     */
    public final String getIndexFileName() {
        return name + ".index";
    }

    private void findAndEvictDiskElement(Element elementJustAdded) {
        // ensure the expiry thread waits until we are done with this particular operation
        synchronized (diskElements) {
            if (maxElementsOnDisk > 0 && diskElements.size() >= maxElementsOnDisk) {
                
                // Find a DiskElement which is 
                Map.Entry entry = ((PolicyMap)diskElements).findElementToEvict(elementJustAdded);
                
                // actually do the remove :)
                diskElements.remove(entry.getKey());

                // remove the DiskElement and fire any listeners if required
                evictDiskElement(entry.getKey(), (DiskElement) entry.getValue());
            }
        }
    }

    private void evictDiskElement(Object key, DiskElement diskElement) {
        RegisteredEventListeners listeners = cache.getCacheEventNotificationService();
        // only load the element from the file if there is a listener interested in hearing about its expiration 
        if (listeners.hasCacheEventListeners()) {
            try {
                Element element = loadElementFromDiskElement(diskElement);
                notifyEvictionListeners(element);
            } catch (Exception exception) {
                LOG.error(name + "Cache: Could not remove disk store entry for " + key + ". Error was " + exception.getMessage(),
                    exception);
            }
        }
        freeBlock(diskElement);
    }

    /**
     * Create an instance of a map based on the store configuration. 
     * 
     *  maxElementsOnDisk is configured to a value above 0, a DiskElementMap will be returned otherwise a regular map will be returned.
     */
    private PolicyMap createDiskElementMap() {
        if (this.maxElementsOnDisk > 0) {
            PolicyMap policyMap;
            // The LFU map is a special case where we want to avoid reading elements from Disk at all costs.
            // Therefore we must use DiskElements instead of elements therefore require a slightly different approach
            if (cache.getEvictionPolicy().equals(EvictionPolicy.LFU)) {
                policyMap = new LfuDiskMap();
            } else {
                policyMap = cache.getEvictionPolicy().createPolicyMap();
            }
            // Create a map which implements the policy
            return policyMap;
        } else {
            // if there is no max, there is no need for a special map. Use LfuDiskMap which is backed by a HashMap.
            return new LfuDiskMap();
        }
    }

    private void configureDiskElementStoreAndEvictionPolicy() {
        maxElementsOnDisk = cache.getMaxElementsOnDisk();
        diskElements = createDiskElementMap();
    }
    
    /**
     * todo test
     * A Lfu Policy for disk elements.
     * 
     * @since 1.2.4
     * @author Jody Brownell
     * @version $Id$
     */
    private static class LfuDiskMap extends LfuMap {
        
        protected boolean isNewEntryLower(Map.Entry existingLowest, Map.Entry newEntry) {
            DiskElement lowest = (DiskElement) existingLowest.getValue();
            DiskElement newElement = (DiskElement) newEntry.getValue();
            
            return lowest.hitcount > newElement.hitcount;
        }
    }
}
