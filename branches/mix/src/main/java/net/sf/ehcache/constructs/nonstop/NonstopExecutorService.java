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

package net.sf.ehcache.constructs.nonstop;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import net.sf.ehcache.CacheException;

/**
 *
 * @author Abhishek Sanoujam
 *
 */
public interface NonstopExecutorService {

    /**
     * System property name which if set to true prints stack trace of nonstop thread upon exception
     */
    public static final String PRINT_STACK_TRACE_ON_EXCEPTION_PROPERTY = "net.sf.ehcache.nonstop.printStackTraceOnException";

    /**
     * Execute a {@link Callable} task with timeout. If the task does not complete within the timeout specified, throws a
     * {@link TimeoutException}
     *
     * @param <V>
     * @param callable
     * @param timeoutValueInMillis
     * @return the return value from the callable
     * @throws TimeoutException
     * @throws CacheException
     * @throws InterruptedException
     */
    public <V> V execute(final Callable<V> callable, final long timeoutValueInMillis) throws TimeoutException, CacheException,
            InterruptedException;

    /**
     * Shut down this executor service
     */
    public void shutdown();

}
