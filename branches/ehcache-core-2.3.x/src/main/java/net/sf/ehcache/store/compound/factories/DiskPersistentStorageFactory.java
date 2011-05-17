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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.FifoPolicy;
import net.sf.ehcache.store.LfuPolicy;
import net.sf.ehcache.store.LruPolicy;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.DiskStore.DiskElement;
import net.sf.ehcache.store.compound.CompoundStore;
import net.sf.ehcache.store.compound.ElementSubstitute;
import net.sf.ehcache.store.compound.ElementSubstituteFilter;
import net.sf.ehcache.store.compound.factories.DiskStorageFactory.DiskMarker;

/**
 * This will be the disk-persistent element substitute factory
 * 
 * @author Chris Dennis
 */
public class DiskPersistentStorageFactory extends DiskStorageFactory<ElementSubstitute> {

    private static final Logger LOG = LoggerFactory.getLogger(DiskPersistentStorageFactory.class);
    private static final int MAX_EVICT = 5;
    private static final int SAMPLE_SIZE = 30;
    
    private final ElementSubstituteFilter<DiskSubstitute> inMemoryFilter = new InMemoryFilter();
    private final ElementSubstituteFilter<CachingDiskMarker> flushableFilter = new FlushableFilter();
    private final ElementSubstituteFilter<CachingDiskMarker> onDiskFilter = new OnDiskFilter();
    
    private final AtomicInteger inMemory = new AtomicInteger();
    private final AtomicInteger onDisk = new AtomicInteger();

    private final File indexFile;
    
    private final IndexWriteTask flushTask;
    
    private volatile int diskCapacity;
    private volatile int memoryCapacity;
    
    private volatile Policy memoryPolicy;
    
    /**
     * Constructs an disk persistent factory for the given cache and disk path.
     * 
     * @param cache cache that fronts this factory
     * @param diskPath path to store data in
     */
    public DiskPersistentStorageFactory(Ehcache cache, String diskPath) {
        super(getDataFile(diskPath, cache), cache.getCacheConfiguration().getDiskExpiryThreadIntervalSeconds(),
                cache.getCacheConfiguration().getDiskSpoolBufferSizeMB(), cache.getCacheEventNotificationService(),
                false, cache.getCacheConfiguration().getDiskAccessStripes());
        
        indexFile = new File(getDataFile().getParentFile(), getIndexFileName(cache));
        flushTask = new IndexWriteTask(indexFile, cache.getCacheConfiguration().isClearOnFlush());

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

        diskCapacity = cache.getCacheConfiguration().getMaxElementsOnDisk();
        memoryCapacity = cache.getCacheConfiguration().getMaxElementsInMemory();
        memoryPolicy = determineEvictionPolicy(cache.getCacheConfiguration());
    }

    /**
     * Chooses the Policy from the cache configuration
     */
    private static final Policy determineEvictionPolicy(CacheConfiguration config) {
        MemoryStoreEvictionPolicy policySelection = config.getMemoryStoreEvictionPolicy();

        if (policySelection.equals(MemoryStoreEvictionPolicy.LRU)) {
            return new LruPolicy();
        } else if (policySelection.equals(MemoryStoreEvictionPolicy.FIFO)) {
            return new FifoPolicy();
        } else if (policySelection.equals(MemoryStoreEvictionPolicy.LFU)) {
            return new LfuPolicy();
        }

        throw new IllegalArgumentException(policySelection + " isn't a valid eviction policy");
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
        int size = inMemory.incrementAndGet();
        inMemoryEvict(size, key);
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
                        int size = inMemory.incrementAndGet();
                        inMemoryEvict(size, key);
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
        super.bind(store);
        loadIndex();
    }
    
