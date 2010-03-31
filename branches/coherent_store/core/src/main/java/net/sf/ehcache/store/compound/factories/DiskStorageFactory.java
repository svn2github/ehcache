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

package net.sf.ehcache.store.compound.factories;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.RandomAccessFile;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.store.compound.CompoundStore;
import net.sf.ehcache.store.compound.ElementSubstitute;
import net.sf.ehcache.store.compound.ElementSubstituteFactory;
import net.sf.ehcache.util.MemoryEfficientByteArrayOutputStream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A mock-up of a on-disk element proxy factory.
 * 
 * @param <T> type of the encoded element substitutes
 * @author Chris Dennis
 */
abstract class DiskStorageFactory<T extends ElementSubstitute> implements ElementSubstituteFactory<T> {

    protected static final String AUTO_DISK_PATH_DIRECTORY_PREFIX = "ehcache_auto_created";
    private static final int SERIALIZATION_CONCURRENCY_DELAY = 250;
    private static final int SHUTDOWN_GRACE_PERIOD = 60;
    
    private static final Logger LOG = LoggerFactory.getLogger(DiskStorageFactory.class.getName());

    private final BlockingQueue<Runnable> diskQueue; 
    /**
     * Executor service used to write elements to disk
     */
    private final ScheduledThreadPoolExecutor diskWriter = new ScheduledThreadPoolExecutor(1);
    
    private final File             file;
    private final RandomAccessFile data;

    private final Collection<DiskMarker> freeChunks = new ConcurrentLinkedQueue<DiskMarker>();

    private final RegisteredEventListeners eventService;
    
    protected volatile CompoundStore                  store;
    
    /**
     * Constructs a disk storage factory using the given data file.
     * 
     * @param dataFile
     */
    DiskStorageFactory(File dataFile, long expiryInterval, RegisteredEventListeners eventService) {
        this.file = dataFile;
        try {
            data = new RandomAccessFile(file, "rw");
        } catch (FileNotFoundException e) {
            throw new CacheException(e);
        }
        this.diskQueue = diskWriter.getQueue();
        this.eventService = eventService;
        
        diskWriter.scheduleWithFixedDelay(new DiskExpiryTask(), expiryInterval, expiryInterval, TimeUnit.SECONDS);
    }
    
