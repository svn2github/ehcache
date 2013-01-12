package net.sf.ehcache.store.chm;

import net.sf.ehcache.Element;
import net.sf.ehcache.pool.impl.UnboundedPool;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
            new UnboundedPool().createPoolAccessor(null, null), 100, 1, 100, 0, null);

        map.put(1, new Element(1, 1), 0);
        map.put(2, new Element(2, 1), 0);
        map.put(3, new Element(3, 1), 0);
        map.put(4, new Element(4, 1), 0);
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
        map.put(5, new Element(5, 5), 0);
        assertThat(map.keySet().contains(5), is(true));

        Set<Integer> expectedKeySet = expectedSet(1, 2, 3, 4, 5);
        for (Object o : map.keySet()) {
            assertThat(expectedKeySet.remove(o), is(true));
        }
        assertThat(expectedKeySet.isEmpty(), is(true));
    }

    @Test
    public void testClockEvictionHonorsMaxSize() {
        final int maximumSize = 1000;
        map = new SelectableConcurrentHashMap(
            new UnboundedPool().createPoolAccessor(null, null), 100, 1, 100, maximumSize, null);
        for (int i = 0; i < maximumSize * 100; i++) {
            map.put(i, new Element(i, "valueof " + i), 0);
            assertThat("At iteration #" + i + ", the size is " + map.quickSize(), map.quickSize() <= maximumSize, is(true));
        }
    }

    private <T> Set<T> expectedSet(T... values) {
        final Set<T> set = new HashSet<T>();
        Collections.addAll(set, values);
        return set;
    }
}
