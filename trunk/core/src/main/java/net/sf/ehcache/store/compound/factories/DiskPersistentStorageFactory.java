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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.compound.CompoundStore;
import net.sf.ehcache.store.compound.ElementSubstitute;
import net.sf.ehcache.store.compound.ElementSubstituteFilter;

/**
 * This will be the disk-persistent element substitute factory
 * 
 * @author Chris Dennis
 */
public class DiskPersistentStorageFactory extends DiskStorageFactory<ElementSubstitute> {

    private static final Logger LOG = LoggerFactory.getLogger(DiskPersistentStorageFactory.class);
    private static final int MAX_EVICT = 5;
    
    private final ElementSubstituteFilter<ElementSubstitute> inMemoryFilter = new InMemoryFilter();
    private final ElementSubstituteFilter<ElementSubstitute> onDiskFilter = new OnDiskFilter();
    
    private final AtomicInteger inMemory = new AtomicInteger();
    private final AtomicInteger onDisk = new AtomicInteger();

    private final File indexFile;
    
    private final IndexWriteTask flushTask;

    /**
     * Constructs an disk persistent factory for the given cache and disk path.
     * 
     * @param cache cache that fronts this factory
     * @param diskPath path to store data in
     */
    public DiskPersistentStorageFactory(Ehcache cache, String diskPath) {
        super(getDataFile(diskPath, cache), cache.getCacheConfiguration().getDiskExpiryThreadIntervalSeconds(),
                cache.getCacheConfiguration().getDiskSpoolBufferSizeMB(), cache.getCacheEventNotificationService());
        
        indexFile = new File(getDataFile().getParentFile(), getIndexFileName(cache));
        flushTask = new IndexWriteTask(indexFile);
        
        if (!getDataFile().exists() || (getDataFile().length() == 0)) {
            LOG.debug("Matching data file missing (or empty) for index file. Deleting index file " + indexFile);
            indexFile.delete();
        } else if (getDataFile().exists() && indexFile.exists()) {
            if (getDataFile().lastModified() > (indexFile.lastModified() + TimeUnit.SECONDS.toMillis(1))) {
                LOG.warn("The index for data file {} is out of date, probably due to an unclean shutdown. " 
                        + "Deleting index file {}", getDataFile(), indexFile);
                indexFile.delete();
            }
        }        
    }

    private static File getDataFile(String diskPath, Ehcache cache) {
        if (diskPath == null) {
            throw new CacheException(cache.getName() + " Cache: Could not create disk store. "
                    + "This CacheManager configuration does not allow creation of DiskStores. "
                    + "If you wish to create DiskStores, please configure a diskStore path.");
        }

        final File diskDir = new File(diskPath);
        // Make sure the cache directory exists
        if (diskDir.exists() && !diskDir.isDirectory()) {
            throw new CacheException("Store directory \"" + diskDir.getAbsolutePath() + "\" exists and is not a directory.");
        }
        if (!diskDir.exists() && !diskDir.mkdirs()) {
            throw new CacheException("Could not create cache directory \"" + diskDir.getAbsolutePath() + "\".");
        }

        File data = new File(diskDir, getDataFileName(cache));

        //if diskpath contains auto generated string
        if (diskPath.indexOf(AUTO_DISK_PATH_DIRECTORY_PREFIX) != -1) {
            LOG.warn("Data in persistent disk stores is ignored for stores from automatically created directories"
                    + " (they start with " + AUTO_DISK_PATH_DIRECTORY_PREFIX + ").\n"
                    + "Remove diskPersistent or resolve the conflicting disk paths in cache configuration.\n"
                    + "Deleting data file " + data.getAbsolutePath());
            data.delete();
        }

        return data;
    }
    
    private static final String getDataFileName(Ehcache cache) {
        String safeName = cache.getName().replace('/', '_');
        return safeName + ".data";
    }
    
    private static final String getIndexFileName(Ehcache cache) {
        String safeName = cache.getName().replace('/', '_');
        return safeName + ".index";
    }
    
