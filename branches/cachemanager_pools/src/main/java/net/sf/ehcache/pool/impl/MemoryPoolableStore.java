package net.sf.ehcache.pool.impl;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolableStore;
import net.sf.ehcache.store.MemoryStore;

/**
 * @author Ludovic Orban
 */
public class MemoryPoolableStore extends MemoryStore implements PoolableStore {

    private final PoolAccessor poolAccessor;

    public MemoryPoolableStore(Ehcache cache, Pool pool) {
        super(cache, null);
        this.poolAccessor = pool.createPoolAccessor(this);
    }

    @Override
    public boolean put(Element element) throws CacheException {
        if (poolAccessor.add(element.getObjectKey(), element.getObjectValue(), element) > -1) {
            return super.put(element);
        } else {
            super.remove(element.getObjectKey());
            cache.getCacheEventNotificationService().notifyElementEvicted(element, false);
            return true;
        }
    }

    @Override
    public Element remove(Object key) {
        Element removedElement = super.remove(key);

        if (removedElement != null) {
            poolAccessor.delete(removedElement.getObjectKey(), removedElement.getObjectValue(), removedElement);
        }

        return removedElement;
    }

    public boolean evictFromOnHeap(int count, long size) {
        for (int i = 0; i < count; i++) {
            Element[] elements = sampleElements(map.size());
            Element selected = policy.selectedBasedOnPolicy(elements, null);
            if (selected == null) {
                return false;
            }
            evict(selected);
            remove(selected.getObjectKey());
        }
        return true;
    }

    public boolean evictFromOffHeap(int count, long size) {
        return false;
    }

    public boolean evictFromOnDisk(int count, long size) {
        return false;
    }

    @Override
    public long getInMemorySizeInBytes() {
        return poolAccessor.getSize();
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
