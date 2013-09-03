package net.sf.ehcache.event;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.store.chm.SelectableConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

/**
 * OrderedEventListenerForMemoryStoreBackendTest
 */
public class OrderedEventListenerForMemoryStoreBackendTest {

    private SelectableConcurrentHashMap map;
    private InternalCacheEventListener listener;

    @Before
    public void setUp() {
        RegisteredEventListeners registeredEventListeners = new RegisteredEventListeners(mock(Cache.class));
        listener = mock(InternalCacheEventListener.class);
        registeredEventListeners.registerOrderedListener(listener);
        map = new SelectableConcurrentHashMap(mock(PoolAccessor.class), 10, 100, registeredEventListeners);
    }

    @Test
    public void putTest() {
        String key = "putKey";
        Element element = new Element(key, "value", 0);
        map.put(key, element, 0);
        verify(listener).notifyElementPut(any(Ehcache.class), eq(element));
    }

    @Test
    public void putAndReplaceTest() {
        String key = "putAndReplaceKey";

        Element element = new Element(key, "value", 0);
        map.put(key, element, 0);

        Element newElement = new Element(key, "value2", 0);
        map.put(key, newElement, 0);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).notifyElementRemoved(any(Ehcache.class), eq(element));
        inOrder.verify(listener).notifyElementPut(any(Ehcache.class), eq(newElement));
    }

    @Test
    public void putIfAbsentTest() {
        String key = "putIfAbsentKey";
        Element element = new Element(key, "someValue", 0);
        map.putIfAbsent(key, element, 0);
        verify(listener).notifyElementPut(any(Ehcache.class), eq(element));
        reset(listener);

        map.putIfAbsent(key, new Element(key, "otherValue", 0), 0);
        verifyZeroInteractions(listener);
    }

    @Test
    public void removeTest() {
        String key = "removeKey";
        Element element = new Element(key, "value", 0);
        map.put(key, element, 0);
        map.remove(key);
        verify(listener).notifyElementRemoved(any(Ehcache.class), eq(element));
        reset(listener);

        map.put(key, element, 0);
        map.remove(key, element);
        verify(listener).notifyElementRemoved(any(Ehcache.class), eq(element));

    }
}
