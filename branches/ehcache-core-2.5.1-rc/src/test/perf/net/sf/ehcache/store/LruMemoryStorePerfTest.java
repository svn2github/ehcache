package net.sf.ehcache.store;

import net.sf.ehcache.MemoryStorePerfTester;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Alex Snaps
 */
public class LruMemoryStorePerfTest extends MemoryStorePerfTester {

    /**
     * setup test
     */
    @Override
    @Before
    public void setUp() throws Exception {

        super.setUp();
        createMemoryOnlyStore(MemoryStoreEvictionPolicy.LRU);
    }

    /**
     * Benchmark to test speed. This uses both memory and disk and tries to be realistic
     * v 1.38 DiskStore 7355
     * v 1.41 DiskStore 1609
     * Adjusted for change to laptop
     */
    @Test
    public void testBenchmarkPutGetSurya() throws Exception {
        benchmarkPutGetSuryaTest(2500);
    }


}
