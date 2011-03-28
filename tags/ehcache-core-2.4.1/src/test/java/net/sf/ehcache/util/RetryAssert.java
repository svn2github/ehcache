/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.util;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;
import org.junit.Assert;

public class RetryAssert {
    private static final long WAIT_TIME = 100L;

    protected RetryAssert() {
        // static only class
    }

    public static <T> void assertBy(long time, TimeUnit unit, Callable<T> value, Matcher<T> matcher) {
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
}
