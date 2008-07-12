package net.sf.ehcache;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import net.sf.ehcache.store.DiskStore;

/**
 * Isolated performance test which only runs one cache at a time.
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class CachePerformanceTest extends TestCase {

    private static final Log LOG = LogFactory.getLog(CacheTest.class.getName());




    /**
     * the CacheManager instance
     */
    protected CacheManager manager;

    /**
     * setup test
     */
    protected void setUp() throws Exception {
        manager = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-defaultonly.xml");
    }

    /**
     * teardown
     */
    protected void tearDown() throws Exception {
        if (manager != null) {
            manager.shutdown();
        }
    }


    /**
     * With 50,000 gets
     * m500d45500Cache: 15098 ms with write back to DiskStore
     * m500d45500Cache: 10941 ms without write back TODO we should be able to do this optimisation with a small change
     *
     */
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
