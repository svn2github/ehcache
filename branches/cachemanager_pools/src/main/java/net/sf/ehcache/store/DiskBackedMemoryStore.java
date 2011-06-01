package net.sf.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
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
