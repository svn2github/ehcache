/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package net.sf.ehcache.util;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.Store;

import org.hamcrest.Matcher;
import org.junit.Assert;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class RetryAssert {
  protected RetryAssert() {
    // static only class
  }

  public static <T> void assertBy(long time, TimeUnit unit, Callable<T> value, Matcher<? super T> matcher) {
    boolean interrupted = false;
    long start = System.nanoTime();
    long end = start + unit.toNanos(time);
    long sleep = Math.max(50, unit.toMillis(time) / 10);
    AssertionError latest;
    try {
      while (true) {
        try {
          Assert.assertThat(value.call(), matcher);
          return;
        } catch (AssertionError e) {
          latest = e;
        } catch (Exception e) {
          latest = new AssertionError(e);
        }

        if (System.nanoTime() > end) {
          break;
        } else {
          try {
            Thread.sleep(sleep);
          } catch (InterruptedException e) {
            interrupted = true;
          }
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
    throw latest;
  }

  public static Callable<Element> elementAt(final Ehcache cache, final Object key) {
    return new Callable<Element>() {
      public Element call() {
        return cache.get(key);
      }
    };
  }

  public static Callable<Integer> sizeOf(final Ehcache cache) {
    return new Callable<Integer>() {
      public Integer call() throws Exception {
        return cache.getSize();
      }
    };
  }

  public static Callable<Integer> sizeOnDiskOf(final Store store) {
    return new Callable<Integer>() {
      public Integer call() throws Exception {
        return store.getOnDiskSize();
      }
    };
  }
}
