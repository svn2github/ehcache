package net.sf.ehcache.event;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.store.CacheStore;
import net.sf.ehcache.store.DefaultElementValueComparator;
import net.sf.ehcache.store.disk.DiskStorageFactory;
import net.sf.ehcache.store.disk.DiskStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InOrder;
import org.terracotta.test.categories.CheckShorts;

import java.lang.reflect.Field;

/**
 * OrderedEventListenerForDiskStoreBackendTest
 */
@Category(CheckShorts.class)
public class OrderedEventListenerForDiskStoreBackendTest {

    private static CacheManager cacheManager;
    private static DiskStore diskStore;
    private static Cache diskCache;
    private InternalCacheEventListener listener;

    @Before
    public void setUp() throws Exception {
        cacheManager = CacheManager.create(new Configuration().name("diskStoreTest"));
        diskCache = new Cache(new CacheConfiguration().name("disk-event-cache").maxEntriesLocalHeap(10).overflowToDisk(true)
                .eternal(false).timeToLiveSeconds(10).timeToIdleSeconds(10).diskPersistent(false).diskExpiryThreadIntervalSeconds(1)
                .diskSpoolBufferSizeMB(10));
        cacheManager.addCache(diskCache);
        Field compoundStoreField = Cache.class.getDeclaredField("compoundStore");
        compoundStoreField.setAccessible(true);
        CacheStore cacheStore = (CacheStore)compoundStoreField.get(diskCache);
        Field field = CacheStore.class.getDeclaredField("authoritativeTier");
        field.setAccessible(true);
        diskStore = (DiskStore)field.get(cacheStore);

        listener = mock(InternalCacheEventListener.class);
        diskCache.getCacheEventNotificationService().registerOrderedListener(listener);
    }

    @Test
    public void putTest() {
        Element element = new Element("putKey", "value", 0);
        diskStore.put(element);
        verify(listener).notifyElementPut(any(Ehcache.class), eq(element));
    }

    @Test
    public void putAndReplaceTest() {
        String key = "putReplaceKey";
        Element element = new Element(key, "value", 0);
        diskStore.put(element);
        Element otherElement = new Element(key, "otherVal", 0);
        diskStore.put(otherElement);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).notifyElementRemoved(any(Ehcache.class), eq(element));
        inOrder.verify(listener).notifyElementPut(any(Ehcache.class), eq(otherElement));
    }

    @Test
    public void putFaultedTest() {
        Element element = new Element("faultedKey", "value", 0);
        diskStore.putFaulted(element);
        verify(listener).notifyElementPut(any(Ehcache.class), eq(element));
    }

    @Test
    public void putIfAbsentTest() {
        String key = "putIfAbsentKey";
        Element element = new Element(key, "value", 0);
        diskStore.putIfAbsent(element);
        verify(listener).notifyElementPut(any(Ehcache.class), eq(element));
        reset(listener);

        diskStore.putIfAbsent(new Element(key, "otherVal", 0));
        verifyZeroInteractions(listener);

    }

    @Test
    public void replaceTest() {
        String key = "replaceKey";
        Element oldElement = new Element(key, "oldValue", 0);
        diskStore.put(oldElement);
        Element element = new Element(key, "value", 0);
        diskStore.replace(element);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).notifyElementRemoved(any(Ehcache.class), eq(oldElement));
        inOrder.verify(listener).notifyElementPut(any(Ehcache.class), eq(element));
    }

    @Test
    public void compareAndReplaceTest() {
        String key = "casKey";
        Element oldElement = new Element(key, "oldValue", 0);
        diskStore.put(oldElement);
        Element element = new Element(key, "value", 0);
        diskStore.replace(oldElement, element, new DefaultElementValueComparator(diskCache.getCacheConfiguration()));

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).notifyElementRemoved(any(Ehcache.class), eq(oldElement));
        inOrder.verify(listener).notifyElementPut(any(Ehcache.class), eq(element));
    }

    @Test
    public void removeTest() {
        String key = "removeKey";
        Element element = new Element(key, "value", 0);
        diskStore.put(element);

        diskStore.remove(key);
        verify(listener).notifyElementRemoved(any(Ehcache.class), eq(element));
        reset(listener);

        diskStore.put(element);
        diskStore.removeElement(element, new DefaultElementValueComparator(diskCache.getCacheConfiguration()));
        verify(listener).notifyElementRemoved(any(Ehcache.class), eq(element));
    }

    @Test
    public void evictTest() {
        String key = "evictKey";
        Element element = new Element(key, "value", 0);
        diskStore.put(element);

        int maxTries = 100;
        int tryCount = 0;
        while (tryCount < maxTries) {
            if (diskStore.evict(key, (DiskStorageFactory.DiskSubstitute)diskStore.unretrievedGet(key))) {
                verify(listener).notifyElementRemoved(any(Ehcache.class), eq(element));
                return;
            } else {
                tryCount++;
            }
        }
        assertThat(diskStore.containsKey(element.getObjectKey()), is(false));
    }

    @After
    public void tearDown() {
        cacheManager.shutdown();
    }
}
