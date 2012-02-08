package net.sf.ehcache.store;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 */
public class CacheKeySetTest {
    
    Collection<Integer>[]  keySets = new Collection[] { new HashSet<Integer>() {{ add(1); add(2); add(3); }},
        new HashSet<Integer>() {{ add(1); add(4); add(5); }},
        new HashSet<Integer>() {{ add(1); add(4); add(6); }} };
    CacheKeySet<Integer> keySet;
    
    @Before
    public void setup() {
        keySet = new CacheKeySet<Integer>( keySets );
    }
    
    @Test
    public void testIteratesOverAllElementsOnce() {
        Set<Integer> keys = new HashSet<Integer>();
        for (Collection<Integer> set : keySets) {
            keys.addAll(set);
        }
        assertThat(keys.size(), is(6));
        for (Integer integer : keySet) {
            keys.remove(integer);
        }
        assertThat(keys.isEmpty(), is(true));
    }
    
    @Test
    public void testSizeSumsAllCollections() {
        assertThat(keySet.size(), is(9));
    }
    
    @Test
    public void testIsEmptyAccountsForAllKeySets() {
        assertThat(keySet.isEmpty(), is(false));
        assertThat(new CacheKeySet(new HashSet()).isEmpty(), is(true));
        assertThat(new CacheKeySet(new HashSet(), new HashSet()).isEmpty(), is(true));
        assertThat(new CacheKeySet(new HashSet(), new HashSet(), new HashSet()).isEmpty(), is(true));
        assertThat(new CacheKeySet(new HashSet(), new HashSet(), new HashSet() {{ add(1); }}).isEmpty(), is(false));
        assertThat(new CacheKeySet(new HashSet(), new HashSet() {{ add(1); }}, new HashSet() {{ add(1); }}).isEmpty(), is(false));
        assertThat(new CacheKeySet(new HashSet(), new HashSet() {{ add(1); }}, new HashSet()).isEmpty(), is(false));
        assertThat(new CacheKeySet(new HashSet() {{ add(1); }}, new HashSet() {{ add(1); }}, new HashSet()).isEmpty(), is(false));
        assertThat(new CacheKeySet(new HashSet() {{ add(1); }}, new HashSet(), new HashSet()).isEmpty(), is(false));
    }
    
    @Test
    public void testContainsIsSupported() {
        Set<Integer> keys = new HashSet<Integer>(keySet);
        for (Integer key : keys) {
            assertThat(keySet.contains(key), is(true));
        }
    }
    
    @Test
    public void testSupportsEmptyKeySets() {
        final CacheKeySet cacheKeySet = new CacheKeySet();
        assertThat(cacheKeySet.isEmpty(), is(true));
        for (Object o : cacheKeySet) {
            fail("Shouldn't get anything!");
        }
    }
}
