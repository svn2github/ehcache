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

/**
 * @author Ludovic Orban
 */
public class OverflowToDiskPoolableStore extends OverflowToDiskStore implements PoolableStore {

    private final Cache cache;
    private final PoolAccessor onHeapPoolAccessor;
    private final PoolAccessor onDiskPoolAccessor;

    private class InternalEventListenerAdapter implements InternalEventListener {

        public void onFault(Object key, Object from, Object to) {
            if (from instanceof Element) {
                overflowToDisk((Element) from);
            } else {
                faultFromDisk((Element) to);
            }
        }

        public void onEvict(Object key, Element evicted) {
            evictFromDisk(evicted);
        }

        public void onUpdate(Element oldElement, Element newElement) {
            onHeapPoolAccessor.delete(oldElement.getObjectKey(), oldElement.getObjectValue(), oldElement);
        }

        public void onRemove(Object removedObject, Element removedElement) {
            if (removedObject instanceof Element) {
                onHeapPoolAccessor.delete(removedElement.getObjectKey(), removedElement.getObjectValue(), removedElement);
            } else {
                onHeapPoolAccessor.delete(removedElement.getObjectKey(), null, removedElement);
                onDiskPoolAccessor.delete(removedElement.getObjectKey(), removedElement.getObjectValue(), removedElement);
            }
        }

    }

    private OverflowToDiskPoolableStore(Cache cache, CapacityLimitedInMemoryFactory memory, DiskOverflowStorageFactory disk, CacheConfiguration config, Pool onHeapPool, Pool onDiskPool) {
        super(memory, disk, config);
        this.cache = cache;
        this.onHeapPoolAccessor = onHeapPool.createPoolAccessor(this);
        this.onDiskPoolAccessor = onDiskPool.createPoolAccessor(this);

        addInternalEventListener(new InternalEventListenerAdapter());
    }

    public static OverflowToDiskPoolableStore create(Cache cache, String diskStorePath, Pool onHeapPool, Pool onDiskPool) {
        CacheConfiguration config = cache.getCacheConfiguration();
        DiskOverflowStorageFactory disk = new DiskOverflowStorageFactory(cache, diskStorePath);
        CapacityLimitedInMemoryFactory memory = new CapacityLimitedInMemoryFactory(disk, config.getMaxElementsInMemory(),
                determineEvictionPolicy(config), cache.getCacheEventNotificationService());
        OverflowToDiskPoolableStore store = new OverflowToDiskPoolableStore(cache, memory, disk, config, onHeapPool, onDiskPool);
        cache.getCacheConfiguration().addConfigurationListener(store);
        return store;
    }

    @Override
    public void dispose() {
        super.dispose();
        onDiskPoolAccessor.unlink();
        onHeapPoolAccessor.unlink();
    }

    @Override
    public boolean put(Element element) throws CacheException {
        if (onHeapPoolAccessor.add(element.getObjectKey(), element.getObjectValue(), element) > -1) {
            return super.put(element);
        } else {
            super.remove(element.getObjectKey());
            cache.getCacheEventNotificationService().notifyElementEvicted(element, false);
            return true;
        }
    }

    private void overflowToDisk(Element e) {
        //System.out.println("to disk: " + e.getObjectKey());
        onHeapPoolAccessor.replace(Role.VALUE, e.getObjectValue(), null);
        onDiskPoolAccessor.add(e.getObjectKey(), e.getObjectValue(), e);
    }

    private void faultFromDisk(Element e) {
        //System.out.println("to heap: " + e.getObjectKey());
        onHeapPoolAccessor.replace(Role.VALUE, null, e.getObjectValue());
        onDiskPoolAccessor.delete(e.getObjectKey(), e.getObjectValue(), e);
    }

    private void evictFromDisk(Element e) {
        //System.out.println("evict: " + e.getObjectKey());
        onHeapPoolAccessor.delete(e.getObjectKey(), null, e);
        onDiskPoolAccessor.delete(e.getObjectKey(), e.getObjectValue(), e);
    }


    public boolean evictFromOnHeap(int count, long size) {
        memoryFactory.evictFromOnHeap(count);
        return true;
    }

    public boolean evictFromOffHeap(int count, long size) {
        return false;
    }

    public boolean evictFromOnDisk(int count, long size) {
        diskFactory.evictFromOnDisk(count);
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
