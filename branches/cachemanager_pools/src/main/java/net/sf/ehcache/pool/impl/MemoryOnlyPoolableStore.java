package net.sf.ehcache.pool.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolableStore;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.compound.factories.CapacityLimitedInMemoryFactory;
import net.sf.ehcache.store.compound.impl.MemoryOnlyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ludovic Orban
 */
public class MemoryOnlyPoolableStore extends MemoryOnlyStore implements PoolableStore {

    private final Logger LOG = LoggerFactory.getLogger(MemoryOnlyPoolableStore.class);

    private final Cache cache;
    private final PoolAccessor onHeapPoolAccessor;

    private MemoryOnlyPoolableStore(Cache cache, CapacityLimitedInMemoryFactory memory, CacheConfiguration config, Pool onHeapPool) {
        super(memory, config);
        this.cache = cache;
        this.onHeapPoolAccessor = onHeapPool.createPoolAccessor(this);

        addInternalEventListener(new InternalEventListenerAdapter());
    }

    /**
     * Constructs an in-memory store for the given cache, using the given disk path.
     *
     * @param cache cache that fronts this store
     * @param diskStorePath disk path to store data in
     * @return a fully initialized store
     */
    public static MemoryOnlyPoolableStore create(Cache cache, String diskStorePath, Pool onHeapPool) {
        CacheConfiguration config = cache.getCacheConfiguration();
        CapacityLimitedInMemoryFactory memory = new CapacityLimitedInMemoryFactory(null, config.getMaxElementsInMemory(),
                determineEvictionPolicy(config), cache.getCacheEventNotificationService());
        MemoryOnlyPoolableStore store = new MemoryOnlyPoolableStore(cache, memory, config, onHeapPool);
        cache.getCacheConfiguration().addConfigurationListener(store);
        return store;
    }


    private class InternalEventListenerAdapter implements InternalEventListener {

        public void onFault(Object key, Object from, Object to) {
            if (LOG.isDebugEnabled()) { LOG.debug("onFault/ " + from + ", heap: " + onHeapPoolAccessor.getSize()); }
        }

        public void onEvict(Object key, Element evicted) {
            if (LOG.isDebugEnabled()) { LOG.debug("onEvict " + key + ", heap: " + onHeapPoolAccessor.getSize()); }
            onHeapPoolAccessor.delete(evicted.getObjectKey(), evicted.getObjectValue(), evicted);
            if (LOG.isDebugEnabled()) { LOG.debug("/onEvict " + key + ", heap: " + onHeapPoolAccessor.getSize()); }
        }

        public void onUpdate(Object removed, Element newElement) {
            if (LOG.isDebugEnabled()) { LOG.debug("onUpdate " + newElement.getObjectKey() + " | " + removed + ", heap: " + onHeapPoolAccessor.getSize()); }
            onHeapPoolAccessor.delete(newElement.getObjectKey(), newElement.getObjectValue(), newElement);
            if (LOG.isDebugEnabled()) { LOG.debug("/onUpdate " + newElement.getObjectKey() + " | " + removed + ", heap: " + onHeapPoolAccessor.getSize()); }
        }

        public void onRemove(Object removedObject, Element removedElement) {
            if (LOG.isDebugEnabled()) { LOG.debug("onRemove " + removedObject + " | " + removedElement); }
            onHeapPoolAccessor.delete(removedElement.getObjectKey(), removedElement.getObjectValue(), removedElement);
            if (LOG.isDebugEnabled()) { LOG.debug("/onRemove " + removedObject + " | " + removedElement); }
        }

    }


    @Override
    public void dispose() {
        super.dispose();
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
    }

    public boolean evictFromOnHeap(int count, long size) {
        return memoryFactory.evictFromOnHeap(count);
    }

    public boolean evictFromOffHeap(int count, long size) {
        return false;
    }

    public boolean evictFromOnDisk(int count, long size) {
        return false;
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
        return 0L;
    }
}