    /**
     * {@inheritDoc}
     */
    public void unbind(CompoundStore store) {
        try {
            flushTask.call();
            shutdown();
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
    protected DiskMarker createMarker(long position, int size, Element element) {
        return new CachingDiskMarker(this, position, size, element);
    }

    /**
     * Disk marker implementation that can hold a cached Element reference.
     */
    private final static class CachingDiskMarker extends DiskMarker implements Serializable {
        
        private static final long serialVersionUID = 43;

        private static final AtomicReferenceFieldUpdater<CachingDiskMarker, Element> CACHED_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(CachingDiskMarker.class, Element.class, "cached");
        
        private transient volatile Element cached;
        private volatile long expiry;
        
        CachingDiskMarker(DiskPersistentStorageFactory factory, long position, int size, Element element) {
            super(factory, position, size, element);
            this.expiry = element.getExpirationTime();
        }

        CachingDiskMarker(DiskPersistentStorageFactory factory, DiskElement element) {
            super(factory, element.getPosition(), element.getSize(), element.getObjectKey(), element.getHitCount());
            this.expiry = element.getExpiry();
        }

        boolean cache(Element element) {
            return CACHED_UPDATER.compareAndSet(this, null, element);
        }

        boolean flush() {
          Element old = CACHED_UPDATER.getAndSet(this, null);
          if (old != null) {
            expiry = old.getExpirationTime();
            return true;
          } else {
            return false;
          }
        }
        
        Element getCached() {
            return CACHED_UPDATER.get(this);
        }
        
        boolean isCaching() {
            return getCached() != null;
        }

        @Override
        long getExpirationTime() {
            Element e = getCached();
            return e == null ? expiry : e.getExpirationTime();
        }
    }
    
    /**
     * Filters for in-memory elements created by this factory
     */
    private class InMemoryFilter implements ElementSubstituteFilter<DiskSubstitute> {

        /**
         * {@inheritDoc}
         */
        public boolean allows(Object object) {
            if (!created(object)) {
                return false;
            }
            
            if (object instanceof PersistentPlaceholder) {
                return true;
            } else if (object instanceof CachingDiskMarker) {
                return ((CachingDiskMarker) object).isCaching();
            } else {
                return false;
            }
        }
    }

    /**
     * Filters for in-memory elements created by this factory
     */
    private class FlushableFilter implements ElementSubstituteFilter<CachingDiskMarker> {

        /**
         * {@inheritDoc}
         */
        public boolean allows(Object object) {
            if (!created(object)) {
                return false;
            }
            
            return (object instanceof CachingDiskMarker) && ((CachingDiskMarker) object).isCaching();
        }
    }

    /**
     * Filters for on-disk elements created by this factory
     */
    private class OnDiskFilter implements ElementSubstituteFilter<CachingDiskMarker> {

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
     * Return the approximate serialized size of the in-memory elements
     */
    public long getInMemorySizeInBytes() {
        long size = 0;
        for (Object o : store.getKeys()) {
            Object e = store.unretrievedGet(o);
            if (inMemoryFilter.allows(e)) {
                size += getElement((DiskSubstitute) e).getSerializedSize();
            }
        }
        return size;
    }

    /**
     * Return the number of on-disk elements
     */
    public int getOnDiskSize() {
        return onDisk.get();
    }

    /**
     * Set the maximum in-memory capacity for this factory.
     */
    public void setInMemoryCapacity(int capacity) {
        memoryCapacity = capacity;
    }
    
    /**
     * Set the maximum on-disk capacity for this factory.
     */
    public void setOnDiskCapacity(int capacity) {
        diskCapacity = capacity;
    }

    /**
     * Set the in-memory eviction policy to be used by this store.
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        memoryPolicy = policy;
    }

    /**
     * Return the in-memory eviction policy used by this store.
     */
    public Policy getInMemoryEvictionPolicy() {
        return memoryPolicy;
    }
    
    private void inMemoryEvict(int size, Object keyHint) {
        if (memoryCapacity > 0) {
            int overflow = size - memoryCapacity;
            for (int i = 0; i < Math.min(MAX_EVICT, overflow); i++) {
                CachingDiskMarker target = getMemoryEvictionTarget(keyHint, size);
                if (target != null && target.flush()) {
                    if (inMemory.decrementAndGet() <= memoryCapacity) {
                        break;
                    }
                }
            }
        }
    }
    
    private CachingDiskMarker getMemoryEvictionTarget(Object keyHint, int size) {
        List<CachingDiskMarker> sample = store.getRandomSample(flushableFilter, Math.min(SAMPLE_SIZE, size), keyHint);
        
        CachingDiskMarker target = null;
        
        for (CachingDiskMarker substitute : sample) {
            if (target == null) {
                target = substitute;
            } else {
                Element targetElement = getElement(target);
                Element element = getElement(substitute);
                if (targetElement == null || (element != null && memoryPolicy.compare(targetElement, element))) {
                    target = substitute;
                }
            }
        }
        return target;
    }

    private static final Element getElement(DiskSubstitute substitute) {
        if (substitute instanceof CachingDiskMarker) {
            return ((CachingDiskMarker) substitute).getCached();
        } else {
            return ((PersistentPlaceholder) substitute).getElement();
        }
    }
    
    private void onDiskEvict(int size, Object keyHint) {
        if (diskCapacity > 0) {
            int overflow = size - diskCapacity;
            for (int i = 0; i < Math.min(MAX_EVICT, overflow); i++) {
                DiskSubstitute target = getDiskEvictionTarget(keyHint, size);
                if (target == null) {
                    continue;
                } else {
                    if (store.evict(target.getKey(), target) && (onDisk.get() <= diskCapacity)) {
                        break;
                    }
                }
            }
        }
    }
    
    private DiskSubstitute getDiskEvictionTarget(Object keyHint, int size) {
        List<CachingDiskMarker> sample = store.getRandomSample(onDiskFilter, Math.min(SAMPLE_SIZE, size), keyHint);
        CachingDiskMarker target = null;
        for (CachingDiskMarker substitute : sample) {
            if ((target == null) || (substitute.getHitCount() < target.getHitCount())) {
                target = substitute;
            }
        }
        return target;
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
        public CachingDiskMarker call() {
            CachingDiskMarker result = (CachingDiskMarker) super.call();
            //don't want to increment on exception throw
            int disk = onDisk.incrementAndGet();
            onDiskEvict(disk, getPlaceholder().getKey());
            if (result != null && result.cache(getPlaceholder().getElement())) {
                int memory = inMemory.incrementAndGet();
                inMemoryEvict(memory, getPlaceholder().getKey());
            }
            return result;
        }
    }
    
    /**
     * Task that writes the index file for this factory.
     */
    class IndexWriteTask implements Callable<Void> {

        private final File index;
        private final boolean clearOnFlush;
        
        /**
         * Create a disk flush task that writes to the given file.
         */
        IndexWriteTask(File index, boolean clear) {
            this.index = index;
            this.clearOnFlush = clear;
        }

        /**
         * {@inheritDoc}
         */
        public synchronized Void call() throws IOException {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(index));
            try {
                for (Object key : store.keySet()) {
                    Object o = store.unretrievedGet(key);
                    if (o instanceof PersistentPlaceholder) {
                        o = new PersistentDiskWriteTask((PersistentPlaceholder) o).call();
                        if (o == null) {
                            o = store.unretrievedGet(key);
                        }
                    }

                    if (o instanceof CachingDiskMarker) {
                        CachingDiskMarker marker = (CachingDiskMarker) o;

                        if (clearOnFlush && marker.flush()) {
                            inMemory.decrementAndGet();
                        }

                        oos.writeObject(key);
                        oos.writeObject(marker);
                    }
                }
            } finally {
                oos.close();
            }
            return null;
        }
        
    }
    
    private void loadIndex() {
        if (!indexFile.exists()) {
            return;
        }
        
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(indexFile));
            try {
                Object key = ois.readObject();
                Object value = ois.readObject();
                
                if (key instanceof Map && value instanceof List) {
                    LOG.info("Loading old format index file.");
                    loadOldIndex((Map) key);
                } else {
                    CachingDiskMarker marker = (CachingDiskMarker) value;
                    while (true) {
                        marker.bindFactory(this);
                        if (store.putRawIfAbsent(key, marker)) {
                            onDisk.incrementAndGet();
                        } else {
                            throw new AssertionError();
                        }
                        markUsed(marker);
                        key = ois.readObject();
                        marker = (CachingDiskMarker) ois.readObject();
                    }
                }
            } finally {
                ois.close();
            }
        } catch (EOFException e) {
            return;
        } catch (Exception e) {
            LOG.warn("Index file {} is corrupt, deleting and ignoring it : {}", indexFile, e);
            e.printStackTrace();
            store.removeAll();
            indexFile.delete();
        } finally {
            shrinkDataFile();
        }
    }

    private void loadOldIndex(Map<Object, DiskElement> elements) {
        for (Entry<Object, DiskElement> entry : elements.entrySet()) {
            Object key = entry.getKey();
            CachingDiskMarker marker = new CachingDiskMarker(this, entry.getValue());
            
            if (store.putRawIfAbsent(key, marker)) {
                onDisk.incrementAndGet();
            } else {
                throw new AssertionError();
            }
            markUsed(marker);
        }
    }

    /**
     * Return the index file for this store.
     */
    public File getIndexFile() {
        return indexFile;
    }
}
