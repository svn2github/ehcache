/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.event.RegisteredEventListeners;

import org.junit.Assert;
import org.mockito.Mockito;
import org.terracotta.cache.CacheConfig;
import org.terracotta.cache.MutableConfig;
import org.terracotta.cache.TerracottaDistributedCache;
import org.terracotta.cache.TimeSource;
import org.terracotta.cache.TimestampedValue;
import org.terracotta.config.Configuration;
import org.terracotta.locking.ClusteredLock;
import org.terracotta.meta.MetaData;
import org.terracotta.modules.ehcache.coherence.CacheCoherence;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author Abhishek Sanoujam
 */
public class LocalBufferedMapTest extends TestCase {
  private static final int                      SLEEP_TIME        = 3 * LocalBufferedMap.DEFAULT_LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS;
  private LocalBufferedMap<String, String>      map;

  private final CollectingClusteredStoreBackend collectingBackend = new CollectingClusteredStoreBackend();

  public void test() {
    // Calendar cal = Calendar.getInstance();
    // int date = cal.get(Calendar.DAY_OF_MONTH);
    // int month = cal.get(Calendar.MONTH);
    // if (date >= 1 && month >= Calendar.MAY) {
    // throw new AssertionError("Timebomb expired, Uncomment and fix test.");
    // } else {
    // System.out.println("LocalBufferedMapTest is disabled.");
    // System.out.println(SLEEP_TIME + "" + map);
    // if (Boolean.valueOf("true")) return;
    // }
    System.out.println("Running LocalBufferedMapTest");
    reinitMap();
    doTestPutAndGet();
    reinitMap();
    doTestRemove();
    reinitMap();
    doTestMultiplePuts();
    reinitMap();
    doTestRemoveAndPut();
    reinitMap();
    doTestPutAndRemove();
    reinitMap();
    doTestRemoveAndGet();
    reinitMap();
    doTestContainsKey();
    reinitMap();
    doTestClear();
    reinitMap();
    doTestGetKeys();
    System.out.println("Done.");
  }

  private void reinitMap() {
    if (map != null) {
      map.dispose();
    }
    map = new UnclusteredLocalBufferedMap<String, String>(collectingBackend, Mockito.mock(CacheCoherence.class),
                                                          Mockito.mock(ValueModeHandler.class));
    map.startThreadIfNecessary();
    collectingBackend.reset();
  }

