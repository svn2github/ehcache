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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
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
import net.sf.ehcache.store.compound.ElementSubstituteFactory;
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

    public DiskPersistentStorageFactory(Ehcache cache, String diskPath) {
        super(getDataFile(diskPath, cache), cache.getCacheConfiguration().getDiskExpiryThreadIntervalSeconds(),
                cache.getCacheEventNotificationService());
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
//        } else if (!readIndex()) {
//            if (LOG.isDebugEnabled()) {
//                LOG.debug("Index file dirty or empty. Deleting data file " + data.getAbsolutePath());
//            }
//            data.delete();
        }

        return data;
    }
    
    private static final String getDataFileName(Ehcache cache) {
        String safeName = cache.getName().replace('/', '_');
        return safeName + ".data";
    }
    
    public ElementSubstitute create(Object key, Element element) throws IllegalArgumentException {
        inMemory.incrementAndGet();
        return new Placeholder(key, element);
    }

    public void free(Lock exclusion, ElementSubstitute object) {
        if (object instanceof Placeholder) {
            inMemory.decrementAndGet();
        } else if (object instanceof CachingDiskMarker) {
            CachingDiskMarker marker = (CachingDiskMarker) object;
            onDisk.decrementAndGet();
            if (marker.flush()) {
                inMemory.decrementAndGet();
            }
        }
    }

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
        } else if (object instanceof Placeholder) {
            return ((Placeholder) object).element;
        } else {
            return null;
        }
    }

    public boolean created(Object object) {
        if (object instanceof ElementSubstitute) {
            return ((ElementSubstitute) object).getFactory() == this;
        } else {
            return false;
        }
    }

    @Override
    public void bind(CompoundStore store) {
        //load index file and put existing mappings into the store
        //add free markers to the list of free markers
        super.bind(store);
    }
    
    public void unbind(CompoundStore store) {
        try {
            shutdown();
            //don't really do this!!!
            delete();
            //schedule index file write
            new DiskFlushTask().call();
        } catch (IOException e) {
            LOG.error("Could not shut down disk cache. Initial cause was " + e.getMessage(), e);
        }
    }

    @Override
    protected DiskMarker createMarker(DiskMarker previous, Object key, int size, long hitCount, long expiry) {
        return new CachingDiskMarker(previous, key, size, hitCount, expiry);
    }
    
    @Override
    protected DiskMarker createMarker(Object key, long position, int size, long hitCount, long expiry) {
        return new CachingDiskMarker(key, position, size, hitCount, expiry);
    }

    /**
     * Placeholder instances are put in place to prevent
     * duplicate write requests while Elements are being
     * written to disk.
     */
    final class Placeholder implements ElementSubstitute {
        private final Object key;
        private final Element element;
        
        private Placeholder(Object key, Element element) {
            this.key = key;
            this.element = element;
        }

        /**
         * {@inheritDoc}
         */
        public ElementSubstituteFactory<ElementSubstitute> getFactory() {
            return DiskPersistentStorageFactory.this;
        }
    }

    private static final AtomicReferenceFieldUpdater<CachingDiskMarker, Element> CACHED_UPDATER = AtomicReferenceFieldUpdater.newUpdater(CachingDiskMarker.class, Element.class, "cached");
    
    final class CachingDiskMarker extends DiskMarker {

        protected volatile Element cached;
        
        public CachingDiskMarker(DiskMarker previous, Object key, int size, long hitCount, long expiry) {
            super(previous, key, size, hitCount, expiry);
        }

        public CachingDiskMarker(Object key, long position, int size, long hitCount, long expiry) {
            super(key, position, size, hitCount, expiry);
        }

        public boolean cache(Element element) {
            return CACHED_UPDATER.compareAndSet(this, null, element);
        }

        public boolean flush() {
            return CACHED_UPDATER.getAndSet(this, null) != null;
        }
        
        public Element getCached() {
            return CACHED_UPDATER.get(this);
        }
        
        public boolean isCaching() {
            return getCached() == null;
        }
    }
    
    class InMemoryFilter implements ElementSubstituteFilter<ElementSubstitute> {

        public boolean allows(Object object) {
            if (!created(object)) {
                return false;
            }
            
            if (object instanceof Placeholder) {
                return true;
            } else if (object instanceof CachingDiskMarker) {
                return ((CachingDiskMarker) object).isCaching();
            } else {
                return false;
            }
        }
    }
    
    class OnDiskFilter implements ElementSubstituteFilter<ElementSubstitute> {

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

    public boolean isInMemory(Object object) {
        return inMemoryFilter.allows(object);
    }
    
    public boolean isOnDisk(Object object) {
        return onDiskFilter.allows(object);
    }

    public int getInMemorySize() {
        return inMemory.get();
    }
    
    public int getOnDiskSize() {
        return onDisk.get();
    }

    class DiskFlushTask implements Callable<Void> {

        public Void call() {
            //lock all segments
            try {
                for (Object key : store.keySet()) {
                    Object o = store.unretrievedGet(key);
                    if (o instanceof Placeholder) {
                        //run the disk write task inline here
                    }
                    if (o instanceof CachingDiskMarker) {
                        ((CachingDiskMarker) o).flush();
                    }

                    //write stuff out to disk here
                    //key, value pairs
                }
            } finally {
                //unlock all segments
            }
            return null;
        }
        
    }
}
