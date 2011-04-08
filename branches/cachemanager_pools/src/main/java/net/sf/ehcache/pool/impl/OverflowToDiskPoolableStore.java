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
                // swap to disk
                //System.out.println("onFault to disk " + key + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize());// new Exception().printStackTrace(System.out);
                Element element = (Element) from;
                onHeapPoolAccessor.replace(Role.VALUE, element.getObjectValue(), null);
                onDiskPoolAccessor.add(element.getObjectKey(), element.getObjectValue(), element);
                //System.out.println("onFault to disk " + key + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize());// new Exception().printStackTrace(System.out);
            } else if (to instanceof Element) {
                // fault from disk
                //System.out.println("onFault to heap " + key + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize());// new Exception().printStackTrace(System.out);
                Element element = (Element) to;
                onHeapPoolAccessor.replace(Role.VALUE, null, element.getObjectValue());
                onDiskPoolAccessor.delete(element.getObjectKey(), element.getObjectValue(), element);
                //System.out.println("onFault to heap " + key + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize());// new Exception().printStackTrace(System.out);
            } // else it's overflow placeholder to disk marker, ie: disk to disk
        }

        public void onEvict(Object key, Element evicted) {
            //System.out.println("onEvict " + key + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize());// new Exception().printStackTrace(System.out);
            onHeapPoolAccessor.delete(evicted.getObjectKey(), null, evicted);
            onDiskPoolAccessor.delete(evicted.getObjectKey(), evicted.getObjectValue(), evicted);
            //System.out.println("onEvict " + key + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize());// new Exception().printStackTrace(System.out);
        }

        public void onUpdate(Object removed, Element newElement) {
            //System.out.println("onUpdate " + newElement.getObjectKey() + " | " + removed + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize());// new Exception().printStackTrace(System.out);
            if (removed instanceof Element) {
                // updated object was on heap -> update on heap pool
                onHeapPoolAccessor.delete(newElement.getObjectKey(), newElement.getObjectValue(), newElement);
            } else {
                // updated object was faulted from disk -> update on disk pool
                onHeapPoolAccessor.delete(newElement.getObjectKey(), null, newElement);
                onDiskPoolAccessor.delete(newElement.getObjectKey(), newElement.getObjectValue(), newElement);
            }
            //System.out.println("onUpdate " + newElement.getObjectKey() + " | " + removed + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize());// new Exception().printStackTrace(System.out);
        }

        public void onRemove(Object removedObject, Element removedElement) {
            //System.out.println("onRemove " + removedObject + " | " + removedElement);// new Exception().printStackTrace(System.out);
            if (removedObject instanceof Element) {
                onHeapPoolAccessor.delete(removedElement.getObjectKey(), removedElement.getObjectValue(), removedElement);
            } else {
                onHeapPoolAccessor.delete(removedElement.getObjectKey(), null, removedElement);
                onDiskPoolAccessor.delete(removedElement.getObjectKey(), removedElement.getObjectValue(), removedElement);
            }
            //System.out.println("onRemove " + removedObject + " | " + removedElement);// new Exception().printStackTrace(System.out);
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
