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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

public class ExecutorServiceTest extends TestCase {

    public void testExecutorThreadsCreated() throws Exception {
        final NonStopCacheExecutorService service = new NonStopCacheExecutorService();
        int corePoolSize = NonStopCacheExecutorService.DEFAULT_THREAD_POOL_SIZE;
        int maxPoolSize = NonStopCacheExecutorService.DEFAULT_MAX_THREAD_POOL_SIZE;
        for (int i = 0; i < corePoolSize; i++) {
            service.execute(new NoopCallable(), 1000);
        }
        // assert at least core pool size threads has been created
        assertTrue(countExecutorThreads() >= corePoolSize);

        // submit another maxPoolSize jobs
        for (int i = 0; i < maxPoolSize; i++) {
            try {
                service.execute(new NoopCallable(), 1);
            } catch (TimeoutException e) {
                // ignore
            }
        }
        // assert maxPoolSize threads has been created
        assertTrue(countExecutorThreads() == maxPoolSize);

        int extraThreads = 10;
        int numAppThreads = maxPoolSize + extraThreads;
        final List<Exception> exceptionList = new ArrayList<Exception>();
        final CyclicBarrier barrier = new CyclicBarrier(numAppThreads + 1);
        final AtomicInteger finishedThreadsCount = new AtomicInteger();
        for (int i = 0; i < numAppThreads; i++) {
            Thread thread = new Thread(new Runnable() {

                public void run() {
                    try {
                        barrier.await();
                        service.execute(new BlockingCallable(false), 5000);
                    } catch (TimeoutException e) {
                        // ignore
                    } catch (Exception e) {
                        exceptionList.add(e);
                    }
                    finishedThreadsCount.incrementAndGet();
                }
            });
            thread.start();
        }

        // start the app threads
        System.out.println("Letting all app threads go ahead");
        barrier.await();
        // wait for one second so that all threads go inside executor
        Thread.sleep(1000);

        // now assert extraThread tasks are in the queue as maxPoolSize tasks should be picked up by the executor threads
        assertEquals(extraThreads, service.getTaskQueue().size());
        System.out.println("Asserted task queue size");

        System.out.println("Waiting for all app threads to complete");
        while (finishedThreadsCount.get() != numAppThreads) {
            Thread.sleep(1000);
            System.out.println("Finished: " + finishedThreadsCount.get() + "/" + numAppThreads);
        }
        // assert no other exception other than timeoutException happened
        assertEquals(0, exceptionList.size());

        // assert no more than maxPoolSize threads created
        assertTrue(countExecutorThreads() == maxPoolSize);
        System.out.println("Test complete successfully");
    }

    private int countExecutorThreads() {
        List<ThreadInformation> threadDump = getThreadDump();
        int rv = 0;
        for (ThreadInformation info : threadDump) {
            if (info.getThreadName().contains(NonStopCacheExecutorService.EXECUTOR_THREAD_NAME_PREFIX)) {
                // System.out.println("Thread: id=" + info.getThreadId() + ", name=\"" + info.getThreadName() +
                // "\": is an executor thread");
                rv++;
            }
        }
        System.out.println("Number of executor threads created till now: " + rv);
        return rv;
    }

    private static List<ThreadInformation> getThreadDump() {
        List<ThreadInformation> rv = new ArrayList<ThreadInformation>();
        ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
        for (long id : tbean.getAllThreadIds()) {
            ThreadInfo tinfo = tbean.getThreadInfo(id, Integer.MAX_VALUE);
            rv.add(new ThreadInformation(tinfo.getThreadId(), tinfo.getThreadName()));
        }
        return rv;
    }

    private static class ThreadInformation {
        private final long threadId;
        private final String threadName;

        public ThreadInformation(long threadId, String name) {
            super();
            this.threadId = threadId;
            this.threadName = name;
        }

        public long getThreadId() {
            return threadId;
        }

        public String getThreadName() {
            return threadName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (threadId ^ (threadId >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ThreadInformation other = (ThreadInformation) obj;
            if (threadId != other.threadId)
                return false;
            return true;
        }

    }

    private static class NoopCallable implements Callable<Void> {

        public Void call() throws Exception {
            // do nothing
            return null;
        }

    }

}
