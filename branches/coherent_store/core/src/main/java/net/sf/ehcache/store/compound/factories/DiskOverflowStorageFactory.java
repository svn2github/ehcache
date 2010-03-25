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
import java.io.NotSerializableException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import net.sf.ehcache.store.compound.ElementSubstitute;
import net.sf.ehcache.store.compound.ElementSubstituteFactory;
import net.sf.ehcache.store.compound.LocalStore;

public class DiskOverflowStorageFactory extends DiskStorageFactory<ElementSubstitute> {
    
    private static final int MAX_EVICT = 5;
    private static final int SAMPLE_SIZE = 30;
    
    private static final Logger                     LOG   = LoggerFactory.getLogger(DiskOverflowStorageFactory.class);

    private final AtomicInteger                     count = new AtomicInteger();

    private final AtomicInteger placeholders = new AtomicInteger();
    private final AtomicInteger replacements = new AtomicInteger();
    
    private volatile LocalStore                     store;
    private volatile CapacityLimitedInMemoryFactory memory;

    private volatile int                            capacity;
    
    public DiskOverflowStorageFactory(Ehcache cache, String diskPath) {
        super(getDataFile(diskPath, cache));
        this.capacity = cache.getCacheConfiguration().getMaxElementsOnDisk();
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

        LOG.debug("Deleting data file " + data.getName());
        data.delete();

        return data;
    }

    public static final String getDataFileName(Ehcache cache) {
        String safeName = cache.getName().replace('/', '_');
        return safeName + ".data";
    }
    
    public void primary(CapacityLimitedInMemoryFactory memory) {
        this.memory = memory;
    }

    /**
     * Encodes an Element as a marker to on-disk location.
     * <p>
     * Immediately substitutes a placeholder for the original
     * element while the Element itself is asynchronously written
     * to disk using the executor service.
     * @throws NotSerializableException 
     */    
    public ElementSubstitute create(Object key, Element element) throws IllegalArgumentException {
        if (element.isSerializable()) {
            int size = count.incrementAndGet();
            if (capacity > 0) {
                int overflow = size - capacity;
                if (overflow > 0) {
                    evict(Math.min(MAX_EVICT, overflow), key);
                }
            }
            return new Placeholder(key, element);
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    /**
     * Decode an ElementProxy from an on disk marker (or a pending placeholder).
     * <p>
     * This implementation makes no attempt to fault in the decoded 
     * Element in place of the proxy.
     */
    public Element retrieve(Object key, ElementSubstitute proxy) {
        if (proxy instanceof DiskStorageFactory.DiskMarker) {
            try {
                DiskMarker marker = (DiskMarker) proxy;
                Element e = read((DiskMarker) proxy);
                if (key != null) {
                    schedule(new DiskFaultTask(key, marker, e));
                }
                return e;
            } catch (IOException e) {
                throw new CacheException(e);
            } catch (ClassNotFoundException e) {
                throw new CacheException(e);
            }
        } else {
            return ((Placeholder) proxy).element;
        }
    }

    public void free(ElementSubstitute substitute) {
        count.decrementAndGet();
        if (substitute instanceof DiskStorageFactory.DiskMarker) {
            free((DiskMarker) substitute);
        }
    }

    public void bind(LocalStore store) {
        this.store = store;
    }

    public void unbind(LocalStore localStore) {
        try {
            shutdown();
            delete();
        } catch (IOException e) {
            LOG.error("Could not shut down disk cache. Initial cause was " + e.getMessage(), e);
        }
    }

    private void evict(int n, Object keyHint) {
        for (int i = 0; i < n; i++) {
            List<ElementSubstitute> sample = store.getRandomSample(this, SAMPLE_SIZE, keyHint);
            if (sample.isEmpty()) {
                continue;
            }
            
            ElementSubstitute target = sample.get(0);
            if (target instanceof Placeholder) {
                Placeholder p = (Placeholder) target;
                store.evict(p.key, p.element);
            } else {
                try {
                    Element element = read((DiskMarker) target);
                    store.evict(element.getObjectKey(), element);
                } catch (Exception e) {
                    LOG.error("Could not evict", e);
                }
            }
        }
    }
    
    /**
     * Placeholder instances are put in place to prevent
     * duplicate write requests while Elements are being
     * written to disk.
     */
    class Placeholder implements ElementSubstitute {
        protected final Object key;
        protected final Element element;
        
        Placeholder(Object key, Element element) {
            this.key = key;
            this.element = element;
        }

        public ElementSubstituteFactory<ElementSubstitute> getFactory() {
            return DiskOverflowStorageFactory.this;
        }

        public void schedule() {
            DiskOverflowStorageFactory.this.schedule(new DiskWriteTask(this));
        }
    }
    
    /**
     * DiskWriteTasks are used to serialize elements
     * to disk and fault in the resultant DiskMarker
     * instance.
     */
    class DiskWriteTask implements Callable<Boolean> {

        private final Placeholder placeholder;
        
        DiskWriteTask(Placeholder p) {
            this.placeholder = p;
        }

        public Boolean call() {
            try {
                DiskMarker marker = write(placeholder.element);
                count.incrementAndGet();
                return Boolean.valueOf(store.fault(placeholder.key, placeholder, marker));
            } catch (IOException e) {
                return Boolean.valueOf(store.evict(placeholder.key, placeholder.element));
            }
        }
    }
    
    class DiskFaultTask implements Callable<Boolean> {
        private final Object key;
        private final DiskMarker marker;
        private final Element element;
        
        DiskFaultTask(Object key, DiskMarker marker, Element element) {
            this.key = key;
            this.marker = marker;
            this.element = element;
        }

        public Boolean call() {
            return Boolean.valueOf(store.exclusiveFault(key, marker, memory.create(key, element)));
        }
    }

    public int getSize() {
        return count.get();
    }

    public long getSizeInBytes() {
        throw new UnsupportedOperationException();
    }

    public void setCapacity(int newCapacity) {
        throw new UnsupportedOperationException();
    }

    public boolean created(Object object) {
        return (object instanceof ElementSubstitute) && (((ElementSubstitute) object).getFactory() == this);
    }
}
