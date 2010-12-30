package net.sf.ehcache.transaction.xa;

import junit.framework.TestCase;
import net.sf.ehcache.transaction.xa.processor.XAThreadPool;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lorban
 */
public class XAThreadPoolTest extends TestCase {

    public void test() throws Exception {
        final int COUNTER = 5000;
        final int CONCURRENCY = 50;

        XAThreadPool xaThreadPool = new XAThreadPool();

        XAThreadPool.MultiRunner[] runners = new XAThreadPool.MultiRunner[CONCURRENCY];
        for (int i=0; i<CONCURRENCY ;i++) {
            runners[i] = xaThreadPool.getMultiRunner();
        }

        final Map<String, AtomicInteger> results = new ConcurrentHashMap<String, AtomicInteger>();

        Callable myCallable = new Callable() {
            public Object call() throws Exception {
                String threadName = Thread.currentThread().getName();

                AtomicInteger counter = results.get(threadName);
                if (counter == null) {
                    counter = new AtomicInteger();
                    results.put(threadName, counter);
                }

                counter.incrementAndGet();

                return null;
            }
        };

        // execution
        for (int i=0; i<COUNTER ;i++) {
            for (int j=0 ;j<CONCURRENCY ;j++) {
                runners[j].execute(myCallable);
            }
        }

        // release
        for (int j=0 ;j<CONCURRENCY ;j++) {
            runners[j].release();

            try {
                runners[j].execute(myCallable);
                fail("expected IllegalStateException");
            } catch (IllegalStateException e) {
                // expected
            }
        }

        // assertions
        assertEquals(CONCURRENCY, results.size());
        for (Map.Entry<String, AtomicInteger> entry : results.entrySet()) {
            assertEquals(COUNTER, entry.getValue().get());
        }
    }
}