    /**
     * Return this size in bytes of this factory
     */
    public long getSizeInBytes() {
        synchronized (data) {
            try {
                return data.length();
            } catch (IOException e) {
                LOG.warn("Exception trying to determine store size", e);
                return 0;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void bind(CompoundStore store) {
        this.store = store;
    }

    /**
     * Shuts down this disk factory.
     * <p>
     * This shuts down the executor and then waits for its termination, before closing the data file.
     */
    protected void shutdown() throws IOException {
        diskWriter.shutdown();
        for (int i = 0; i < SHUTDOWN_GRACE_PERIOD; i++) {
            try {
                if (diskWriter.awaitTermination(1, TimeUnit.SECONDS)) {
                    break;
                } else {
                    LOG.info("Waited " + (i + 1) + " seconds for shutdown");
                }
            } catch (InterruptedException e) {
                LOG.warn("Received exception while waiting for shutdown", e);
            }
        }
        data.close();
    }
    
    /**
     * Deletes the data file for this factory. 
     */
    protected void delete() {
        file.delete();
        freeChunks.clear();
        if (file.getAbsolutePath().contains(AUTO_DISK_PATH_DIRECTORY_PREFIX)) {
            //try to delete the auto_createtimestamp directory. Will work when the last Disk Store deletes
            //the last files and the directory becomes empty.
            File dataDirectory = file.getParentFile();
            if (dataDirectory != null && dataDirectory.exists()) {
                if (dataDirectory.delete()) {
                    LOG.debug("Deleted directory " + dataDirectory.getName());
                }
            }

        }
    }

    /**
     * Schedule to given task on the disk writer executor service.
     * 
     * @param <U> return type of the callable
     * @param call callable to call
     * @return Future representing the return of this call
     */
    protected <U> Future<U> schedule(Callable<U> call) {
        return diskWriter.submit(call);
    }

    /**
     * Read the data at the given marker, and return the associated deserialized Element.
     * 
     * @param marker marker to read
     * @return deserialized Element
     * @throws IOException on read error
     * @throws ClassNotFoundException on deserialization error
     */
    protected Element read(DiskMarker marker) throws IOException, ClassNotFoundException {
        final byte[] buffer = new byte[marker.getSize()];
        synchronized (data) {
            // Load the element
            data.seek(marker.getPosition());
            data.readFully(buffer);
        }
        
        ObjectInputStream objstr = new ObjectInputStream(new ByteArrayInputStream(buffer)) {
            /**
             * Overridden because of:
             * Bug 1324221 ehcache DiskStore has issues when used in Tomcat
             */
            @Override
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
        
        try {
            return (Element) objstr.readObject();
        } finally {
            objstr.close();
        }
    }

    /**
     * Write the given element to disk, and return the associated marker.
     * 
     * @param element to write
     * @return marker representing the element
     * @throws IOException on write error
     */
    protected DiskMarker write(Element element) throws IOException {
        MemoryEfficientByteArrayOutputStream buffer = serializeElement(element);
        int bufferLength = buffer.size();
        DiskMarker marker = alloc(element, bufferLength);
        // Write the record
        synchronized (data) {
            data.seek(marker.getPosition());
            data.write(buffer.toByteArray(), 0, bufferLength);
        }
        return marker;
    }

    private MemoryEfficientByteArrayOutputStream serializeElement(Element element) throws IOException {
        // try two times to Serialize. A ConcurrentModificationException can occur because Java's serialization
        // mechanism is not threadsafe and POJOs are seldom implemented in a threadsafe way.
        // e.g. we are serializing an ArrayList field while another thread somewhere in the application is appending to it.
        // The best we can do is try again and then give up.
        ConcurrentModificationException exception = null;
        for (int retryCount = 0; retryCount < 2; retryCount++) {
            try {
                return MemoryEfficientByteArrayOutputStream.serialize(element);
            } catch (ConcurrentModificationException e) {
                exception = e;
                try {
                    // wait for the other thread(s) to finish
                    MILLISECONDS.sleep(SERIALIZATION_CONCURRENCY_DELAY);
                } catch (InterruptedException e1) {
                    //no-op
                }
            }
        }
        throw exception;
    }

    private DiskMarker alloc(Element element, int size) throws IOException {
        //check for a matching chunk
        for (DiskMarker marker : freeChunks) {
            if (marker.getCapacity() >= size) {
                freeChunks.remove(marker);
                return createMarker(marker, element.getObjectKey(), size, element.getHitCount(), element.getExpirationTime());
            }
        }
        
        //extend file
        long position;
        synchronized (data) {
            position = data.length();
            data.setLength(position + size);
        }
        return createMarker(element.getObjectKey(), position, size, element.getHitCount(), element.getExpirationTime());
    }
    
    protected DiskMarker createMarker(DiskMarker previous, Object key, int size, long hitCount, long expiry) {
        return new DiskMarker(previous, key, size, hitCount, expiry);
    }
    
    protected DiskMarker createMarker(Object key, long position, int size, long hitCount, long expiry) {
        return new DiskMarker(key, position, size, hitCount, expiry);
    }
    
    /**
     * Free the given marker to be used by a subsequent write.
     * 
     * @param marker marker to be free'd
     */
    protected void free(DiskMarker marker) {
        freeChunks.add(marker);
    }

    /**
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        return diskQueue.size() > 10000;
    }
    
    /**
     * DiskMarker instances point to the location of their
     * associated serialized Element instance.
     */
    class DiskMarker implements ElementSubstitute {
        
        private final Object key;
        
        private final long position;
        private final int capacity;
        private final int size;
        
        private final long hitCount;
        private final long expiry;
        
        DiskMarker(Object key, long position, int size, long hitCount, long expiry) {
            this(key, position, size, size, hitCount, expiry);
        }
        
        DiskMarker(DiskMarker from, Object key, int size, long hitCount, long expiry) {
            this(key, from.getPosition(), from.getCapacity(), size, hitCount, expiry);
        }
        
        private DiskMarker(Object key, long position, int capacity, int size, long hitCount, long expiry) {
            this.key = key;
            this.position = position;
            this.capacity = capacity;
            this.size = size;
            
            this.hitCount = hitCount;
            this.expiry = expiry;
        }

        /**
         * Key to which this Element is mapped.
         * 
         * @return key for this Element
         */
        Object getKey() {
            return key;
        }

        /**
         * Disk offset at which this element is stored.
         * 
         * @return disk offset
         */
        private long getPosition() {
            return position;
        }

        /**
         * Returns the size of the currently occupying element.
         * 
         * @return size of the stored element
         */
        private int getSize() {
            return size;
        }

        /**
         * Returns the capacity of this marker.
         * <p>
         * The capacity may be smaller than the size of the current
         * occupying element.
         * 
         * @return the capacity of this marker
         */
        private int getCapacity() {
            return capacity;
        }
        
        /**
         * {@inheritDoc}
         */
        public DiskStorageFactory getFactory() {
            return DiskStorageFactory.this;
        }
    }
    
    final class DiskExpiryTask implements Runnable {

        public void run() {
            long now = System.currentTimeMillis();
            for (Object key : store.getKeyArray()) {
                Object value = store.unretrievedGet(key);
                if (created(value) && value instanceof DiskStorageFactory.DiskMarker) {
                    DiskMarker marker = (DiskMarker) value;
                    if (marker.expiry < now) {
                        if (eventService.hasCacheEventListeners()) {
                            try {
                                Element element = read(marker);
                                if (store.evict(marker.getKey(), marker)) {
                                    eventService.notifyElementExpiry(element, false);
                                }
                            } catch (Exception e) {
                                continue;
                            }
                        } else {
                            store.evict(marker.getKey(), marker);
                        }
                    }
                }
            }
        }
    }
}
