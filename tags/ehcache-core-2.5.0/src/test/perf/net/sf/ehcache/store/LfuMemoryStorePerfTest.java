package net.sf.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.MemoryStorePerfTester;
import net.sf.ehcache.StopWatch;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertTrue;

/**
 * @author Alex Snaps
 */
public class LfuMemoryStorePerfTest extends MemoryStorePerfTester {

    private static final Logger LOG = LoggerFactory.getLogger(LfuMemoryStorePerfTest.class.getName());

    private static final Field AUTHORITY;
    private static final Method FIND_EVICTION_CANDIDATE;
    static {
        try {
            AUTHORITY = FrontEndCacheTier.class.getDeclaredField("authority");
            AUTHORITY.setAccessible(true);
            FIND_EVICTION_CANDIDATE = MemoryStore.class.getDeclaredMethod("findEvictionCandidate", Element.class);
            FIND_EVICTION_CANDIDATE.setAccessible(true);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * setup test
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        createMemoryOnlyStore(MemoryStoreEvictionPolicy.LFU);
    }

    /**
     * Benchmark to test speed.
     * This takes a little longer for LFU than the others.
     * Used to take about 7400ms. Now takes 827.
     */
    @Test
    public void testBenchmarkPutGetSurya() throws Exception {
        benchmarkPutGetSuryaTest(9000);
    }

    /**
     * Benchmark to test speed.
     * new sampling LFU 417ms
     */
    @Override
    @Test
    public void testBenchmarkPutGetRemove() throws Exception {
        super.testBenchmarkPutGetRemove();
    }

    /**
     * Benchmark to test speed.
     * new sampling LFU 132ms
     */
    @Override
    @Test
    public void testBenchmarkPutGet() throws Exception {
        super.testBenchmarkPutGet();
    }

    /**
     * HashMap
     * INFO: done putting: 128ms
     * INFO: 15ms
     * <p/>
     * ConcurrentHashMap
     * INFO: done putting: 200ms
     * INFO: 117ms
     * <p/>
     * ConcurrentHashMap
     */
//    @Test
    public void testSpeedOfIteration() {
        StopWatch stopWatch = new StopWatch();
        Map map = new ConcurrentHashMap(100000);
        for (int i = 1; i <= 100000; i++) {
            map.put(i, i);
        }
        LOG.info("done putting: " + stopWatch.getElapsedTimeString());

        Collection collection = map.values();
        for (Object o : collection) {
            o.toString();
        }
        LOG.info(stopWatch.getElapsedTimeString());

    }

        /**
     * Check we get reasonable results for 2000 entries where entry 0 is accessed once increasing to entry 1999 accessed
     * 2000 times.
     * <p/>
     * 1 to 5000 population, with hit counts ranging from 1 to 500, not selecting lowest half. 5000 tests
     * <p/>
     * Samples  Cost    No
     * 7        38      99.24% confidence
     * 8        27      99.46% confidence
     * 9        10
     * 10       11300 4       99.92% confidence
     * 12       2
     * 20 11428 0  99.99% confidence
     * <p/>
     * 1 to 5000 population, with hit counts ranging from 1 to 500, not selecting lowest quarter. 5000 tests
     * S        No
     * 10       291 94.18% confidence
     * 20       15
     * 30       11536 1 99.99% confidence
     * <p/>
     * For those with a statistical background the branch of stats which deals with this is hypothesis testing and
     * the Student's T distribution. The higher your sample the greater confidence you can have in a hypothesis, in
     * this case whether or not the "lowest" value lies in the bottom half or quarter of the distribution. Adding
     * samples rapidly increases confidence but the return from extra sampling rapidly diminishes.
     * <p/>
     * Cost is not affected much by sample size. Profiling shows that it is the iteration that is causing most of the
     * time. If we had access to the array backing Map, all would work very fast. Still, it is fast enough.
     * <p/>
     * A 99.99% confidence interval can be achieved that the "lowest" element is actually in the bottom quarter of the
     * hit count distribution.
     *
     * @throws java.io.IOException Performance:
     *                     With a sample size of 10: 523ms for 5000 runs = 104 ?s per run
     *                     With a sample size of 30: 628ms for 5000 runs = 125 ?s per run
     */
    @Test
    public void testLowest() throws Exception {
        createMemoryOnlyStore(MemoryStoreEvictionPolicy.LFU, 5000);

        // Populate the cache with 5000 unaccessed Elements
        for (int i = 0; i < 5000; i++) {
            store.put(new Element(Integer.valueOf(i), new Date()));
        }

        for (int i = 0; i < 10; i++) {
            // Add a new Element at the i'th position
            Element newElement = new Element(Integer.valueOf(i), new Date());
            store.put(newElement);

            // Hit that Element (i+1) times - this makes it the most hit Element
            // in the cache
            for (int h = 0; h < (i + 1); h++) {
                store.get(Integer.valueOf(i)).updateAccessStatistics();
            }

            // Select an Element for "eviction".
            Element element = (Element)FIND_EVICTION_CANDIDATE.invoke(AUTHORITY.get(store), new Object[] { null });
            // This shouldn't be the newly added Element as it is the "most hit"
            assertTrue(!element.equals(newElement));
            // In fact since the sample size is > 10, the hit count should be 0
            // as we must have selected some of the non hit Elements in our sample.
            assertTrue(element.getHitCount() == 0);
        }

        // Repeat the hitting procedure above, but for the remaining elements
        // This gives a flat distribution of hit counts from 1 to 5000 all with
        // equal probability (1 element of each count).
        for (int i = 10; i < 5000; i++) {
            store.put(new Element(Integer.valueOf(i), new Date()));
            for (int h = 0; h < (i + 1); h++) {
                store.get(Integer.valueOf(i)).updateAccessStatistics();
            }
        }

        long lowestQuartile = 5000 / 4;
        
        long findTime = 0;
        StopWatch stopWatch = new StopWatch();
        int lowestQuartileNotIdentified = 0;
        for (int i = 0; i < 5000; i++) {
            stopWatch.getElapsedTime();
            // Select an Element for "eviction"
            Element e = (Element)FIND_EVICTION_CANDIDATE.invoke(AUTHORITY.get(store), new Object[] { null });
            findTime += stopWatch.getElapsedTime();
            long lowest = e.getHitCount();
            // See if it is outside the lowest quartile (i.e. it has an abnormaly
            // high hit count).
            if (lowest > lowestQuartile) {
                LOG.info(e.getKey() + " hit count: " + e.getHitCount() + " lowestQuartile: " + lowestQuartile);
                lowestQuartileNotIdentified++;
            }
        }

        LOG.info("Find time: " + findTime);
        // Assert that we can do all this in a reasonable length of time
        assertTrue(findTime < 200);
        LOG.info("Selections not in lowest quartile: " + lowestQuartileNotIdentified);
        // Assert that we didn't see too many eviction candidates from outside
        // the lowest quartile.
        assertTrue(lowestQuartileNotIdentified + " > 10!!!", lowestQuartileNotIdentified <= 10);
    }
}
