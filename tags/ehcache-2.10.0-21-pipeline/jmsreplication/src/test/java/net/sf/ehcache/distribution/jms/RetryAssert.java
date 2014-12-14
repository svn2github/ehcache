/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.distribution.jms;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.Store;

import org.hamcrest.Matcher;
import org.junit.Assert;

public class RetryAssert {

    protected RetryAssert() {
        // static only class
    }

    public static void sleepFor(long time, TimeUnit unit) {
      boolean interrupted = false;
      long duration = unit.toNanos(time);
      long sleep = Math.max(20, TimeUnit.NANOSECONDS.toMillis(duration / 10));
      long end = System.nanoTime() + duration;
      try {
        while (true) {
          long remaining  = end - System.nanoTime();
          if (remaining <= 0) {
            break;
          } else {
            try {
              Thread.sleep(Math.min(sleep, TimeUnit.NANOSECONDS.toMillis(remaining) + 1));
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
    }
    
    public static <T> void assertBy(long time, TimeUnit unit, Callable<T> value, Matcher<? super T> matcher) {
        boolean interrupted = false;
        long end = System.nanoTime() + unit.toNanos(time);
        try {
            for (long sleep = 10; ; sleep <<= 1L) {
                try {
                    Assert.assertThat(value.call(), matcher);
                    return;
                } catch (Throwable t) {
                    //ignore - wait for timeout
                }

                long remaining = end - System.nanoTime();
                if (remaining <= 0) {
                    break;
                } else {
                    try {
                        Thread.sleep(Math.min(sleep, TimeUnit.NANOSECONDS.toMillis(remaining) + 1));
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
        try {
            Assert.assertThat(value.call(), matcher);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static Callable<Element> elementAt(final Ehcache cache, final Object key) {
        return new Callable<Element>() {
            public Element call() {
                return cache.get(key);
            }
        };
    }

    public static Callable<Object> valueAt(final Ehcache cache, final Object key) {
        return new Callable<Object>() {
            public Object call() {
                Element e = cache.get(key);
                return e == null ? null : e.getObjectValue();
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
