package net.sf.ehcache.pool.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolableStore;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.compound.factories.DiskPersistentStorageFactory;
import net.sf.ehcache.store.compound.impl.DiskPersistentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * @author Ludovic Orban
 */
public class DiskPersistentPoolableStore extends DiskPersistentStore implements PoolableStore {

    private final Logger LOG = LoggerFactory.getLogger(DiskPersistentPoolableStore.class);

    private final Cache cache;
    private final PoolAccessor onHeapPoolAccessor;
    private final PoolAccessor onDiskPoolAccessor;

    private class InternalEventListenerAdapter implements InternalEventListener {

        public void onFault(Object key, Object from, Object to) {
            if (LOG.isDebugEnabled()) { LOG.debug("onFault/ " + from + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize()); }
        }

        public void onEvict(Object key, Element evicted) {
            if (LOG.isDebugEnabled()) { LOG.debug("onEvict " + key + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize()); }
            onHeapPoolAccessor.delete(evicted.getObjectKey(), evicted.getObjectValue(), evicted);
            onDiskPoolAccessor.delete(evicted.getObjectKey(), evicted.getObjectValue(), evicted);
            if (LOG.isDebugEnabled()) { LOG.debug("/onEvict " + key + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize()); }
        }

        public void onUpdate(Object removed, Element newElement) {
            if (LOG.isDebugEnabled()) { LOG.debug("onUpdate " + newElement.getObjectKey() + " | " + removed + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize()); }
            onHeapPoolAccessor.delete(newElement.getObjectKey(), newElement.getObjectValue(), newElement);
            onDiskPoolAccessor.delete(newElement.getObjectKey(), newElement.getObjectValue(), newElement);
            if (LOG.isDebugEnabled()) { LOG.debug("/onUpdate " + newElement.getObjectKey() + " | " + removed + ", heap: " + onHeapPoolAccessor.getSize() + ", disk: " + onDiskPoolAccessor.getSize()); }
        }

        public void onRemove(Object removedObject, Element removedElement) {
            if (LOG.isDebugEnabled()) { LOG.debug("onRemove " + removedObject + " | " + removedElement); }
            onHeapPoolAccessor.delete(removedElement.getObjectKey(), removedElement.getObjectValue(), removedElement);
            onDiskPoolAccessor.delete(removedElement.getObjectKey(), removedElement.getObjectValue(), removedElement);
            if (LOG.isDebugEnabled()) { LOG.debug("/onRemove " + removedObject + " | " + removedElement); }
        }

    }

    private DiskPersistentPoolableStore(Cache cache, DiskPersistentStorageFactory disk, CacheConfiguration config, Pool onHeapPool, Pool onDiskPool) {
        super(disk, config);
        this.cache = cache;
        this.onHeapPoolAccessor = onHeapPool.createPoolAccessor(this);
        this.onDiskPoolAccessor = onDiskPool.createPoolAccessor(this);

        // todo: re-processing all persisted elements isn't exactly optimal
        if (onDiskPool.getSize() >= 0) {
            // only done when in pooling mode
            // refresh all persisted elements to size them
            Collection keys = getKeys();
            for (Object key : keys) {
                Object value = getQuiet(key);
                put(new Element(key, value));
            }
        }

        addInternalEventListener(new InternalEventListenerAdapter());
    }

    /**
     * Creates a persitent-to-disk store for the given cache, using the given disk path.
     *
     * @param cache cache that fronts this store
     * @param diskStorePath disk path to store data in
     * @return a fully initialized store
     */
    public static DiskPersistentPoolableStore create(Cache cache, String diskStorePath, Pool onHeapPool, Pool onDiskPool) {
        CacheConfiguration config = cache.getCacheConfiguration();
        DiskPersistentStorageFactory disk = new DiskPersistentStorageFactory(cache, diskStorePath, cache.getCacheEventNotificationService());
        DiskPersistentPoolableStore store = new DiskPersistentPoolableStore(cache, disk, config, onHeapPool, onDiskPool);
        cache.getCacheConfiguration().addConfigurationListener(store);
        return store;
    }

    private boolean add(Element element) {
        boolean success = true;
        long onHeapPoolAddedSize = onHeapPoolAccessor.add(element.getObjectKey(), element.getObjectValue(), element);
        if (onHeapPoolAddedSize > -1) {
            long onDiskPoolAddedSize = onDiskPoolAccessor.add(element.getObjectKey(), element.getObjectValue(), element);
            if (onDiskPoolAddedSize < 0) {
                onHeapPoolAccessor.delete(element.getObjectKey(), element.getObjectValue(), element);
                success = false;
            }
        } else {
            success = false;
        }
        return success;
    }

    private void delete(Element element) {
        onDiskPoolAccessor.delete(element.getObjectKey(), element.getObjectValue(), element);
        onHeapPoolAccessor.delete(element.getObjectKey(), element.getObjectValue(), element);
    }

    
    @Override
    public void dispose() {
        super.dispose();
        onDiskPoolAccessor.unlink();
        onHeapPoolAccessor.unlink();
    }

    @Override
    public boolean put(Element element) throws CacheException {
        if (add(element)) {
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
        if (add(element)) {
            Element oldElement = super.putIfAbsent(element);
            if (oldElement != null) {
                delete(element);
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
        if (add(element)) {
            Element oldElement = super.replace(element);
            if (oldElement == null) {
                delete(element);
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
        if (add(element)) {
            boolean replaced = super.replace(old, element, comparator);
            if (!replaced) {
                delete(element);
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

        // if the disk index is corrupt, the superclass ctor will call removeAll() before this
        // constructor had a chance to initialize the pool accessors -> null checks
        if (onHeapPoolAccessor != null) {
            onHeapPoolAccessor.clear();
        }
        if (onDiskPoolAccessor != null) {
            onDiskPoolAccessor.clear();
        }
    }

    public boolean evictFromOnHeap(int count, long size) {
        return disk.evict(count) == count;
    }

    public boolean evictFromOffHeap(int count, long size) {
        return false;
    }

    public boolean evictFromOnDisk(int count, long size) {
        return disk.evict(count) == count;
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
        if (onDiskPoolAccessor.getSize() < 0) {
            return disk.getOnDiskSizeInBytes();
        }
        return onDiskPoolAccessor.getSize();
    }


    @Override
    public boolean isElementOnDisk(Object key) {
        return containsKey(key);
    }

    @Override
    public boolean isElementOnHeap(Object key) {
        return containsKey(key);
    }
}
