package net.sf.ehcache;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.RegisteredEventListenersMockHelper;
import net.sf.ehcache.store.Store;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author cschanck
 * @author lorban
 * @author cdennis
 */
public class CacheUnitTest {

    //  Element wishing to be final/lots of final methods, class not final
    //      - discussion about the utility of final methods or final classes or final fields
    //  created specific CacheUnitTest class to focus on unit testing to avoid
    //      disrupting existing test and focus on really unit testing
    //  Mockito sucking and failing silently
    //     - equals/hashcode issues
    //     - cost a bunch of time
    //  Added constructor to Cache, package protected, for unit test purposes only
    //  Loosened scope on the 5 internal methods in RegisteredEventListeners
    //     - allowed us to create a seam class in the test tree
    //     - allowed us to continue testing
    //     - useful that you can widen scope when subclassing
    //  added 9 tests relating to isExpired() behavior in Cache, plus 3 generic put()
    //     tests
    //  Surprising (documented) Cache.put(null) behavior observed
    //  How much of this is useful unit testing and how much is pointless contract testing
    //  Lots of cut and paste repetition, refactoring it might be nice, perhaps not. Design choice.
    //  Highlight differences between these tests and the ones in CacheTest
    //      -- UnitTest.java suffix -- perhaps this can feed to checkshort.
    //      -- Where do we go in terms of checkshort, renaming, integration test, deleting trests
    //  Enormous amount of test code written for small amount of behaviour.
    //  easy next step is to continue adding to CacheUnitTest
    //  another easy step would be to unit test RegisteredEventListener
    //  do we make mocking strategies reusable?
    //  multiple correct answers for most questions above
    //  not checked in yet, made 2 changes to production code base

    @Test
    public void testCacheWhenElementIsExpired() {
        CacheConfiguration conf = mock(CacheConfiguration.class);
        when(conf.isTerracottaClustered()).thenReturn(Boolean.TRUE);

        Element e1 = new Element("foo", "bar") {
            @Override
            public boolean isExpired(CacheConfiguration config) {
                return true;
            }
        };

        Cache cache = new Cache(conf, mock(Store.class), null);
        Assert.assertThat(cache.isExpired(e1), is(true));
    }

    @Test
    public void testCacheWhenElementIsNotExpired() {
        CacheConfiguration conf = mock(CacheConfiguration.class);
        when(conf.isTerracottaClustered()).thenReturn(Boolean.TRUE);

        Element e1 = new Element("foo", "bar") {
            @Override
            public boolean isExpired(CacheConfiguration config) {
                return false;
            }
        };

        Cache cache = new Cache(conf, mock(Store.class), null);
        Assert.assertThat(cache.isExpired(e1), is(false));
    }

    @Test
    public void testGetIsNullWhenElementIsExpired() {
        CacheConfiguration conf = mock(CacheConfiguration.class);
        when(conf.isTerracottaClustered()).thenReturn(Boolean.TRUE);

        Element e1 = new Element("foo", "bar") {
            @Override
            public boolean isExpired(CacheConfiguration config) {
                return true;
            }
        };

        Store store = mock(Store.class);
        when(store.get("foo")).thenReturn(e1);
        Cache cache = new Cache(conf, store, null);
        Element got = cache.get("foo");
        Assert.assertThat(got, nullValue());
    }

    @Test
    public void testGetIsNotNullWhenElementIsNotExpired() {
        CacheConfiguration conf = mock(CacheConfiguration.class);
        when(conf.isTerracottaClustered()).thenReturn(Boolean.TRUE);

        Element e1 = new Element("foo", "bar") {
            @Override
            public boolean isExpired(CacheConfiguration config) {
                return false;
            }
        };

        Store store = mock(Store.class);
        when(store.get("foo")).thenReturn(e1);
        Cache cache = new Cache(conf, store, null);
        Element got = cache.get("foo");
        Assert.assertThat(got, notNullValue());
    }

    @Test
    public void testGetQuietIsNotNullWhenElementIsNotExpired() {
        CacheConfiguration conf = mock(CacheConfiguration.class);
        when(conf.isTerracottaClustered()).thenReturn(Boolean.TRUE);

        Element e1 = new Element("foo", "bar") {
            @Override
            public boolean isExpired(CacheConfiguration config) {
                return false;
            }
        };

        Store store = mock(Store.class);
        when(store.getQuiet("foo")).thenReturn(e1);
        Cache cache = new Cache(conf, store, null);
        Element got = cache.getQuiet("foo");
        Assert.assertThat(got, notNullValue());
    }

