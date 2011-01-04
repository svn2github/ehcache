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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.ehcache.constructs.nonstop.ThreadDump.ThreadInformation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutorServiceTest extends TestCase {

    private static final String TEST_EXECUTOR_THREAD_NAME_PREFIX = "Test Executor thread";
    private NonstopExecutorServiceImpl service;
    private static final Logger LOG = LoggerFactory.getLogger(ExecutorServiceTest.class);

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

    public void testOnlyOneExecutorThreadCreated() throws Exception {
        int initialThreadsCount = countExecutorThreads();
        int numOps = 100;
        for (int i = 0; i < numOps; i++) {
            service.execute(new NoopCallable(), 5000);
        }
        // assert almost 1 pool thread is created
        int actualThreadCount = countExecutorThreads() - initialThreadsCount;
        assertTrue("ActualThreadCount: " + actualThreadCount + " Expected to be less than 3", 3 >= actualThreadCount);
    }

    public void testOneExecutorThreadCreatedPerAppThread() throws Exception {
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
    }

    public void testMultipleExecutorThreadsCreatedPerAppThread() throws Exception {
        int initialThreadsCount = countExecutorThreads();
        int numRequests = 20;
        // submitting multiple blocking jobs should create multiple threads
        for (int i = 0; i < numRequests; i++) {
            try {
                service.execute(new BlockingCallable(), 10);
                fail("Executing blockingCallable should timeout");
            } catch (TimeoutException e) {
                // ignore
            }
        }
        Thread.sleep(1000);
        assertEquals("", numRequests, countExecutorThreads() - initialThreadsCount);

    }

    private int countExecutorThreads() {
        List<ThreadInformation> threadDump = ThreadDump.getThreadDump();
        int rv = 0;
        List<ThreadInformation> threads = new ArrayList();
        for (ThreadInformation info : threadDump) {
            if (info.getThreadName().contains(TEST_EXECUTOR_THREAD_NAME_PREFIX)) {
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
