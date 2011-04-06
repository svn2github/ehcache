package net.sf.ehcache.pool.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolableStore;
import net.sf.ehcache.pool.Role;
import net.sf.ehcache.store.compound.factories.CapacityLimitedInMemoryFactory;
import net.sf.ehcache.store.compound.factories.DiskOverflowStorageFactory;
import net.sf.ehcache.store.compound.impl.OverflowToDiskStore;

import java.util.List;

/**
 * @author Ludovic Orban
 * todo we should listen to overflow-to-disk and read-back-from-disk events
 */
public class OverflowToDiskPoolableStore extends OverflowToDiskStore implements PoolableStore, DiskOverflowStorageFactory.FaultListener {

    private final PoolAccessor onHeapPoolAccessor;
    private final PoolAccessor onDiskPoolAccessor;

    private OverflowToDiskPoolableStore(CapacityLimitedInMemoryFactory memory, DiskOverflowStorageFactory disk, CacheConfiguration config, Pool onHeapPool, Pool onDiskPool) {
        super(memory, disk, config);
        this.onHeapPoolAccessor = onHeapPool.createPoolAccessor(this);
        this.onDiskPoolAccessor = onDiskPool.createPoolAccessor(this);

        disk.addFaultListener(this);
    }

/*
    public List onHeapKeys() {
        getKeys()
    }
*/


    public static OverflowToDiskPoolableStore create(Cache cache, String diskStorePath, Pool onHeapPool, Pool onDiskPool) {
        CacheConfiguration config = cache.getCacheConfiguration();
        DiskOverflowStorageFactory disk = new DiskOverflowStorageFactory(cache, diskStorePath);
        CapacityLimitedInMemoryFactory memory = new CapacityLimitedInMemoryFactory(disk, config.getMaxElementsInMemory(),
                determineEvictionPolicy(config), cache.getCacheEventNotificationService());
        OverflowToDiskPoolableStore store = new OverflowToDiskPoolableStore(memory, disk, config, onHeapPool, onDiskPool);
        cache.getCacheConfiguration().addConfigurationListener(store);
        return store;
    }

    @Override
    public boolean put(Element element) throws CacheException {
        if (onHeapPoolAccessor.add(element.getObjectKey(), element.getObjectValue(), element)) {
            return super.put(element);
        } else {
            super.remove(element.getObjectKey());
            // todo: fire eviction event
            return true;
        }
    }

    @Override
    public Element remove(Object key) {
        Object[] feedback = super.feedbackRemove(key);
        Element removedElement = (Element) feedback[0];
        Object onDiskSubstitute = feedback[1];

        if (removedElement == null) {
            return null;
        }

        if (onDiskSubstitute == null) {
            // was only on-heap
            onHeapPoolAccessor.delete(removedElement.getObjectKey(), removedElement.getObjectValue(), removedElement);
        } else {
            // was on-disk
            onHeapPoolAccessor.delete(removedElement.getObjectKey(), onDiskSubstitute, removedElement);
            onDiskPoolAccessor.delete(removedElement.getObjectKey(), removedElement.getObjectValue(), removedElement);
        }

        return removedElement;
    }

    public void toDisk(Element e) {
        onHeapPoolAccessor.replace(Role.VALUE, e.getObjectValue(), null);
        onDiskPoolAccessor.add(e.getObjectKey(), e.getObjectValue(), e);

    }

    public void toOnHeap(Element e) {
        onHeapPoolAccessor.replace(Role.VALUE, null, e.getObjectValue());
        onDiskPoolAccessor.delete(e.getObjectKey(), e.getObjectValue(), e);
    }


    public boolean evictFromOnHeap(int count) {
        // do the eviction
        List[] lists = super.memoryFactory.evictFromOnHeap(count);

        // update onHeap Pool with what has been evicted
        List evictedFromOnHeap = lists[0];
        for (Object o : evictedFromOnHeap) {
            Element e = (Element) o;
            onHeapPoolAccessor.delete(e.getObjectKey(), e.getObjectValue(), e);
        }

        // update onHeap Pool with what has been faulted to disk
        List faultedToDiskElements = lists[1];
        for (int i = 0; i < faultedToDiskElements.size(); i++) {
            Element faultedToDiskElement = (Element) faultedToDiskElements.get(i);

            onHeapPoolAccessor.replace(Role.VALUE, faultedToDiskElement.getObjectValue(), null);
            onDiskPoolAccessor.add(faultedToDiskElement.getObjectKey(), faultedToDiskElement.getObjectValue(), faultedToDiskElement);

            //todo onDiskPoolAccessor.add may return false
        }

        return true;
    }

    public boolean evictFromOffHeap(int count) {
        return false;
    }

    public boolean evictFromOnDisk(int count) {
        List evicted = diskFactory.evictFromOnDisk(count);
        for (Object o : evicted) {
            Element e = (Element) o;
            onDiskPoolAccessor.delete(e.getObjectKey(), e.getObjectValue(), e);
        }
        return true;
    }

    @Override
    public long getInMemorySizeInBytes() {
        return onHeapPoolAccessor.getSize();
    }

    @Override
    public long getOffHeapSizeInBytes() {
        return 0L;
    }

    @Override
    public long getOnDiskSizeInBytes() {
        return onDiskPoolAccessor.getSize();
    }

}