    @Test
    public void testGetQuietIsNullWhenElementIsExpired() {
        CacheConfiguration conf = mock(CacheConfiguration.class);
        when(conf.isTerracottaClustered()).thenReturn(Boolean.TRUE);

        Element e1 = new Element("foo", "bar") {
            @Override
            public boolean isExpired(CacheConfiguration config) {
                return true;
            }
        };

        Store store = mock(Store.class);
        when(store.getQuiet("foo")).thenReturn(e1);

        Cache cache = new Cache(conf, store, null);
        Element got = cache.getQuiet("foo");
        Assert.assertThat(got, nullValue());
    }

    @Test
    public void testGetAllIsEmptyWhenElementIsExpired() {
        CacheConfiguration conf = mock(CacheConfiguration.class);
        when(conf.isTerracottaClustered()).thenReturn(Boolean.TRUE);

        Element e1 = new Element("foo", "bar") {
            @Override
            public boolean isExpired(CacheConfiguration config) {
                return true;
            }
        };

        Set<String> keys = Collections.singleton("foo");
        Store store = mock(Store.class);
        when(store.getAll(keys)).thenReturn(Collections.<Object, Element>singletonMap("foo", e1));
        Cache cache = new Cache(conf, store, null);
        Map<Object, Element> got = cache.getAll(keys);
        Assert.assertThat(got.isEmpty(), is(true));
    }

    @Test
    public void testGetAllIsNotEmptyWhenElementIsNotExpired() {
        CacheConfiguration conf = mock(CacheConfiguration.class);
        when(conf.isTerracottaClustered()).thenReturn(Boolean.TRUE);

        Element e1 = new Element("foo", "bar") {
            @Override
            public boolean isExpired(CacheConfiguration config) {
                return false;
            }
        };

        Set<String> keys = Collections.singleton("foo");
        Store store = mock(Store.class);
        when(store.getAll(keys)).thenReturn(Collections.<Object, Element>singletonMap("foo", e1));
        Cache cache = new Cache(conf, store, null);
        Map<Object, Element> got = cache.getAll(keys);
        Assert.assertThat(got.size(), is(1));
        Assert.assertThat(got, hasKey("foo"));
        Assert.assertThat(got, hasValue(e1));
    }

    @Test
    public void testGetAllWhenElementExpiryIsMixed() {
        CacheConfiguration conf = mock(CacheConfiguration.class);
        when(conf.isTerracottaClustered()).thenReturn(Boolean.TRUE);

        final Element e1 = new Element("foo1", "bar") {
            @Override
            public boolean isExpired(CacheConfiguration config) {
                return true;
            }
        };

        final Element e2 = new Element("foo2", "bar") {
            @Override
            public boolean isExpired(CacheConfiguration config) {
                return false;
            }
        };

        Store store = mock(Store.class);
        List<String> keys = Arrays.asList("foo1", "foo2");
        HashMap<Object, Element> returnMap = new HashMap<Object, Element>() {
            {
                put("foo1", e1);
                put("foo2", e2);
            }
        };
        when(store.getAll(keys)).thenReturn(returnMap);

        Cache cache = new Cache(conf, store, null);
        Map<Object, Element> got = cache.getAll(keys);
        Assert.assertThat(got.size(), is(1));
        Assert.assertThat(got, hasEntry((Object)"foo2", e2));
    }

    @Test
    public void testPutElementPutsToStore() {
        CacheConfiguration conf = mock(CacheConfiguration.class);
        when(conf.isTerracottaClustered()).thenReturn(Boolean.TRUE);

        Element e1 = new Element("foo", "bar") {
            @Override
            public boolean isExpired(CacheConfiguration config) {
                return false;
            }
        };

        RegisteredEventListenersMockHelper listener = mock(RegisteredEventListenersMockHelper.class);
        Store store = mock(Store.class);
        Cache cache = new Cache(conf, store, listener);
        cache.put(e1);
        verify(store, times(1)).put(e1);
    }

    @Test
    public void testPutNullElementIsNOOP() {
        CacheConfiguration conf = mock(CacheConfiguration.class);
        when(conf.isTerracottaClustered()).thenReturn(Boolean.TRUE);

        RegisteredEventListenersMockHelper listener = mock(RegisteredEventListenersMockHelper.class);
        Store store = mock(Store.class);
        Cache cache = new Cache(conf, store, listener);

        cache.put(null);
        verify(store, times(0)).put(any(Element.class));
    }

    @Test
    public void testPutElementKeyIsNullisNOOP() {
        CacheConfiguration conf = mock(CacheConfiguration.class);
        when(conf.isTerracottaClustered()).thenReturn(Boolean.TRUE);

        RegisteredEventListenersMockHelper listener = mock(RegisteredEventListenersMockHelper.class);
        Store store = mock(Store.class);
        Cache cache = new Cache(conf, store, listener);

        Element e1 = new Element(null, "bar");
        cache.put(e1);
        verify(store, times(0)).put(any(Element.class));
    }

}
