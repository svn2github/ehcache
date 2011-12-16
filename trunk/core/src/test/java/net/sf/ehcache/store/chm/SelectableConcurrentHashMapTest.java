package net.sf.ehcache.store.chm;

import net.sf.ehcache.Element;
import net.sf.ehcache.pool.impl.UnboundedPool;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class SelectableConcurrentHashMapTest {

    private SelectableConcurrentHashMap map;

    @Before
    public void setUp() throws Exception {
        map = new SelectableConcurrentHashMap(
            new UnboundedPool().createPoolAccessor(null, null), true, 100, 1, 100, 0, null);

        map.setPinned(0, true);
        map.setPinned(1, true);
        map.put(1, new Element(1, 1));
        map.put(2, new Element(2, 1));
        map.put(3, new Element(3, 1));
        map.setPinned(3, true);
        map.put(4, new Element(4, 1));
    }

    @Test
    public void testReturnsPinnedKeysThatArePresent() {
        assertThat(map.get(1), notNullValue());
        assertThat(map.get(2), notNullValue());
        assertThat(map.get(3), notNullValue());
        assertThat(map.get(4), notNullValue());
    }

    @Test
    public void testDoesNotReturnsPinnedKeysThatAreNotPresent() {
        assertThat(map.get(0), nullValue());
        assertThat(map.get(5), nullValue());
    }

    @Test
    public void testReturnFullSetOfKeys() {
        assertThat(map.keySet().size(), is(4));
        assertThat(map.keySet().contains(0), is(false));
        assertThat(map.keySet().contains(1), is(true));
        assertThat(map.keySet().contains(2), is(true));
        assertThat(map.keySet().contains(3), is(true));
        assertThat(map.keySet().contains(4), is(true));
        assertThat(map.keySet().contains(5), is(false));
    }
}
