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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.ehcache.constructs.nonstop.ThreadDump.ThreadInformation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NonStopThreadDestroyedTest extends TestCase {

    private static final String TEST_EXECUTOR_THREAD_NAME_PREFIX = "Test Executor thread";
    private static final String TEST_NONSTOP_REAPER_THREAD = "non stop reaper thread";
    private NonstopExecutorServiceImpl service;
    private static final Logger LOG = LoggerFactory.getLogger(ExecutorServiceTest.class);
    private final Object waitObject = new Object();

    @Override
    protected void setUp() throws Exception {
        int initialThreadsCount = countExecutorThreads();
        LOG.info("Initial thread count: " + initialThreadsCount);
        Assert.assertEquals(0, initialThreadsCount);
        service = new NonstopExecutorServiceImpl(new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger();

            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, TEST_EXECUTOR_THREAD_NAME_PREFIX + "-" + count.incrementAndGet() + " [for '"
                        + Thread.currentThread().getName() + "']");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    @Override
    protected void tearDown() throws Exception {
        service.shutdown();
        Thread.sleep(2000);
        int threads = countExecutorThreads();
        LOG.info("After shutting down service, thread count: " + threads);
        Assert.assertEquals(0, threads);
        LOG.info("Test complete successfully");
    }

    public void testNonStopThreadDestroyedAfterGC() throws Exception {
        int initialThreadsCount = countExecutorThreads();
        // each request thread should create one executor thread
        List<Thread> requestThreads = new ArrayList<Thread>();
        int extraRequests = 20;
        for (int i = 0; i < extraRequests; i++) {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        service.execute(new NoopCallable(), 5000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail("Executing noopCallable should not fail");
                    }
                }
            }, "RequestThread-" + i);
            requestThreads.add(thread);
            thread.start();
        }
        for (Thread t : requestThreads) {
            t.join();
        }
        Assert.assertEquals(extraRequests, countExecutorThreads() - initialThreadsCount);

        //clear it so that threads can get gced
        requestThreads.clear();

        for (int i = 0; i < 10; i++)
            System.gc();

        Thread.sleep(5000);
        Assert.assertEquals(0, countExecutorThreads() - initialThreadsCount);
    }

    public void testNonStopThreadDestroyedWithoutGC() throws Exception {
        int initialThreadsCount = countExecutorThreads();
        // each request thread should create one executor thread
        List<Thread> requestThreads = new ArrayList<Thread>();
        int extraRequests = 20;
        for (int i = 0; i < extraRequests; i++) {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        service.execute(new NoopCallable(), 5000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail("Executing noopCallable should not fail");
                    }
                }
            }, "RequestThread-" + i);
            requestThreads.add(thread);
            thread.start();
        }
        for (Thread t : requestThreads) {
            t.join();
        }
        Assert.assertEquals(extraRequests, countExecutorThreads() - initialThreadsCount);

        //don't clear requestThreads since we don't want app threads to be gced

        //we check whether the app thread has died evert 100 seconds so wait for like 2 mins
        System.out.println("waiting for 2 mins...");
        Thread.sleep(2 * 60 * 1000);
        Assert.assertEquals(0, countExecutorThreads() - initialThreadsCount);
    }

    private int countExecutorThreads() {
        List<ThreadInformation> threadDump = ThreadDump.getThreadDump();
        int rv = 0;
        List<ThreadInformation> threads = new ArrayList();
        for (ThreadInformation info : threadDump) {
            if (info.getThreadName().contains(TEST_EXECUTOR_THREAD_NAME_PREFIX)
                    || info.getThreadName().contains(TEST_NONSTOP_REAPER_THREAD)) {
                // LOG.info("Thread: id=" + info.getThreadId() + ", name=\"" + info.getThreadName() +
                // "\": is an executor thread");
                threads.add(info);
                rv++;
            }
        }
        LOG.info("Counting number of executor threads created till now: " + rv);
        String string = "{";
        for (ThreadInformation info : threads) {
            string += info.getThreadName() + " [id=" + info.getThreadId() + "], ";
        }
        string += "}";
        LOG.info("Thread name/ids: " + string);
        return rv;
    }

    private static class NoopCallable implements Callable<Void> {

        public Void call() throws Exception {
            // do nothing
            return null;
        }

    }

}
