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
            new UnboundedPool().createPoolAccessor(null, null), true, 100, 1, 100, 0, null);

        map.setPinned(0, true);
        map.setPinned(1, true);
        map.put(1, new Element(1, 1), 0);
        map.put(2, new Element(2, 1), 0);
        map.put(3, new Element(3, 1), 0);
        map.setPinned(3, true);
        map.put(4, new Element(4, 1), 0);
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
        map.put(5, new Element(5, 5), 0);
        assertThat(map.keySet().contains(5), is(true));

        Set<Integer> expectedKeySet = expectedSet(1, 2, 3, 4, 5);
        for (Object o : map.keySet()) {
            assertThat(expectedKeySet.remove(o), is(true));
        }
        assertThat(expectedKeySet.isEmpty(), is(true));
    }
    
    @Test
    public void testReturnsPresentPinnedKeySet() {
        SelectableConcurrentHashMap map = new SelectableConcurrentHashMap(
            new UnboundedPool().createPoolAccessor(null, null), true, 100, 1, 100, 0, null);

        map.setPinned(0, true);
        assertThat(map.pinnedSize(), is(0));
        assertThat(map.pinnedKeySet().size(), is(0));
        map.setPinned(1, true);
        assertThat(map.pinnedSize(), is(0));
        assertThat(map.pinnedKeySet().size(), is(0));
        map.put(1, new Element(1, 1), 0);
        assertThat(map.pinnedSize(), is(1));
        assertThat(map.pinnedKeySet().size(), is(1));
        assertThat(map.pinnedKeySet().contains(1), is(true));
        map.put(2, new Element(2, 1), 0);
        assertThat(map.pinnedSize(), is(1));
        assertThat(map.pinnedKeySet().size(), is(1));
        assertThat(map.pinnedKeySet().contains(1), is(true));
        map.put(3, new Element(3, 1), 0);
        assertThat(map.pinnedSize(), is(1));
        assertThat(map.pinnedKeySet().size(), is(1));
        assertThat(map.pinnedKeySet().contains(1), is(true));
        map.setPinned(3, true);
        assertThat(map.pinnedSize(), is(2));
        assertThat(map.pinnedKeySet().size(), is(2));
        assertThat(map.pinnedKeySet().contains(1), is(true));
        assertThat(map.pinnedKeySet().contains(3), is(true));
        map.put(4, new Element(4, 1), 0);
        assertThat(map.pinnedSize(), is(2));
        assertThat(map.pinnedKeySet().size(), is(2));
        assertThat(map.pinnedKeySet().contains(1), is(true));
        assertThat(map.pinnedKeySet().contains(3), is(true));
        assertThat(map.pinnedKeySet().contains(0), is(false));
        assertThat(map.pinnedKeySet().contains(2), is(false));
        map.put(0, new Element(0, 0), 0);
        assertThat(map.pinnedKeySet().contains(0), is(true));
        Set<Integer> expectedKeySet = expectedSet(0, 1, 3);
        for (Object o : map.pinnedKeySet()) {
            assertThat("Failed to remove " + o, expectedKeySet.remove(o), is(true));
        }
        assertThat("Still have " + expectedKeySet, expectedKeySet.isEmpty(), is(true));
    }

    @Test
    public void testTracksDummyPinnedCountProperly() {
        final int size = map.size();
        final int pinnedSize = map.pinnedSize();
        final String key = "removeTest";
        assertThat(map.containsKey(key), is(false));
        map.setPinned(key, true);
        assertThat(map.containsKey(key), is(false));
        assertThat(map.size(), is(size));
        assertThat(map.pinnedSize(), is(pinnedSize));
        map.put(key, new Element(key, "value"), 0);
        assertThat(map.containsKey(key), is(true));
        assertThat(map.size(), is(size + 1));
        assertThat(map.pinnedSize(), is(pinnedSize + 1));
        assertThat(map.containsKey(key), is(true));
        assertThat(map.size(), is(size + 1));
        assertThat(map.pinnedSize(), is(pinnedSize + 1));
        assertThat(map.remove(key), notNullValue());
        assertThat(map.containsKey(key), is(false));
        assertThat(map.size(), is(size));
        assertThat(map.pinnedSize(), is(pinnedSize));
        assertThat(map.remove(key), nullValue());
        assertThat(map.containsKey(key), is(false));
        assertThat(map.size(), is(size));
        assertThat(map.pinnedSize(), is(pinnedSize));
        map.putIfAbsent(key, new Element(key, "value"), 0);
        assertThat(map.containsKey(key), is(true));
        assertThat(map.size(), is(size + 1));
        assertThat(map.pinnedSize(), is(pinnedSize + 1));
        assertThat(map.get("notThere"), nullValue());
        map.setPinned("notThere", false);
        assertThat(map.size(), is(size + 1));
        assertThat(map.pinnedSize(), is(pinnedSize + 1));
        map.setPinned("notThere", false);
        map.setPinned("notThere", false);
        assertThat(map.size(), is(size + 1));
        assertThat(map.pinnedSize(), is(pinnedSize + 1));
        map.setPinned("notThere", true);
        assertThat(map.size(), is(size + 1));
        assertThat(map.pinnedSize(), is(pinnedSize + 1));
    }

    @Test
    public void testUnpinsAll() {
        final int size = map.size();
        final int pinnedSize = map.pinnedSize();
        map.setPinned("unpinAll1", true);
        map.setPinned("unpinAll2", true);
        map.put("unpinAll1", new Element("unpinAll1", "randomValue"), 0);
        assertThat(map.size(), is(size + 1));
        assertThat(map.pinnedSize(), is(pinnedSize + 1));
        map.unpinAll();
        assertThat(map.size(), is(size + 1));
        assertThat(map.pinnedSize(), is(0));
        map.clear();
        assertThat(map.size(), is(0));
        assertThat(map.pinnedSize(), is(0));
        map.clear();
        assertThat(map.size(), is(0));
        assertThat(map.pinnedSize(), is(0));
        map.setPinned("someKey", true);
        map.unpinAll();
        assertThat(map.size(), is(0));
    }
    
    @Test
    public void testClockEvictionHonorsMaxSize() {
        final int maximumSize = 1000;
        map = new SelectableConcurrentHashMap(
            new UnboundedPool().createPoolAccessor(null, null), true, 100, 1, 100, maximumSize, null);
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
