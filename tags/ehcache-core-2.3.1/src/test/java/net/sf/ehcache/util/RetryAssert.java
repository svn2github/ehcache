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

import org.junit.Assert;

public class RetryAssert {
    private static final long WAIT_TIME = 100L;

    protected RetryAssert() {
        // static only class
    }

    /**
     * Acts like Assert.assertEquals(expected, actual) but allow for retries with
     * some wait time in between
     * @param expected
     * @param actual
     * @param retries
     */
    public static void assertEquals(long expected, long actual, int retries) {
        int tries = 0;
        while (true) {
            try {
                Assert.assertEquals(expected, actual);
                break;
            } catch (AssertionError error) {
                tries++;
                if (tries < retries) {
                    try {
                        Thread.sleep(WAIT_TIME);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    throw error;
                }
            }
        }
    }
}
