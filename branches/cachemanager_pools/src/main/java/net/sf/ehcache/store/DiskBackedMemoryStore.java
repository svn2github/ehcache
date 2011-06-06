package net.sf.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;
import net.sf.ehcache.store.disk.DiskStore;

/**
 * @author Ludovic Orban
 */
public class DiskBackedMemoryStore extends FrontEndCacheTier<MemoryStore, DiskStore> {

    private final boolean copyOnRead;
    private final boolean copyOnWrite;
    private final ReadWriteCopyStrategy<Element> copyStrategy;

    private final boolean alwaysPutOnHeap;

    public DiskBackedMemoryStore(CacheConfiguration cacheConfiguration, MemoryStore cache, DiskStore authority) {
        super(cache, authority);
        this.alwaysPutOnHeap = getAdvancedBooleanConfigProperty("alwaysPutOnHeap", cacheConfiguration.getName(), false);

        this.copyOnRead = cacheConfiguration.isCopyOnRead();
        this.copyOnWrite = cacheConfiguration.isCopyOnWrite();
        this.copyStrategy = cacheConfiguration.getCopyStrategy();
    }

    private static boolean getAdvancedBooleanConfigProperty(String property, String cacheName, boolean defaultValue) {
        String globalPropertyKey = "net.sf.ehcache.store.config." + property;
        String cachePropertyKey = "net.sf.ehcache.store." + cacheName + ".config." + property;
        return Boolean.parseBoolean(System.getProperty(cachePropertyKey, System.getProperty(globalPropertyKey, Boolean.toString(defaultValue))));
    }

    public static Store create(Ehcache cache, String diskStorePath, Pool onHeapPool, Pool onDiskPool) {
        final MemoryStore memoryStore = createMemoryStore(cache, onHeapPool);
        DiskStore diskStore = createDiskStore(cache, diskStorePath, onHeapPool, onDiskPool);

        cache.getCacheEventNotificationService().registerListener(new CacheEventListener() {
            @Override
            public Object clone() throws CloneNotSupportedException {
                return super.clone();
            }
            public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
            }
            public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
            }
            public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
            }
            public void notifyElementExpired(Ehcache cache, Element element) {
            }
            public void notifyElementEvicted(Ehcache cache, Element element) {
                // if an element can't be serialized by the disk store, it gets evicted
                // and we must propagate the removal to the memory store
                memoryStore.remove(element.getObjectKey());
            }
            public void notifyRemoveAll(Ehcache cache) {
            }
            public void dispose() {
            }
        });

        return new DiskBackedMemoryStore(cache.getCacheConfiguration(), memoryStore, diskStore);
    }

    private static MemoryStore createMemoryStore(Ehcache cache, Pool onHeapPool) {
        return MemoryStore.create(cache, onHeapPool);
    }

    private static DiskStore createDiskStore(Ehcache cache, String diskPath, Pool onHeapPool, Pool onDiskPool) {
        CacheConfiguration config = cache.getCacheConfiguration();
        if (config.isDiskPersistent() || config.isOverflowToDisk()) {
            return DiskStore.create(cache, diskPath, onHeapPool, onDiskPool);
        } else {
            throw new CacheException("DiskBackedMemoryStore can only be used when cache overflows to disk or is disk persistent");
        }
    }

    @Override
    protected Element copyElementForReadIfNeeded(Element element) {
        if (copyOnRead && copyOnWrite) {
            return copyStrategy.copyForRead(element);
        } else if (copyOnRead) {
            return copyStrategy.copyForRead(copyStrategy.copyForWrite(element));
        } else {
            return element;
        }
    }

    @Override
    protected Element copyElementForWriteIfNeeded(Element element) {
        if (copyOnRead && copyOnWrite) {
            return copyStrategy.copyForWrite(element);
        } else if (copyOnWrite) {
            return copyStrategy.copyForRead(copyStrategy.copyForWrite(element));
        } else {
            return element;
        }
    }

    @Override
    protected boolean isCacheFull() {
        return !alwaysPutOnHeap && cache.isFull();
    }

    @Override
    public void readLock(Object key) {
        authority.readLock(key);
    }

    @Override
    public void readUnlock(Object key) {
        authority.readUnlock(key);
    }

    @Override
    public void writeLock(Object key) {
        authority.writeLock(key);
    }

    @Override
    public void writeUnlock(Object key) {
        authority.writeUnlock(key);
    }

    @Override
    public void readLock() {
        authority.readLock();
    }

    @Override
    public void readUnlock() {
        authority.readUnlock();
    }

    @Override
    public void writeLock() {
        authority.writeLock();
    }

    @Override
    public void writeUnlock() {
        authority.writeUnlock();
    }

    public Status getStatus() {
        //TODO this might be wrong...
        return authority.getStatus();
    }

    public Policy getInMemoryEvictionPolicy() {
        return cache.getInMemoryEvictionPolicy();
    }

    public void setInMemoryEvictionPolicy(Policy policy) {
        cache.setInMemoryEvictionPolicy(policy);
    }

    public Object getInternalContext() {
        return authority.getInternalContext();
    }

    public Object getMBean() {
        return authority.getMBean();
    }
}
