package net.sf.ehcache;

import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Isolated performance test which only runs one cache at a time.
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class CachePerformanceTest {

    private static final Logger LOG = LoggerFactory.getLogger(CachePerformanceTest.class.getName());


    /**
     * the CacheManager instance
     */
    protected CacheManager manager;

    /**
     * setup test
     */
    @Before
    public void setUp() throws Exception {
        manager = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-defaultonly.xml");
    }

    /**
     * teardown
     */
    @After
    public void tearDown() throws Exception {
        if (manager != null) {
            manager.shutdown();
        }
    }


    /**
     * With 50,000 gets
     * Time to get 50000 entries from m500d45500Cache: 5746
     * Time to get 50000 entries from m500d45500Cache: 3795 if writeback to the memory store is turned off
     * TODO we should be able to do this optimisation with a small change
     */
    @Test
    public void testGetSpeedMostlyDisk() throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        long time = 0;
        Cache m500d500Cache = manager.getCache("m500d45500Cache");
        if (m500d500Cache == null) {
            m500d500Cache = new Cache("m500d45500Cache", 500, true, true, 5, 2);
            manager.addCache(m500d500Cache);
            m500d500Cache = manager.getCache("m500d45500Cache");
            for (int i = 0; i < 50000; i++) {
                Integer key = new Integer(i);
                m500d500Cache.put(new Element(key, "value" + i));
            }
            stopWatch.getElapsedTime();
            //let spool write out before testing get performance
        }
        time = stopWatch.getElapsedTime();
        for (int i = 0; i < 50000; i++) {
            Integer key = new Integer(i);
            m500d500Cache.get(key);
        }
        time = stopWatch.getElapsedTime();
        LOG.info("Time to get 50000 entries from m500d45500Cache: " + time);
        assertTrue("Time to get 50000 entries from m500d500Cache", time < 20000);
    }


    /**
     * With 50,000 gets
     * Time to get 50000 entries from m50000Cache: 174
     */
    @Test
    public void testGetSpeedMemoryOnly() throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        long time = 0;
        Cache m500d500Cache = manager.getCache("m50000Cache");
        if (m500d500Cache == null) {
            m500d500Cache = new Cache("m50000Cache", 50000, true, true, 5, 2);
            manager.addCache(m500d500Cache);
            m500d500Cache = manager.getCache("m50000Cache");
            for (int i = 0; i < 50000; i++) {
                Integer key = new Integer(i);
                m500d500Cache.put(new Element(key, "value" + i));
            }
            Thread.sleep(1000);
            stopWatch.getElapsedTime();

        }
        time = stopWatch.getElapsedTime();
        for (int i = 0; i < 50000; i++) {
            Integer key = new Integer(i);
            m500d500Cache.get(key);
        }
        time = stopWatch.getElapsedTime();
        LOG.info("Time to get 50000 entries from m50000Cache: " + time);
        assertTrue("Time to get 50000 entries from m50000Cache", time < 20000);
    }


}
