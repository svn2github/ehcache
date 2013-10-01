package net.sf.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.MemoryStorePerfTester;
import org.junit.Test;

/**
 * @author Alex Snaps
 */
public class LruMemoryStorePerfTest extends MemoryStorePerfTester {

    @Override
    protected Cache createCache() throws CacheException {
        return new Cache("LruMemoryStorePerfTest", 12000, MemoryStoreEvictionPolicy.FIFO, false, System.getProperty("java.io.tmpdir"), 
                true, 60, 30, false, 60, null);
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
