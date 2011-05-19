package net.sf.ehcache.pool.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolableStore;
import net.sf.ehcache.pool.Role;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.compound.factories.CapacityLimitedInMemoryFactory;
import net.sf.ehcache.store.compound.factories.DiskOverflowStorageFactory;
import net.sf.ehcache.store.compound.impl.OverflowToDiskStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ludovic Orban
 */
public class OverflowToDiskPoolableStore extends OverflowToDiskStore implements PoolableStore {

    private final Logger LOG = LoggerFactory.getLogger(OverflowToDiskPoolableStore.class);

    private final Cache cache;
    private final PoolAccessor onHeapPoolAccessor;
    private final PoolAccessor onDiskPoolAccessor;

    private class InternalEventListenerAdapter implements InternalEventListener {

        public void onFault(Object key, Object from, Object to) {
            if (from instanceof Element) {
                // swap to disk
                if (LOG.isDebugEnabled()) { LOG.debug("onFault to disk " + from + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize()); }
                Element element = (Element) from;
                onHeapPoolAccessor.replace(Role.VALUE, element.getObjectValue(), null);
                onDiskPoolAccessor.add(element.getObjectKey(), element.getObjectValue(), element);
                if (LOG.isDebugEnabled()) { LOG.debug("/onFault to disk " + from + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize()); }
            } else if (to instanceof Element) {
                // fault from disk
                if (LOG.isDebugEnabled()) { LOG.debug("onFault to heap " + to + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize()); }
                Element element = (Element) to;
                onHeapPoolAccessor.replace(Role.VALUE, null, element.getObjectValue());
                onDiskPoolAccessor.delete(element.getObjectKey(), element.getObjectValue(), element);
                if (LOG.isDebugEnabled()) { LOG.debug("/onFault to heap " + to + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize()); }
            } // else it's overflow placeholder to disk marker, ie: disk to disk
        }

        public void onEvict(Object key, Element evicted) {
            if (LOG.isDebugEnabled()) { LOG.debug("onEvict " + key + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize()); }
            onHeapPoolAccessor.delete(evicted.getObjectKey(), null, evicted);
            onDiskPoolAccessor.delete(evicted.getObjectKey(), evicted.getObjectValue(), evicted);
            if (LOG.isDebugEnabled()) { LOG.debug("/onEvict " + key + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize()); }
        }

        public void onUpdate(Object removed, Element newElement) {
            if (LOG.isDebugEnabled()) { LOG.debug("onUpdate " + newElement.getObjectKey() + " | " + removed + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize()); }
            if (removed instanceof Element) {
                // updated object was on heap -> update on heap pool
                onHeapPoolAccessor.delete(newElement.getObjectKey(), newElement.getObjectValue(), newElement);
            } else {
                // updated object was faulted from disk -> update on disk pool
                onHeapPoolAccessor.delete(newElement.getObjectKey(), null, newElement);
                onDiskPoolAccessor.delete(newElement.getObjectKey(), newElement.getObjectValue(), newElement);
            }
            if (LOG.isDebugEnabled()) { LOG.debug("/onUpdate " + newElement.getObjectKey() + " | " + removed + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize()); }
        }

        public void onRemove(Object removedObject, Element removedElement) {
            if (LOG.isDebugEnabled()) { LOG.debug("onRemove " + removedObject + " | " + removedElement); }
            if (removedObject instanceof Element) {
                onHeapPoolAccessor.delete(removedElement.getObjectKey(), removedElement.getObjectValue(), removedElement);
            } else {
                onHeapPoolAccessor.delete(removedElement.getObjectKey(), null, removedElement);
                onDiskPoolAccessor.delete(removedElement.getObjectKey(), removedElement.getObjectValue(), removedElement);
            }
            if (LOG.isDebugEnabled()) { LOG.debug("/onRemove " + removedObject + " | " + removedElement); }
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
        DiskOverflowStorageFactory disk = new DiskOverflowStorageFactory(cache, diskStorePath, cache.getCacheEventNotificationService());
        int capacity = config.getMaxElementsInMemory();
        if (config.getPinningConfiguration() != null) {
            capacity = 0;
        }
        CapacityLimitedInMemoryFactory memory = new CapacityLimitedInMemoryFactory(disk, capacity,
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
        if (element == null) {
            return false;
        }

        if (onHeapPoolAccessor.add(element.getObjectKey(), element.getObjectValue(), element) > -1) {
            return super.put(element);
        } else {
            super.remove(element.getObjectKey());
            cache.getCacheEventNotificationService().notifyElementEvicted(element, false);
            return true;
        }
    }

    @Override
    public Element putIfAbsent(Element element) throws NullPointerException {
        //todo: there is a chance that the element is present but will get evicted by add() which makes super.putIfAbsent return null while it should not
        if (onHeapPoolAccessor.add(element.getObjectKey(), element.getObjectValue(), element) > -1) {
            Element oldElement = super.putIfAbsent(element);
            if (oldElement != null) {
                onHeapPoolAccessor.delete(element.getObjectKey(), element.getObjectValue(), element);
            }
            return oldElement;
        } else {
            cache.getCacheEventNotificationService().notifyElementEvicted(element, false);
            return element;
        }
    }

    @Override
    public Element replace(Element element) throws NullPointerException {
        //todo: there is a chance that the element is present but will get evicted by add() which makes super.replace return null while it should not
        if (onHeapPoolAccessor.add(element.getObjectKey(), element.getObjectValue(), element) > -1) {
            Element oldElement = super.replace(element);
            if (oldElement == null) {
                onHeapPoolAccessor.delete(element.getObjectKey(), element.getObjectValue(), element);
            }
            return oldElement;
        } else {
            cache.getCacheEventNotificationService().notifyElementEvicted(element, false);
            return null;
        }
    }

    @Override
    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException, IllegalArgumentException {
        //todo: there is a chance that the element is present but will get evicted by add() which makes super.replace return null while it should not
        if (onHeapPoolAccessor.add(element.getObjectKey(), element.getObjectValue(), element) > -1) {
            boolean replaced = super.replace(old, element, comparator);
            if (!replaced) {
                onHeapPoolAccessor.delete(element.getObjectKey(), element.getObjectValue(), element);
            }
            return replaced;
        } else {
            cache.getCacheEventNotificationService().notifyElementEvicted(element, false);
            return false;
        }
    }

    @Override
    public void removeAll() {
        super.removeAll();
        onHeapPoolAccessor.clear();
        onDiskPoolAccessor.clear();
    }

    public boolean evictFromOnHeap(int count, long size) {
        //todo: all on-heap memory may be consumed by on-disk elements, we may have to evict from there too
        return memoryFactory.evictFromOnHeap(count);
    }

    public boolean evictFromOffHeap(int count, long size) {
        return false;
    }

    public boolean evictFromOnDisk(int count, long size) {
        return diskFactory.evictFromOnDisk(count);
    }

    @Override
    public long getInMemorySizeInBytes() {
        if (onHeapPoolAccessor.getSize() < 0) {
            return memoryFactory.getSizeInBytes();
        }
        return onHeapPoolAccessor.getSize();
    }

    @Override
    public long getOffHeapSizeInBytes() {
        return 0L;
    }

    @Override
    public long getOnDiskSizeInBytes() {
        if (onDiskPoolAccessor.getSize() < 0) {
            return diskFactory.getOnDiskSizeInBytes();
        }
        return onDiskPoolAccessor.getSize();
    }

}
