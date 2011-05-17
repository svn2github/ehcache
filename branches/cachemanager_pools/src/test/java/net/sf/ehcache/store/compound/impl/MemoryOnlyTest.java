package net.sf.ehcache.store.compound.impl;

import junit.framework.Assert;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.impl.MemoryOnlyPoolableStore;
import net.sf.ehcache.pool.impl.UnboundedPool;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * @author Alex Snaps
 */
public class MemoryOnlyTest extends CompoundStoreTest {

    @Before
    public void init() {
        store = MemoryOnlyPoolableStore.create(new Cache(new CacheConfiguration("SomeCache", 1000)), null, new UnboundedPool());
        xaStore = MemoryOnlyPoolableStore.create(new Cache(new CacheConfiguration("SomeXaCache", 1000).transactionalMode("xa_strict")), null, new UnboundedPool());
    }

    @Test
    public void testPinning() {
        for (int i = 0; i < 2000; i++) {
            Element element = new Element("Ku-" + i, "@" + i);
            store.put(element);
        }

        Assert.assertEquals(1000, store.getSize());

        for (int i = 0; i < 2000; i++) {
            Element element = new Element("Kp-" + i, "#" + i);
            element.setPinned(true);
            store.put(element);
        }

        Assert.assertTrue(2000 <= store.getSize());

        for (int i = 0; i < 2000; i++) {
            Assert.assertTrue(store.containsKey("Kp-" + i));
        }
    }

}
