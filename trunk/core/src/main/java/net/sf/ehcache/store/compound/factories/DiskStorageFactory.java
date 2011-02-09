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

package net.sf.ehcache.store.compound.factories;

import net.sf.ehcache.concurrent.ConcurrencyUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.RandomAccessFile;
import java.io.Serializable;

import java.util.ConcurrentModificationException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

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

    /**
     * Path stub used to create unique ehcache directories.
     */
    protected static final String AUTO_DISK_PATH_DIRECTORY_PREFIX = "ehcache_auto_created";
    private static final int SERIALIZATION_CONCURRENCY_DELAY = 250;
    private static final int SHUTDOWN_GRACE_PERIOD = 60;
    private static final int MEGABYTE = 1024 * 1024;
    
    private static final Logger LOG = LoggerFactory.getLogger(DiskStorageFactory.class.getName());

    /**
     * The store bound to this factory.
     */
    protected volatile CompoundStore                  store;
    
    private final BlockingQueue<Runnable> diskQueue; 
    /**
     * Executor service used to write elements to disk
     */
    private final ScheduledThreadPoolExecutor diskWriter;
    
    private final long queueCapacity;
    
    private final File             file;
    private final RandomAccessFile[] dataAccess;

    private final FileAllocationTree allocator;

    private final RegisteredEventListeners eventService;

    private volatile int elementSize;
    
    /**
     * Constructs a disk storage factory using the given data file.
     * 
     * @param dataFile
     */
    DiskStorageFactory(File dataFile, long expiryInterval, long queueCapacity,
        RegisteredEventListeners eventService, final boolean daemonWriter, int stripes) {
        this.file = dataFile;
        try {
            dataAccess = allocateRandomAccessFiles(file, stripes);
        } catch (FileNotFoundException e) {
            throw new CacheException(e);
        }
        this.allocator = new FileAllocationTree(Long.MAX_VALUE, dataAccess[0]);
        
        diskWriter = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, file.getName());
                t.setDaemon(daemonWriter);
                return t;
            }
        });
        this.diskQueue = diskWriter.getQueue();
        this.eventService = eventService;
        this.queueCapacity = queueCapacity * MEGABYTE;
        
        diskWriter.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        diskWriter.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        diskWriter.scheduleWithFixedDelay(new DiskExpiryTask(), expiryInterval, expiryInterval, TimeUnit.SECONDS);
    }
    
    private static RandomAccessFile[] allocateRandomAccessFiles(File f, int stripes) throws FileNotFoundException {
        int roundedStripes = stripes;
        while ((roundedStripes & (roundedStripes - 1)) != 0) {
            ++roundedStripes;
        }

        RandomAccessFile [] result = new RandomAccessFile[roundedStripes];
        for (int i = 0; i < result.length; ++i) {
            result[i] = new RandomAccessFile(f, "rw");
        }

        return result;
    }

    private RandomAccessFile getDataAccess(Object key) {
        return this.dataAccess[ConcurrencyUtil.selectLock(key, dataAccess.length)];
    }

    /**
     * Return this size in bytes of this factory
     */
    public long getOnDiskSizeInBytes() {
        synchronized (dataAccess[0]) {
            try {
                return dataAccess[0].length();
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
     * {@inheritDoc}
     */
    public void free(Lock lock, ElementSubstitute substitute) {
        if (substitute instanceof DiskStorageFactory.DiskMarker) {
            //free done asynchronously under the relevant segment lock...
            DiskFreeTask free = new DiskFreeTask(lock, (DiskMarker) substitute);
            if (lock.tryLock()) {
                try {
                    free.call();
                } finally {
                    lock.unlock();
                }
            } else {
                schedule(free);
            }
        }
    }
    
    /**
     * Mark this on-disk marker as used (hooks into the file space allocation structure).
     */
    protected void markUsed(DiskMarker marker) {
        allocator.mark(new Region(marker.getPosition(), marker.getPosition() + marker.getSize() - 1));
    }

    /**
     * Shrink this store's data file down to a minimal size for its contents.
     */
    protected void shrinkDataFile() {
        synchronized (dataAccess[0]) {
            try {
                dataAccess[0].setLength(allocator.getFileSize());
            } catch (IOException e) {
                LOG.error("Exception trying to shrink data file to size", e);
            }
        }
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
                    LOG.info("Waited " + (i + 1) + " seconds for shutdown of [" + file.getName() + "]");
                }
            } catch (InterruptedException e) {
                LOG.warn("Received exception while waiting for shutdown", e);
            }
        }

        for (RandomAccessFile raf : dataAccess) {
            synchronized (raf) {
                raf.close();
            }
        }
    }
    
    /**
     * Deletes the data file for this factory. 
     */
    protected void delete() {
        file.delete();
        allocator.clear();
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
        RandomAccessFile data = getDataAccess(marker.getKey());
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
        elementSize = bufferLength;
        DiskMarker marker = alloc(element, bufferLength);
        // Write the record
        RandomAccessFile data = getDataAccess(element.getObjectKey());
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
        Region r = allocator.alloc(size);
        return createMarker(r.start(), size, element);
    }
    
    /**
     * Create a disk marker representing the given element, and area on disk.
     * <p>
     * This method can be overridden by subclasses to use different marker types.
     * 
     * @param position starting disk offset
     * @param size size of in disk area
     * @param element element to be written to area
     * @return marker representing the element.
     */
    protected DiskMarker createMarker(long position, int size, Element element) {
        return new OverflowDiskMarker(this, position, size, element);
    }
    
    /**
     * Free the given marker to be used by a subsequent write.
     * 
     * @param marker marker to be free'd
     */
    protected void free(DiskMarker marker) {
        allocator.free(new Region(marker.getPosition(), marker.getPosition() + marker.getSize() - 1));
    }

    /**
     * Return {@code true} if the disk write queue is full.
     */
    public boolean bufferFull() {
        return (diskQueue.size() * elementSize) > queueCapacity;
    }

    /**
     * Return a reference to the data file backing this factory.
     */
    public File getDataFile() {
        return file;
    }
    
    /**
     * DiskWriteTasks are used to serialize elements
     * to disk and fault in the resultant DiskMarker
     * instance.
     */
    abstract class DiskWriteTask implements Callable<DiskMarker> {

        private final Placeholder placeholder;
        
        /**
         * Create a disk-write task for the given placeholder.
         */
        DiskWriteTask(Placeholder p) {
            this.placeholder = p;
        }

        /**
         * Return the placeholder that this task will write.
         */
        Placeholder getPlaceholder() {
            return placeholder;
        }
        
        /**
         * {@inheritDoc}
         */
        public DiskMarker call() {
            try {
                DiskMarker marker = write(placeholder.getElement());
                if (store.fault(placeholder.getKey(), placeholder, marker)) {
                    return marker;
                } else {
                    return null;
                }
            } catch (IOException e) {
                LOG.error("Disk Write of " + placeholder.getKey() + " failed (it will be evicted instead): ", e);
                store.evict(placeholder.getKey(), placeholder);
                return null;
            }
        }
    }

    /**
     * Disk free tasks are used to asynchronously free DiskMarker instances under the correct
     * exclusive write lock.  This ensure markers are not free'd until no more readers can be
     * holding references to them.
     */
    private final class DiskFreeTask implements Callable<Void> {
        private final Lock lock;
        private final DiskMarker marker;
        
        private DiskFreeTask(Lock lock, DiskMarker marker) {
            this.lock = lock;
            this.marker = marker;
        }

        /**
         * {@inheritDoc}
         */
        public Void call() {
            lock.lock();
            try {
                DiskStorageFactory.this.free(marker);
            } finally {
                lock.unlock();
            }
            return null;
        }
    }

    /**
     * Abstract superclass for all disk substitutes.
     */
    abstract static class DiskSubstitute implements ElementSubstitute {

        private transient volatile DiskStorageFactory<? extends ElementSubstitute> factory;
        
        /**
         * Create a disk subsitute bound to no factory.  This constructor is used during
         * de-serialization.
         */
        public DiskSubstitute() {
            this.factory = null;
        }

        /**
         * Create a disk subsitute bound to the given factory.
         */
        DiskSubstitute(DiskStorageFactory<? extends ElementSubstitute> factory) {
            this.factory = factory;
        }

        /**
         * Return the key to which this marker is (or should be) mapped.
         */
        abstract Object getKey();

        /**
         * Return the total number of hits on this marker
         */
        abstract long getHitCount();
        
        /**
         * Return the time at which this marker expires.
         */
        abstract long getExpirationTime();

        /**
         * {@inheritDoc}
         */
        public final DiskStorageFactory<ElementSubstitute> getFactory() {
            return (DiskStorageFactory<ElementSubstitute>) factory;
        }
        
        /**
         * Bind this marker to a given factory.
         * <p>
         * Used during deserialization of markers to associate them with the deserializing factory.
         */
        void bindFactory(DiskStorageFactory<? extends ElementSubstitute> factory) {
            this.factory = factory;
        }
    }
    
    /**
     * Placeholder instances are put in place to prevent
     * duplicate write requests while Elements are being
     * written to disk.
     */
    abstract static class Placeholder extends DiskSubstitute {
        private final Object key;
        private final Element element;
        
        /**
         * Create a Placeholder wrapping the given element and key.
         */
        Placeholder(DiskStorageFactory<ElementSubstitute> factory, Element element) {
            super(factory);
            this.key = element.getObjectKey();
            this.element = element;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        Object getKey() {
            return key;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        long getHitCount() {
            return getElement().getHitCount();
        }
        
        @Override
        long getExpirationTime() {
            return getElement().getExpirationTime();
        }

        /**
         * Return the element that this Placeholder is wrapping.
         */
        Element getElement() {
            return element;
        }        
    }
    
    /**
     * Overflow specific disk marker implementation.
     */
    private final static class OverflowDiskMarker extends DiskMarker {

        private final long expiry;

        OverflowDiskMarker(DiskStorageFactory<? extends ElementSubstitute> factory, long position, int size, Element element) {
            super(factory, position, size, element);
            this.expiry = element.getExpirationTime();
        }

        OverflowDiskMarker(DiskStorageFactory<? extends ElementSubstitute> factory, long position, int size, Object key, long hits,
                long expiry) {
            super(factory, position, size, key, hits);
            this.expiry = expiry;
        }

        @Override
        long getExpirationTime() {
            return expiry;
        }
    }

    /**
     * DiskMarker instances point to the location of their
     * associated serialized Element instance.
     */
    static abstract class DiskMarker extends DiskSubstitute implements Serializable {
        
        private final Object key;
        
        private final long position;
        private final int size;
        
        private final long hitCount;
        
        /**
         * Create a new marker tied to the given factory instance.
         * 
         * @param factory factory responsible for this marker
         * @param position position on disk where the element will be stored
         * @param size size of the serialized element
         * @param element element being stored
         */
        DiskMarker(DiskStorageFactory<? extends ElementSubstitute> factory, long position, int size, Element element) {
            super(factory);
            this.position = position;
            this.size = size;
            
            this.key = element.getObjectKey();
            this.hitCount = element.getHitCount();
        }

        /**
         * Create a new marker tied to the given factory instance.
         * 
         * @param factory factory responsible for this marker
         * @param position position on disk where the element will be stored
         * @param size size of the serialized element
         * @param key key to which this element is mapped
         * @param hits hit count for this element
         */
        DiskMarker(DiskStorageFactory<? extends ElementSubstitute> factory, long position, int size, Object key, long hits) {
            super(factory);
            this.position = position;
            this.size = size;
            
            this.key = key;
            this.hitCount = hits;
        }
        
        /**
         * Key to which this Element is mapped.
         * 
         * @return key for this Element
         */
        @Override
        Object getKey() {
            return key;
        }

        /**
         * Number of hits on this Element.
         */
        @Override
        long getHitCount() {
            return hitCount;
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
         * {@inheritDoc}
         * <p>
         * A No-Op
         */
        public void installed() {
            //no-op
        }        
    }


    /**
     * Remove elements created by this factory if they have expired.
     */
    public void expireElements() {
        new DiskExpiryTask().run();
    }
    
    /**
     * Causes removal of all expired elements (and fires the relevant events).
     */
    private final class DiskExpiryTask implements Runnable {
        
        /**
         * {@inheritDoc}
         */
        public void run() {
            long now = System.currentTimeMillis();
            for (Object key : store.keySet()) {
                Object value = store.unretrievedGet(key);
                if (created(value) && value instanceof DiskStorageFactory.DiskMarker) {
                    checkExpiry((DiskMarker) value, now);
                }
            }
        }
        
        private void checkExpiry(DiskMarker marker, long now) {
            if (marker.getExpirationTime() < now) {
                if (eventService.hasCacheEventListeners()) {
                    try {
                        Element element = read(marker);
                        if (store.evict(marker.getKey(), marker)) {
                            eventService.notifyElementExpiry(element, false);
                        }
                    } catch (Exception e) {
                        return;
                    }
                } else {
                    store.evict(marker.getKey(), marker);
                }
            }
        }
    }
}
