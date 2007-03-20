package net.sf.ehcache.distribution;


import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A distributed testing tool for manual distributed testing of ehcache on cluster nodes.
 * <p/>
 * It is passed the ehcache configuration to use and cache to monitor
 *
 * @author Greg Luck
 * @version $Id: RemoteDebugger.java 331 2007-01-29 09:38:41Z gregluck $
 */
public final class RemoteDebugger {
    private static final int TWO_SECONDS = 2000;

    private static final Log LOG = LogFactory.getLog(RemoteDebugger.class.getName());

    /**
     * Utility class. No constructor
     */
    private RemoteDebugger() {
        //noop
    }


    /**
     * A Remote Debugger which prints out the cache size of the monitored cache.
     * Additional logging messages can be observed by setting the logging level to debug
     * or trace for net.sf.ehcache.distribution
     *
     * @param args path_to_ehcache.xml and a cache name
     * @throws InterruptedException thrown when it is interrupted. It will keep going until then.
     */
    public static void main(String[] args) throws InterruptedException {

        if (args.length < 1 || args.length > 2) {
            LOG.info("Command line to list caches to monitor: java -jar ehcache-remote-debugger.jar path_to_ehcache.xml\n" +
                    "Command line to monitor a specific cache: java -jar ehcache-remote-debugger.jar path_to_ehcache.xml" +
                    " cacheName");
            System.exit(2);
        }

        if (!LOG.isTraceEnabled()) {
            LOG.info("Increase the net.sf.ehcache.distribution logging level to debug or trace to see distributed" +
                    " cache operations as they occur.");
        }

        CacheManager manager = new CacheManager(args[0]);
        String[] cacheNames = manager.getCacheNames();
        StringBuffer availableCaches = new StringBuffer();
        if (args.length == 1) {
            for (int i = 0; i < cacheNames.length; i++) {
                String name = cacheNames[i];
                availableCaches.append(name).append(' ');
            }
            LOG.info("Available caches are: " + availableCaches);
            System.exit(1);
        } else {
            String cacheName = args[1];
            Ehcache cache = manager.getCache(cacheName);
            if (cache == null) {
                LOG.error("No cache named " + cacheName + " exists. Available caches are: " + availableCaches);
            } else {
                LOG.info("Monitoring cache: " + cacheName);

                while (true) {
                    Thread.sleep(TWO_SECONDS);
                    LOG.info("Cache size: " + cache.getSize());
                }
            }
        }
    }
}
