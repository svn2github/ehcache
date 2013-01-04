package net.sf.ehcache.store;

import net.sf.ehcache.MemoryStorePerfTester;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Alex Snaps
 */
public class FifoMemoryStorePerfTest extends MemoryStorePerfTester {

    /**
     * setup test
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        createMemoryOnlyStore(MemoryStoreEvictionPolicy.FIFO);
    }

    /**
     * Benchmark to test speed.
     * v 1.38 DiskStore 7238
     * v 1.42 DiskStore 1907
     */
    @Test
    public void testBenchmarkPutGetSurya() throws Exception {
        benchmarkPutGetSuryaTest(1500);
    }
    
}