  private void doTestPutAndGet() {
    System.out.println("... testing put and get");
    map.put("key", "value", null);
    // does not put in the backend as not started yet
    Assert.assertEquals("value", map.internalGetFromCollectingMap("key").getValue());
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));

    map.startBuffering();
    reallySleep(SLEEP_TIME * 3);
    // the put should have went to backend now
    Assert.assertEquals(null, map.internalGetFromCollectingMap("key"));
    Assert.assertEquals("value", collectingBackend.unlockedGet("key"));
  }

  private void doTestRemove() {
    System.out.println("... testing remove");
    String removed = map.remove("key", null);
    Assert.assertEquals(null, removed);
    Assert.assertEquals(true, map.internalGetFromCollectingMap("key").isRemove());

    // not propagated to backend yet, start the flush thread
    map.startBuffering();
    reallySleep(SLEEP_TIME);
    Assert.assertTrue(collectingBackend.removeCalled("key"));
  }

  private void doTestMultiplePuts() {
    System.out.println("... testing multiple puts");
    map.put("key", "value", null);
    // does not put in the backend as not started yet
    Assert.assertEquals("value", map.internalGetFromCollectingMap("key").getValue());
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));
    map.put("key", "value1", null);
    // does not put in the backend as not started yet
    Assert.assertEquals("value1", map.internalGetFromCollectingMap("key").getValue());
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));
    map.put("key", "value2", null);
    // does not put in the backend as not started yet
    Assert.assertEquals("value2", map.internalGetFromCollectingMap("key").getValue());
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));
    map.put("key", "value3", null);
    // does not put in the backend as not started yet
    Assert.assertEquals("value3", map.internalGetFromCollectingMap("key").getValue());
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));

    map.startBuffering();
    reallySleep(SLEEP_TIME);
    // the put should have went to backend now, assert last put wins
    Assert.assertEquals(null, map.internalGetFromCollectingMap("key"));
    Assert.assertEquals("value3", collectingBackend.unlockedGet("key"));
  }

  private void doTestRemoveAndPut() {
    System.out.println("... testing remove and put");
    String removed = map.remove("key", null);
    Assert.assertEquals(null, removed);
    Assert.assertEquals(true, map.internalGetFromCollectingMap("key").isRemove());

    removed = map.remove("key", null);
    Assert.assertEquals(null, removed);
    Assert.assertEquals(true, map.internalGetFromCollectingMap("key").isRemove());

    map.put("key", "value", null);
    removed = map.remove("key", null);
    Assert.assertEquals("value", removed);
    Assert.assertEquals(true, map.internalGetFromCollectingMap("key").isRemove());

    removed = map.remove("key", null);
    Assert.assertEquals(null, removed);
    Assert.assertEquals(true, map.internalGetFromCollectingMap("key").isRemove());

    map.put("key", "value1", null);
    Assert.assertEquals("value1", map.internalGetFromCollectingMap("key").getValue());

    // not propagated to backend yet, start the flush thread
    map.startBuffering();
    reallySleep(SLEEP_TIME);
    Assert.assertEquals("value1", collectingBackend.unlockedGet("key"));
    Assert.assertFalse(collectingBackend.removeCalled("key"));
  }

  private void doTestPutAndRemove() {
    System.out.println("... testing puts and remove");
    map.put("key", "value", null);
    // does not put in the backend as not started yet
    Assert.assertEquals("value", map.internalGetFromCollectingMap("key").getValue());
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));
    map.put("key", "value1", null);
    // does not put in the backend as not started yet
    Assert.assertEquals("value1", map.internalGetFromCollectingMap("key").getValue());
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));
    map.put("key", "value2", null);
    // does not put in the backend as not started yet
    Assert.assertEquals("value2", map.internalGetFromCollectingMap("key").getValue());
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));
    map.put("key", "value3", null);
    // does not put in the backend as not started yet
    Assert.assertEquals("value3", map.internalGetFromCollectingMap("key").getValue());
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));

    String removed = map.remove("key", null);
    Assert.assertEquals("value3", removed);
    Assert.assertEquals(true, map.internalGetFromCollectingMap("key").isRemove());
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));

    // start the flush thread, propagate to backend
    map.startBuffering();
    reallySleep(SLEEP_TIME);
    Assert.assertEquals(null, map.internalGetFromCollectingMap("key"));
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));
    Assert.assertTrue(collectingBackend.removeCalled("key"));
  }

  private void doTestRemoveAndGet() {
    System.out.println("... testing remove and get");
    map.put("key", "value", null);
    // does not put in the backend as not started yet
    Assert.assertEquals("value", map.internalGetFromCollectingMap("key").getValue());
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));

    String get = map.get("key");
    // does not put in the backend as not started yet
    Assert.assertEquals("value", get);
    Assert.assertEquals("value", map.internalGetFromCollectingMap("key").getValue());
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));

    String removed = map.remove("key", null);
    get = map.get("key");
    Assert.assertEquals("value", removed);
    Assert.assertEquals(null, get);
    Assert.assertEquals(true, map.internalGetFromCollectingMap("key").isRemove());
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));

    // start the flush thread, propagate to backend
    map.startBuffering();
    reallySleep(SLEEP_TIME);
    get = map.get("key");
    Assert.assertEquals(null, get);
    Assert.assertEquals(null, map.internalGetFromCollectingMap("key"));
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));
    Assert.assertTrue(collectingBackend.removeCalled("key"));
  }

  private void doTestContainsKey() {
    System.out.println("... testing containsKey");
    map.put("key", "value", null);
    // does not put in the backend as not started yet
    Assert.assertEquals("value", map.internalGetFromCollectingMap("key").getValue());
    Assert.assertTrue(map.containsKey("key"));
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));

    map.put("key", "value1", null);
    // does not put in the backend as not started yet
    Assert.assertEquals("value1", map.internalGetFromCollectingMap("key").getValue());
    Assert.assertTrue(map.containsKey("key"));
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));

    String removed = map.remove("key", null);
    Assert.assertEquals("value1", removed);
    Assert.assertFalse(map.containsKey("key"));
    Assert.assertEquals(true, map.internalGetFromCollectingMap("key").isRemove());
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));

    // start the flush thread, propagate to backend
    map.startBuffering();
    reallySleep(SLEEP_TIME);
    Assert.assertFalse(map.containsKey("key"));
    Assert.assertEquals(null, map.internalGetFromCollectingMap("key"));
    Assert.assertEquals(null, collectingBackend.unlockedGet("key"));
    Assert.assertTrue(collectingBackend.removeCalled("key"));
  }

  private void doTestClear() {
    System.out.println("... testing clear");
    for (int i = 0; i < 100; i++) {
      // does not put in the backend as not started yet
      map.put("key" + i, "value" + i, null);
    }
    for (int i = 0; i < 100; i++) {
      Assert.assertEquals("value" + i, map.internalGetFromCollectingMap("key" + i).getValue());
      Assert.assertTrue(map.containsKey("key" + i));
      Assert.assertEquals(null, collectingBackend.unlockedGet("key" + i));
    }
    Assert.assertFalse(collectingBackend.clearCalled());
    map.clear(null);

    map.startBuffering();
    reallySleep(SLEEP_TIME);
    Assert.assertTrue(collectingBackend.clearCalled());
    Assert.assertEquals(0, collectingBackend.backendMap.size());
    for (int i = 0; i < 100; i++) {
      Assert.assertEquals(null, map.internalGetFromCollectingMap("key" + i));
      Assert.assertFalse(map.containsKey("key" + i));
    }
  }

  private void doTestGetKeys() {
    System.out.println("... testing getKeys");
    for (int i = 0; i < 10; i++) {
      // does not put in the backend as not started yet
      map.put("key1" + i, "value" + i, null);
    }

    map.allowFlushBufferWrites();

    for (int i = 0; i < 10; i++) {
      // does not put in the backend as not started yet
      map.internalPutInFlushBuffer("key2" + i, "value" + i, null);
    }

    Collection keys = map.getKeys();
    Assert.assertEquals(20, keys.size());
  }

  private static class CollectingClusteredStoreBackend implements ClusteredStoreBackend<Object, Object> {

    private Map<Object, Object> backendMap  = Collections.synchronizedMap(new HashMap<Object, Object>());
    private final Set<Object>   pinnedKeys  = Collections.synchronizedSet(new HashSet<Object>());
    private Set<Object>         removedKeys = Collections.synchronizedSet(new HashSet<Object>());
    private volatile boolean    clearCalled = false;

    private final CacheConfig   config;

    public CollectingClusteredStoreBackend() {
      this.config = new MutableConfig();
    }

    public void initializeLocalCache(Configuration c) {
      //
    }

    public CacheConfig getConfig() {
      return config;
    }

    public void reset() {
      backendMap = Collections.synchronizedMap(new HashMap<Object, Object>());
      removedKeys = Collections.synchronizedSet(new HashSet<Object>());
      clearCalled = false;
    }

    public boolean removeCalled(Object key) {
      return removedKeys.contains(key);
    }

    public boolean clearCalled() {
      return clearCalled;
    }

    public void clear(MetaData metaData) {
      clearCalled = true;
      backendMap.clear();
    }

    public void unpinAll() {
      pinnedKeys.clear();
    }

    public boolean isPinned(Object key) {
      return pinnedKeys.contains(key);
    }

    public void setPinned(Object key, boolean pinned) {
      if (pinned) {
        pinnedKeys.add(key);
      }
    }

    public boolean containsKey(Object key) {
      return backendMap.containsKey(key);
    }

    public boolean containsLocalKey(Object key) {
      return backendMap.containsKey(key);
    }

    public void removeNoReturn(Object key, MetaData metaData) {
      throw new AssertionError();
    }

    public boolean unlockedContainsKey(Object key) {
      return backendMap.containsKey(key);
    }

    public boolean unlockedContainsLocalKey(Object key) {
      return backendMap.containsKey(key);
    }

    public Object unlockedGet(Object key) {
      return backendMap.get(key);
    }

    public void unlockedPutNoReturn(Object key, Object value, MetaData metaData) {
      backendMap.put(key, value);
    }

    public void unlockedRemoveNoReturn(Object key, MetaData metaData) {
      removedKeys.add(key);
      backendMap.remove(key);
    }

    public ClusteredLock createFinegrainedLock(Object key) {
      throw new AssertionError();
    }

    public void initializeTransients(RegisteredEventListeners cacheEventNotificationService, ClusteredStore store) {
      throw new AssertionError();
    }

    public TimeSource getTimeSource() {
      throw new AssertionError();
    }

    public int size() {
      throw new AssertionError();
    }

    public int localSize() {
      throw new AssertionError();
    }

    public Set<Object> keySet() {
      throw new AssertionError();
    }

    public Set<Object> localKeySet() {
      throw new AssertionError();
    }

    public TimestampedValue getTimestampedValue(Object key) {
      throw new AssertionError();
    }

    public TimestampedValue getTimestampedValueQuiet(Object key) {
      throw new AssertionError();
    }

    public Map<Object, TimestampedValue<Object>> getTimestampedValues(Set<Object> keys, boolean quiet) {
      throw new AssertionError();
    }

    public void putNoReturn(Object key, Object value, MetaData searchMetaData) {
      throw new AssertionError();
    }

    public TimestampedValue removeTimestampedValue(Object key, MetaData metaData) {
      throw new AssertionError();
    }

    public TimestampedValue unlockedGetTimestampedValue(Object key, boolean quiet) {
      throw new AssertionError();
    }

    public TimestampedValue unsafeGetTimestampedValue(Object key, boolean quiet) {
      throw new AssertionError();
    }

    public void shutdown() {
      throw new AssertionError();
    }

    public void clearLocalCache() {
      throw new AssertionError();
    }

    public TerracottaDistributedCache<Object, Object> getTerracottaDistributedCache() {
      throw new AssertionError();
    }

    public void unlockedReplaceNoReturn(Object key, Object currentValue, Object newValue, MetaData searchMetaData) {
      throw new AssertionError();
    }

    public void unlockedRemoveNoReturn(Object key, Object oldValue, MetaData metaData) {
      throw new AssertionError();
    }

    public void unlockedPutIfAbsentNoReturn(Object key, Object value, MetaData searchMetaData) {
      throw new AssertionError();
    }

    public Map<Object, TimestampedValue<Object>> unlockedGetAllTimeStampedValues(Set<Object> batchedKeys, boolean quiet) {
      throw new AssertionError();
    }

    public long localOnHeapSizeInBytes() {
      return 0;
    }

    public long localOffHeapSizeInBytes() {
      return 0;
    }

    public int localOnHeapSize() {
      return 0;
    }

    public int localOffHeapSize() {
      return 0;
    }

    public boolean containsKeyLocalOnHeap(Object key) {
      return false;
    }

    public boolean containsKeyLocalOffHeap(Object key) {
      return false;
    }

    public void setTargetMaxTotalCount(int targetMaxTotalCount) {
      // Do nothing
    }

    public void setMaxTTI(int maxTTI) {
      // Do nothing
    }

    public void setMaxTTL(int maxTTL) {
      // Do nothing
    }

    public void setTargetMaxInMemoryCount(int targetMaxInMemoryCount) {
      // Do nothing
    }

    public void setLoggingEnabled(boolean loggingEnabled) {
      // Do nothing
    }

    public void setMaxBytesLocalHeap(long maxBytesLocalHeap) {
      // Ignore
    }

    public void setLocalCacheEnabled(boolean enabled) {
      // Ignore
    }

    public void recalculateLocalCacheSize(Object key) {
      // ignored
    }
  }

  public static void reallySleep(long millis) {
    reallySleep(millis, 0);
  }

  public static void reallySleep(long millis, int nanos) {
    try {
      long millisLeft = millis;
      while (millisLeft > 0 || nanos > 0) {
        long start = System.currentTimeMillis();
        Thread.sleep(millisLeft, nanos);
        millisLeft -= System.currentTimeMillis() - start;
        nanos = 0; // Not using System.nanoTime() since it is 1.5 specific
      }
    } catch (InterruptedException ie) {
      Assert.fail("Interrupted while attempting to sleep for " + millis + " millis and " + nanos + " nanos.");
    }
  }

}
