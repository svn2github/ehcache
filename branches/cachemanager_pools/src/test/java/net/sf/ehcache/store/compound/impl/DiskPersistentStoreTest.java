package net.sf.ehcache.store.compound.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.impl.DiskPersistentPoolableStore;
import net.sf.ehcache.pool.impl.UnboundedPool;
import org.junit.Before;

/**
 * @author Alex Snaps
 */
public class DiskPersistentStoreTest extends CompoundStoreTest {

    @Before
    public void init() {
        store = DiskPersistentPoolableStore.create(new Cache(new CacheConfiguration("SomeCache", 1000)), System.getProperty("java.io.tmpdir"), new UnboundedPool(), new UnboundedPool());
        xaStore = DiskPersistentPoolableStore.create(new Cache(new CacheConfiguration("SomeXaCache", 1000).transactionalMode("xa_strict")), System.getProperty("java.io.tmpdir"), new UnboundedPool(), new UnboundedPool());
    }
}
