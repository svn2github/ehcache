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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import net.sf.ehcache.store.compound.ElementSubstitute;
import net.sf.ehcache.store.compound.CompoundStore;
import net.sf.ehcache.store.compound.ElementSubstituteFilter;
import net.sf.ehcache.transaction.SoftLock;

/**
 * A factory that stores elements on disk in their serialized form.
 * 
 * @author Chris Dennis
 */
public class DiskOverflowStorageFactory extends DiskStorageFactory<ElementSubstitute> {
    
    private static final int MAX_EVICT = 5;
    private static final int SAMPLE_SIZE = 30;
    
    private static final Logger                     LOG   = LoggerFactory.getLogger(DiskOverflowStorageFactory.class);

    private final ElementSubstituteFilter<DiskStorageFactory.DiskSubstitute> filter = 
        new ElementSubstituteFilter<DiskStorageFactory.DiskSubstitute>() {
        /**
         * {@inheritDoc}
         */
        public boolean allows(Object object) {
            return created(object);
        }
    };
    
    private final AtomicInteger                     count = new AtomicInteger();

    private volatile CapacityLimitedInMemoryFactory memory;

    private volatile int                            capacity;

    /**
     * Constructs an overflow factory for the given cache and disk path.
     * 
     * @param cache cache that fronts this factory
     * @param diskPath path to store data in
     */
    public DiskOverflowStorageFactory(Ehcache cache, String diskPath) {
        super(getDataFile(diskPath, cache), cache.getCacheConfiguration().getDiskExpiryThreadIntervalSeconds(),
                cache.getCacheConfiguration().getDiskSpoolBufferSizeMB(), cache.getCacheEventNotificationService(),
                true, cache.getCacheConfiguration().getDiskAccessStripes());
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

    private static final String getDataFileName(Ehcache cache) {
        String safeName = cache.getName().replace('/', '_');
        return safeName + ".data";
    }
    
    /**
     * Sets the primary factory that this factory should fault to, when
     * elements are retrieved.
     */
    public void primary(CapacityLimitedInMemoryFactory memory) {
        this.memory = memory;
    }

    /**
     * Encodes an Element as a marker to on-disk location.
     * <p>
     * Immediately substitutes a placeholder for the original
     * element while the Element itself is asynchronously written
     * to disk using the executor service.
     * @throws IllegalArgumentException 
     */    
    public ElementSubstitute create(Object key, Element element) throws IllegalArgumentException {
        if (element.isSerializable() || element.getObjectValue() instanceof SoftLock) {
            int size = count.incrementAndGet();
            if (capacity > 0) {
                int overflow = size - capacity;
                if (overflow > 0) {
                    evict(Math.min(MAX_EVICT, overflow), key, size);
                }
            }
            return new OverflowPlaceholder(element);
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
                OverflowDiskMarker marker = (OverflowDiskMarker) proxy;
                Element e = marker.getSoftLockedElement();
                if (e == null) {
                    e = read(marker);
                }
                if (key != null) {
                    store.tryFault(key, marker, memory.create(key, e));
                }
                return e;
            } catch (IOException e) {
                throw new CacheException(e);
            } catch (ClassNotFoundException e) {
                throw new CacheException(e);
            }
        } else {
            Element e = ((Placeholder) proxy).getElement();
            if (key != null) {
                store.fault(key, proxy, memory.create(key, e));
            }
            return e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void free(Lock lock, ElementSubstitute substitute) {
        count.decrementAndGet();
        super.free(lock, substitute);
    }

    /**
     * {@inheritDoc}
     */
    public void unbind(CompoundStore localStore) {
        try {
            shutdown();
            delete();
        } catch (IOException e) {
            LOG.error("Could not shut down disk cache. Initial cause was " + e.getMessage(), e);
        }
    }

    private void evict(int n, Object keyHint, int size) {
        for (int i = 0; i < n; i++) {
            DiskSubstitute target = getEvictionTarget(keyHint, size);
            if (target == null) {
                continue;
            } else {
                store.evict(target.getKey(), target);
            }
        }
    }
    
    private DiskSubstitute getEvictionTarget(Object keyHint, int size) {
        List<DiskStorageFactory.DiskSubstitute> sample = store.getRandomSample(filter, Math.min(SAMPLE_SIZE, size), keyHint);
        DiskSubstitute target = null;
        for (DiskSubstitute substitute : sample) {
            if ((target == null) || (substitute.getHitCount() < target.getHitCount())) {
                target = substitute;
            }
        }
        return target;
    }


    protected DiskMarker createMarker(long position, int size, Element element) {
        return new OverflowDiskMarker(this, position, size, element);
    }

    /**
     * Overflow specific disk marker implementation.
     */
    private final static class OverflowDiskMarker extends DiskMarker {

        private final long expiry;

        private static final AtomicReferenceFieldUpdater<OverflowDiskMarker, Element> SOFTLOCKED_ELEMENT_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(OverflowDiskMarker.class, Element.class, "softLockedElement");

        private transient volatile Element softLockedElement;

        OverflowDiskMarker(DiskStorageFactory<? extends ElementSubstitute> factory, long position, int size, Element element) {
            super(factory, position, size, element);
            if (element.getObjectValue() instanceof SoftLock) {
                SOFTLOCKED_ELEMENT_UPDATER.compareAndSet(this, null, element);
            }
            this.expiry = element.getExpirationTime();
        }

        OverflowDiskMarker(DiskStorageFactory<? extends ElementSubstitute> factory, long position, int size, Object key, long hits,
                long expiry) {
            super(factory, position, size, key, hits);
            this.expiry = expiry;
        }

        Element getSoftLockedElement() {
            return SOFTLOCKED_ELEMENT_UPDATER.get(this);
        }

        @Override
        long getExpirationTime() {
            return expiry;
        }
    }

    /**
     * Placeholder implementation for the overflow-to-disk factory.
     */
    class OverflowPlaceholder extends Placeholder {

        /**
         * Creates a overflow to disk Placeholder for the given key and element
         */
        OverflowPlaceholder(Element element) {
            super(DiskOverflowStorageFactory.this, element);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Schedules the disk write for this Placeholder.
         */
        public void installed() {
            DiskOverflowStorageFactory.this.schedule(new OverflowDiskWriteTask(this));
        }
    }
    
    /**
     * DiskWriteTasks are used to serialize elements
     * to disk and fault in the resultant DiskMarker
     * instance.
     */
    private final class OverflowDiskWriteTask extends DiskWriteTask {

        private OverflowDiskWriteTask(Placeholder p) {
            super(p);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DiskMarker call() {
            DiskMarker result = super.call();
            //don't want to increment on exception throw
            int size = count.incrementAndGet();
            if (capacity > 0) {
                int overflow = size - capacity;
                if (overflow > 0) {
                    evict(Math.min(MAX_EVICT, overflow), result.getKey(), size);
                }
            }
            return result;
        }
    }

    /**
     * Return the count of elements created by this factory.
     */
    public int getSize() {
        return count.get();
    }

    /**
     * Set the maximum on-disk capacity for this factory.
     */
    public void setCapacity(int newCapacity) {
        this.capacity = newCapacity;
    }

    /**
     * {@inheritDoc}
     */
    public boolean created(Object object) {
        return (object instanceof ElementSubstitute) && (((ElementSubstitute) object).getFactory() == this);
    }
}
