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

package net.sf.ehcache.constructs.nonstop;

import java.lang.reflect.Constructor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.TestCase;

import org.junit.Test;

public class RejectedExecutionTest extends TestCase {

    private static final long SYSTEM_CLOCK_EPSILON_MILLIS = 20;

    @Test
    public void testRejectedExecution() throws Exception {
        Constructor<NonStopCacheExecutorService> constructor = NonStopCacheExecutorService.class
                .getDeclaredConstructor(ExecutorService.class);
        constructor.setAccessible(true);

        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(1);
        NonStopCacheExecutorService nonStopCacheExecutorService = constructor.newInstance(new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS,
                workQueue));

        // this job will start the thread
        try {
            nonStopCacheExecutorService.execute(new BlockingCallable(), 100);
            fail("should have thrown timeout exception");
        } catch (Exception e) {
            if (!(e instanceof TimeoutException)) {
                throw new AssertionError("Expected to catch TimeoutException but caught: " + e);
            }
            System.out.println("Caught expected exception - " + e);
        }

        // this job will be queued in the workQueue (has capacity 1)
        try {
            assertEquals(0, workQueue.size());
            nonStopCacheExecutorService.execute(new BlockingCallable(), 1000);
            fail("should have thrown timeout exception");
        } catch (Exception e) {
            if (!(e instanceof TimeoutException)) {
                throw new AssertionError("Expected to catch TimeoutException but caught: " + e);
            }
            System.out.println("Caught expected exception - " + e);
        }
        assertEquals(1, workQueue.size());

        // now scheduling another job should be rejected
        // as our executor has only 1 thread and a work queue with capacity 1
        long start = System.currentTimeMillis();
        try {
            nonStopCacheExecutorService.execute(new BlockingCallable(), 1000);
            fail("should have thrown timeout exception");
        } catch (Exception e) {
            long timeTakenMillis = System.currentTimeMillis() - start;
            if (!(e instanceof TimeoutException)) {
                throw new AssertionError("Expected to catch TimeoutException but caught: " + e);
            }
            assertTrue("TimeoutException should be due to RejectedExecutionException", e instanceof TaskNotSubmittedTimeoutException);
            // make sure attempt to schedule the job was made until timeout provided
            assertTrue(timeTakenMillis + SYSTEM_CLOCK_EPSILON_MILLIS >= 1000);
            // at least more than one attempt was made to submit the task, although practically this number is way bigger than 2
            int attemptCount = ((TaskNotSubmittedTimeoutException) e).getSubmitAttemptCount();
            System.out.println("Number of attempts made to submit task: " + attemptCount);
            assertTrue(attemptCount >= 2);
        }

    }

    private static class BlockingCallable implements Callable<Void> {

        public Void call() throws Exception {
            System.out.println("inside blocking callable");
            Thread.currentThread().join();
            return null;
        }

    }

}
