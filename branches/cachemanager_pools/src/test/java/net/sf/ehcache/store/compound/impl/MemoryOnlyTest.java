package net.sf.ehcache.store.compound.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.impl.MemoryOnlyPoolableStore;
import net.sf.ehcache.pool.impl.UnboundedPool;
import org.junit.Before;

/**
 * @author Alex Snaps
 */
public class MemoryOnlyTest extends CompoundStoreTest {

    @Before
    public void init() {
        store = MemoryOnlyPoolableStore.create(new Cache(new CacheConfiguration("SomeCache", 1000)), null, new UnboundedPool());
        xaStore = MemoryOnlyPoolableStore.create(new Cache(new CacheConfiguration("SomeXaCache", 1000).transactionalMode("xa_strict")), null, new UnboundedPool());
    }
}
