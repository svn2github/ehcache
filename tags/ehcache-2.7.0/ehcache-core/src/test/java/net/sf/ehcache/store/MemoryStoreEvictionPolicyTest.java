package net.sf.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.impl.UnboundedPool;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class MemoryStoreEvictionPolicyTest {

    @Test
    public void testSetsMemoryEvictionPolicy() {
        final String name = "FAKE!";
        Store store = NotifyingMemoryStore.createNotifyingStore(new Cache(new CacheConfiguration("fakeCache", 100)), new UnboundedPool());
        store.setInMemoryEvictionPolicy(new AbstractPolicy() {
            public String getName() {
                return name;
            }

            public boolean compare(final Element element1, final Element element2) {
                return false;
            }
        });
        final Policy inMemoryEvictionPolicy = store.getInMemoryEvictionPolicy();
        assertThat(inMemoryEvictionPolicy, notNullValue());
        assertThat(inMemoryEvictionPolicy.getName(), equalTo(name));
    }
}
