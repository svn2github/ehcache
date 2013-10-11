package net.sf.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.MemoryStorePerfTester;
import org.junit.Test;

/**
 * @author Alex Snaps
 */
public class FifoMemoryStorePerfTest extends MemoryStorePerfTester {

    @Override
    protected Cache createCache() throws CacheException {
        return new Cache("FifoMemoryStorePerfTest", 12000, MemoryStoreEvictionPolicy.FIFO, false, System.getProperty("java.io.tmpdir"), 
                true, 60, 30, false, 60, null);
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
