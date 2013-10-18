package org.terracotta.modules.ehcache;

import net.sf.ehcache.util.concurrent.ConcurrentHashMap;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Eugene Shelestovich
 */
public class WanAwareToolkitCacheTest {

  private WanAwareToolkitCache<String, String> cache;
  private ToolkitCacheInternal<String, String> delegate;

  @Before
  public void setUp() {
    delegate = mock(ToolkitCacheInternal.class);
    final ToolkitMap<String, Serializable> configMap = new MockToolkitMap<String, Serializable>();
    cache = new WanAwareToolkitCache<String, String>(delegate, configMap);
  }

  @Test
  public void testMustCallDelegateIfActiveAndThrowISEOtherwise() {
    cache.activate();
    cache.put("k1", "v1");
    verify(delegate).put("k1", "v1");

    cache.deactivate();
    try {
      cache.put("k1", "v1");
      fail("Exception expected, but not thrown");
    } catch (IllegalStateException e) {
      // just as planned
    }
    verifyNoMoreInteractions(delegate);
  }

  @Test
  public void testMustBeInactiveByDefault() {
    try {
      cache.put("k1", "v1");
      fail("Exception expected, but not thrown");
    } catch (IllegalStateException e) {
      // just as planned
    }
    verifyNoMoreInteractions(delegate);
  }

  @Test
  public void testMustDropPutIfAbsentVersionedIfTombstoneExists() {
    cache.removeVersioned("key", 0L);
    verify(delegate).removeVersioned("key", 0L);

    cache.putIfAbsentVersioned("key", "value", 1L);
    verify(delegate, never()).putIfAbsentVersioned(anyString(), anyString(), anyLong());
    verifyNoMoreInteractions(delegate);
  }

  @Test
  public void testMustClearTombstoneAfterPutIfAbsentVersionedDropped() {
    cache.removeVersioned("key", 0L);
    verify(delegate).removeVersioned("key", 0L);

    cache.putIfAbsentVersioned("key", "value", 1L);
    cache.putIfAbsentVersioned("key2", "value2", 2L);
    verify(delegate).putIfAbsentVersioned("key2", "value2", 2L);
    verifyNoMoreInteractions(delegate);
  }

  @Test
  public void testMustClearTombstoneOnPutVersioned() {
    cache.removeVersioned("key", 0L);
    verify(delegate).removeVersioned("key", 0L);

    cache.putVersioned("key", "value", 1L);
    verify(delegate).putVersioned("key", "value", 1L);

    cache.putIfAbsentVersioned("key", "value", 1L);
    verify(delegate).putIfAbsentVersioned("key", "value", 1L);
    verifyNoMoreInteractions(delegate);
  }

  private static final class MockToolkitMap<K, V> implements ToolkitMap<K, V> {

    private final ConcurrentMap<K, V> delegate = new ConcurrentHashMap<K, V>();

    public V putIfAbsent(final K key, final V value) {return delegate.putIfAbsent(key, value);}

    @Override
    public boolean remove(final Object key, final Object value) {return delegate.remove(key, value);}

    public boolean replace(final K key, final V oldValue, final V newValue) {
      return delegate.replace(key, oldValue, newValue);
    }

    public V replace(final K key, final V value) {return delegate.replace(key, value);}

    @Override
    public int size() {return delegate.size();}

    @Override
    public boolean isEmpty() {return delegate.isEmpty();}

    @Override
    public boolean containsKey(final Object key) {return delegate.containsKey(key);}

    @Override
    public boolean containsValue(final Object value) {return delegate.containsValue(value);}

    @Override
    public V get(final Object key) {return delegate.get(key);}

    public V put(final K key, final V value) {return delegate.put(key, value);}

    @Override
    public V remove(final Object key) {return delegate.remove(key);}

    public void putAll(final Map<? extends K, ? extends V> m) {delegate.putAll(m);}

    @Override
    public void clear() {delegate.clear();}

    @Override
    public Set<K> keySet() {return delegate.keySet();}

    @Override
    public Collection<V> values() {return delegate.values();}

    @Override
    public Set<Entry<K, V>> entrySet() {return delegate.entrySet();}

    @Override
    public boolean equals(final Object o) {return delegate.equals(o);}

    @Override
    public int hashCode() {return delegate.hashCode();}

    @Override
    public boolean isDestroyed() {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public void destroy() {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public ToolkitReadWriteLock getReadWriteLock() {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public String getName() {
      throw new UnsupportedOperationException("Implement me!");
    }
  }
}
