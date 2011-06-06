package net.sf.ehcache.store;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;

/**
 * @author Ludovic Orban
 */
public class MemoryOnlyStore extends FrontEndCacheTier<NullStore, MemoryStore> {

    private final boolean copyOnRead;
    private final boolean copyOnWrite;
    private final ReadWriteCopyStrategy<Element> copyStrategy;

    public MemoryOnlyStore(CacheConfiguration cacheConfiguration, NullStore cache, MemoryStore authority) {
        super(cache, authority);

        this.copyOnRead = cacheConfiguration.isCopyOnRead();
        this.copyOnWrite = cacheConfiguration.isCopyOnWrite();
        this.copyStrategy = cacheConfiguration.getCopyStrategy();
    }

    public static Store create(Ehcache cache, Pool onHeapPool) {
        final NullStore nullStore = NullStore.create();
        final MemoryStore memoryStore = createMemoryStore(cache, onHeapPool);
        return new MemoryOnlyStore(cache.getCacheConfiguration(), nullStore, memoryStore);
    }

    private static MemoryStore createMemoryStore(Ehcache cache, Pool onHeapPool) {
        return MemoryStore.create(cache, onHeapPool);
    }

    @Override
    protected boolean isAuthorityHandlingPinnedElements() {
        return true;
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