    /**
     * {@inheritDoc}
     */
    public ElementSubstitute create(Object key, Element element) throws IllegalArgumentException {
        inMemory.incrementAndGet();
        return new PersistentPlaceholder(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void free(Lock exclusion, ElementSubstitute object) {
        if (object instanceof PersistentPlaceholder) {
            inMemory.decrementAndGet();
        } else if (object instanceof CachingDiskMarker) {
            CachingDiskMarker marker = (CachingDiskMarker) object;
            onDisk.decrementAndGet();
            if (marker.flush()) {
                inMemory.decrementAndGet();
            }
        }
        super.free(exclusion, object);
    }

    /**
     * {@inheritDoc}
     */
    public Element retrieve(Object key, ElementSubstitute object) {
        if (object instanceof CachingDiskMarker) {
            try {
                CachingDiskMarker marker = (CachingDiskMarker) object;
                Element element = marker.getCached();
                if (element == null) {
                    element = read(marker);
                    if (marker.cache(element)) {
                        inMemory.incrementAndGet();
                    }
                }
                return element;
            } catch (IOException e) {
                throw new CacheException(e);
            } catch (ClassNotFoundException e) {
                throw new CacheException(e);
            }
        } else if (object instanceof DiskStorageFactory.Placeholder) {
            return ((Placeholder) object).getElement();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean created(Object object) {
        if (object instanceof ElementSubstitute) {
            return ((ElementSubstitute) object).getFactory() == this;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bind(CompoundStore store) {
        loadIndex(store);
        super.bind(store);
    }
    
    /**
     * {@inheritDoc}
     */
    public void unbind(CompoundStore store) {
        try {
            shutdown();
            flushTask.call();
            if (getDataFile().getAbsolutePath().contains(AUTO_DISK_PATH_DIRECTORY_PREFIX)) {
                indexFile.delete();
                delete();
            }
        } catch (IOException e) {
            LOG.error("Could not shut down disk cache. Initial cause was " + e.getMessage(), e);
        }
    }

    /**
     * Schedule a flush (index write) for this factory.
     */
    public Future<Void> flush() {
        return schedule(flushTask);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DiskMarker createMarker(DiskMarker previous, int size, Element element) {
        return new CachingDiskMarker(this, previous, size, element);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DiskMarker createMarker(long position, int size, Element element) {
        return new CachingDiskMarker(this, position, size, element);
    }

    /**
     * Disk marker implementation that can hold a cached Element reference.
     */
    private final static class CachingDiskMarker extends DiskMarker implements Serializable {
        
        private static final long serialVersionUID = 42;
        private static final AtomicReferenceFieldUpdater<CachingDiskMarker, Element> CACHED_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(CachingDiskMarker.class, Element.class, "cached");
        
        private transient volatile Element cached;
        
        CachingDiskMarker(DiskPersistentStorageFactory factory, DiskMarker previous, int size, Element element) {
            super(factory, previous, size, element);
        }

        CachingDiskMarker(DiskPersistentStorageFactory factory, long position, int size, Element element) {
            super(factory, position, size, element);
        }

        boolean cache(Element element) {
            return CACHED_UPDATER.compareAndSet(this, null, element);
        }

        boolean flush() {
            return CACHED_UPDATER.getAndSet(this, null) != null;
        }
        
        Element getCached() {
            return CACHED_UPDATER.get(this);
        }
        
        boolean isCaching() {
            return getCached() == null;
        }
    }
    
    /**
     * Filters for in-memory elements created by this factory
     */
    private class InMemoryFilter implements ElementSubstituteFilter<ElementSubstitute> {

        /**
         * {@inheritDoc}
         */
        public boolean allows(Object object) {
            if (!created(object)) {
                return false;
            }
            
            if (object instanceof DiskStorageFactory.Placeholder) {
                return true;
            } else if (object instanceof CachingDiskMarker) {
                return ((CachingDiskMarker) object).isCaching();
            } else {
                return false;
            }
        }
    }

    /**
     * Filters for on-disk elements created by this factory
     */
    private class OnDiskFilter implements ElementSubstituteFilter<ElementSubstitute> {

        /**
         * {@inheritDoc}
         */
        public boolean allows(Object object) {
            if (!created(object)) {
                return false;
            }
            
            if (object instanceof CachingDiskMarker) {
                return !((CachingDiskMarker) object).isCaching();
            } else {
                return false;
            }
        }
    }

    /**
     * Return {@code true} if the given element is in memory
     */
    public boolean isInMemory(Object object) {
        return inMemoryFilter.allows(object);
    }
    
    /**
     * Return {@code true} if the given element is on disk
     */
    public boolean isOnDisk(Object object) {
        return onDiskFilter.allows(object);
    }

    /**
     * Return the number of in-memory elements
     */
    public int getInMemorySize() {
        return inMemory.get();
    }
    
    /**
     * Return the number of on-disk elements
     */
    public int getOnDiskSize() {
        return onDisk.get();
    }

    /**
     * Placeholder implementation for disk persistent stores.
     */
    private final class PersistentPlaceholder extends Placeholder {

        /**
         * Create a disk persistent placeholder around the given key, element pair.
         */
        PersistentPlaceholder(Element element) {
            super(DiskPersistentStorageFactory.this, element);
        }

        public void installed() {
            DiskPersistentStorageFactory.this.schedule(new PersistentDiskWriteTask(this));
        }
    }

    /**
     * Disk write task implementation for disk persistent stores.
     */
    private final class PersistentDiskWriteTask extends DiskWriteTask {

        /**
         * Create a disk persistent disk-write task for this placeholder.
         */
        PersistentDiskWriteTask(PersistentPlaceholder p) {
            super(p);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean call() {
            Boolean result = super.call();
            //don't want to increment on exception throw
            onDisk.incrementAndGet();
            return result;
        }
    }
    
    /**
     * Task that writes the index file for this factory.
     */
    class IndexWriteTask implements Callable<Void> {

        private final File index;

        /**
         * Create a disk flush task that writes to the given file.
         */
        IndexWriteTask(File index) {
            this.index = index;
        }

        /**
         * {@inheritDoc}
         */
        public synchronized Void call() throws IOException {
            //lock all segments
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(index));
            try {
                for (Object key : store.keySet()) {
                    Object o = store.unretrievedGet(key);
                    if (o instanceof PersistentPlaceholder) {
                        new PersistentDiskWriteTask((PersistentPlaceholder) o).call();
                        o = store.unretrievedGet(key);
                    }
                    
                    CachingDiskMarker marker = (CachingDiskMarker) o;
                    if (true) {
                        marker.flush();
                    }

                    //write stuff out to disk here
                    //key, value pairs
                    oos.writeObject(key);
                    oos.writeObject(marker);
                }
            } finally {
                oos.close();
                //unlock all segments
            }
            return null;
        }
        
    }
    
    private void loadIndex(CompoundStore store) {
        if (!indexFile.exists()) {
            return;
        }
        
        try {
            //lock all segments?
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(indexFile));
            try {
                while (true) {
                    Object key = ois.readObject();
                    CachingDiskMarker marker = (CachingDiskMarker) ois.readObject();
                    marker.bindFactory(this);
                    if (store.putRawIfAbsent(key, marker)) {
                        onDisk.incrementAndGet();
                    } else {
                        throw new AssertionError();
                    }
                }
            } finally {
                ois.close();
                //unlock all segments?
            }
        } catch (EOFException e) {
            return;
        } catch (Exception e) {
            LOG.warn("Index file {} is corrupt, deleting and ignoring it : {}", indexFile, e);
            store.removeAll();
            indexFile.delete();
        }
    }

    /**
     * Return the index file for this store.
     */
    public File getIndexFile() {
        return indexFile;
    }
}
