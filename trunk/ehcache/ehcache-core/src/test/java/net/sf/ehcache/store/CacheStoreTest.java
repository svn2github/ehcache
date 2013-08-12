package net.sf.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.cachingtier.CountBasedBackEnd;
import net.sf.ehcache.store.cachingtier.OnHeapCachingTier;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.writer.CacheWriterManager;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * When I wrote this, it probably made sense... now though, looking at it... wtf ?!
 * @author Alex Snaps
 */
public class CacheStoreTest {

    public static final String KEY = "I'm with ";
    private static final String NAME = "idiot";

    @Test
    public void testGetFaultsIntoHeapFlushesOnEviction() {
        final AtomicInteger faults = new AtomicInteger();
        final AtomicInteger flushes = new AtomicInteger();
        final Store memStore = createMemStore(10);
        CachingTier<Object, Element> cachingTier = new OnHeapCachingTier<Object, Element>(new CountBasedBackEnd<Object, Object>(1));
        CacheStore cacheStore = new CacheStore(cachingTier, new DelegatingStoreAuthority(memStore,
            new DelegatingStoreAuthority.FaultAction() {
            @Override
            public Element fault(final Object key, final boolean updateStats) {
                faults.getAndIncrement();
                return memStore.get(key);
            }
        }, new DelegatingStoreAuthority.FlushAction() {
            @Override
            public boolean flush(final Element element) {
                flushes.getAndIncrement();
                return true;
            }
        }));

        assertThat(cachingTier.get(KEY, new Callable<Element>() {
            @Override
            public Element call() throws Exception {
                return null;
            }
        }, false), nullValue());
        assertThat(cacheStore.putIfAbsent(new Element(KEY, NAME)), nullValue());
        assertThat(memStore.containsKey(KEY), is(true));
        assertThat(cachingTier.get(KEY, new Callable<Element>() {
            @Override
            public Element call() throws Exception {
                return null;
            }
        }, false), nullValue());

        Element element = cacheStore.get(KEY);
        assertThat(element, notNullValue());
        assertThat(element.getObjectValue(), equalTo((Object)NAME));
        assertThat(faults.get(), is(1));
        assertThat(flushes.get(), is(0));
        assertThat(cachingTier.get(KEY, new Callable<Element>() {
            @Override
            public Element call() throws Exception {
                return null;
            }
        }, false), notNullValue());

        assertThat(cacheStore.put(new Element(KEY + KEY, NAME)), is(true));
        assertThat(faults.get(), is(1));
        assertThat(flushes.get(), is(0));

        element = cacheStore.get(KEY + KEY);
        assertThat(flushes.get(), is(1));
        assertThat(cachingTier.get(KEY, new Callable<Element>() {
            @Override
            public Element call() throws Exception {
                return null;
            }
        }, false), nullValue());
        assertThat(element, notNullValue());
        assertThat(element.getObjectValue(), equalTo((Object)NAME));
        assertThat(faults.get(), is(2));
        assertThat(flushes.get(), is(2));
    }

    private Store createMemStore(final int maxElements) {
        CacheManager manager = new CacheManager(new Configuration().name("CacheStoreTest"));
        Cache cache = new Cache(new CacheConfiguration().maxEntriesLocalHeap(maxElements).name(NAME));
        manager.addCache(cache);
        try {
            final Field compoundStore = Cache.class.getDeclaredField("compoundStore");
            compoundStore.setAccessible(true);
            return (Store) compoundStore.get(cache);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

class DelegatingStoreAuthority implements AuthoritativeTier {

    private final Store delegate;
    private final FaultAction faultAction;
    private final FlushAction flushAction;

    DelegatingStoreAuthority(final Store delegate, final FaultAction faultAction, final FlushAction flushAction) {
        this.delegate = delegate;
        this.faultAction = faultAction;
        this.flushAction = flushAction;
    }

    @Override
    public Element fault(final Object key, final boolean updateStats) {
        return faultAction.fault(key, updateStats);
    }

    @Override
    public boolean putFaulted(final Element element) {
        return delegate.put(element);
    }

    @Override
    public boolean flush(final Element element) {
        return flushAction.flush(element);
    }

    @Override
    public void addStoreListener(final StoreListener listener) {
        delegate.addStoreListener(listener);
    }

    @Override
    public void removeStoreListener(final StoreListener listener) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public boolean put(final Element element) throws CacheException {
        return delegate.put(element);
    }

    @Override
    public void putAll(final Collection<Element> elements) throws CacheException {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public boolean putWithWriter(final Element element, final CacheWriterManager writerManager) throws CacheException {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public Element get(final Object key) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public Element getQuiet(final Object key) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public List getKeys() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public Element remove(final Object key) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public void removeAll(final Collection<?> keys) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public Element removeWithWriter(final Object key, final CacheWriterManager writerManager) throws CacheException {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public void removeAll() throws CacheException {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public Element putIfAbsent(final Element element) throws NullPointerException {
        return delegate.putIfAbsent(element);
    }

    @Override
    public Element removeElement(final Element element, final ElementValueComparator comparator) throws NullPointerException {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public boolean replace(final Element old, final Element element, final ElementValueComparator comparator) throws NullPointerException, IllegalArgumentException {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public Element replace(final Element element) throws NullPointerException {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public void dispose() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public int getSize() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public int getInMemorySize() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public int getOffHeapSize() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public int getOnDiskSize() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public int getTerracottaClusteredSize() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public long getInMemorySizeInBytes() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public long getOffHeapSizeInBytes() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public long getOnDiskSizeInBytes() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public boolean hasAbortedSizeOf() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public Status getStatus() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public boolean containsKey(final Object key) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public boolean containsKeyOnDisk(final Object key) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public boolean containsKeyOffHeap(final Object key) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public boolean containsKeyInMemory(final Object key) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public void expireElements() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public void flush() throws IOException {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public boolean bufferFull() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public Policy getInMemoryEvictionPolicy() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public void setInMemoryEvictionPolicy(final Policy policy) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public Object getInternalContext() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public boolean isCacheCoherent() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public boolean isClusterCoherent() throws TerracottaNotRunningException {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public boolean isNodeCoherent() throws TerracottaNotRunningException {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public void setNodeCoherent(final boolean coherent) throws UnsupportedOperationException, TerracottaNotRunningException {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public void waitUntilClusterCoherent() throws UnsupportedOperationException, TerracottaNotRunningException, InterruptedException {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public Object getMBean() {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public void setAttributeExtractors(final Map<String, AttributeExtractor> extractors) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public Results executeQuery(final StoreQuery query) throws SearchException {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public <T> Attribute<T> getSearchAttribute(final String attributeName) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }
    
    @Override
    public Set<Attribute> getSearchAttributes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Object, Element> getAllQuiet(final Collection<?> keys) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public Map<Object, Element> getAll(final Collection<?> keys) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    @Override
    public void recalculateSize(final Object key) {
        throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
    }

    interface FaultAction {
        Element fault(final Object key, final boolean updateStats);
    }
    interface FlushAction {
        boolean flush(final Element element);
    }
}
