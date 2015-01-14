/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.junit.Assert;
import org.terracotta.toolkit.Toolkit;

import java.io.Serializable;

// do remove before doing puts
public class WriteBehindAtomicityTestClient extends AbstractWriteBehindClient {
  private static final int ELEMENT_COUNT = 10;
  
  public WriteBehindAtomicityTestClient(String[] args) {
    super(args);
  }

  @Override
  public long getSleepBetweenWrites() {
    return 100L;
  }

  @Override
  public long getSleepBetweenDeletes() {
    return 100L;
  }

  @Override
  protected void runTest(final Cache cache, Toolkit toolkit) throws Throwable {
    cache.registerCacheWriter(new WriteBehindCacheWriter(this));
    testPutWithWriter(cache);
    validateDataInCache(cache);
    testRemoveWithWriter(cache);
    validateEmptyCache(cache);
  }

  private void validateDataInCache(Cache cache) {
    for (int i = 0; i < ELEMENT_COUNT; i++) {
      Assert.assertEquals(cache.get(getKey(i)), new Element(getKey(i), getValue(i)));
    }
  }

  private void testPutWithWriter(final Cache cache) {
    for (int i = 0; i < ELEMENT_COUNT; i++) {
      cache.putWithWriter(new Element(getKey(i), getValue(i)));
    }
  }

  private void testRemoveWithWriter(final Cache cache) {
    for (int i = 0; i < ELEMENT_COUNT; i++) {
      cache.removeWithWriter(getKey(i));
    }
  }

  private void validateEmptyCache(Cache cache) {
    Assert.assertEquals(0, cache.getSize());
    for (int i = 0; i < ELEMENT_COUNT; i++) {
      Assert.assertNull(cache.get(getKey(i)));
    }
  }

  private Object getValue(int i) {
    return new MyValue("value" + i);
  }

  private Object getKey(int i) {
    return new MyKey("key" + i);
  }

  private static class MyObject implements Serializable {
    private final String value;

    public MyObject(String value) {
      this.value = value;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      MyObject other = (MyObject) obj;
      if (value == null) {
        if (other.value != null) return false;
      } else if (!value.equals(other.value)) return false;
      return true;
    }

    @Override
    public String toString() {
      return "TCObject [i=" + value + "]";
    }

  }

  private static class MyKey extends MyObject {

    public MyKey(String value) {
      super(value);
    }
  }

  private static class MyValue extends MyObject {

    public MyValue(String value) {
      super(value);
    }

  }

}
