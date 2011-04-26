package net.sf.ehcache.store.compound.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.impl.OverflowToDiskPoolableStore;
import net.sf.ehcache.pool.impl.UnboundedPool;
import org.junit.Before;

/**
 * @author Alex Snaps
 */
public class OverflowToDiskStoreTest extends CompoundStoreTest {

    @Before
    public void init() {
        store = OverflowToDiskPoolableStore.create(new Cache(new CacheConfiguration("SomeCache", 1000)), System.getProperty("java.io.tmpdir"), new UnboundedPool(), new UnboundedPool());
        xaStore = OverflowToDiskPoolableStore.create(new Cache(new CacheConfiguration("SomeXaCache", 1000).transactionalMode("xa_strict")), System.getProperty("java.io.tmpdir"), new UnboundedPool(), new UnboundedPool());
    }
}
