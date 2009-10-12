package net.sf.ehcache.distribution;


import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.event.CacheEventListener;

import java.util.Iterator;

/**
 * A distributed testing tool for monitoring and debugging of distributed ehcache.
 * <p/>
 * This class has its own main method and is packaged in a jar.
 * <p/>
 * It can be invoked with two arguments:
 * <ol>
 * <li>the ehcache configuration file e.g. app/config/ehcache.xml. This file should be configured to allow
 * ehcache to joing the cluster. Using one of the existing ehcache.xml files from the other nodes normally is
 * sufficient.
 * <li>the name of the cache e.g. distributedCache1
 * </ol>
 * If only the first argument is passed, it will print our a list of caches with replication configured
 * from the configuration file, which are then available for monitoring.
 * <p/>
 * If the second argument is provided, the debugger will monitor cache operations received for the given cache.
 * <p/>
 * This is done by registering a CacheEventListener which prints out each operation.
 * <p/>
 * If nothing is happening, but cache operations should be going through, enable trace (LOG4J) or finest (JDK) level
 * logging on <code>net.sf.ehcache.distribution</code> in the logging configuration being used by the debugger.
 *
 * @author Greg Luck
 * @version $Id: RemoteDebugger.java 331 2007-01-29 09:38:41Z gregluck $
 */
public final class RemoteDebugger {

    private static final int TWO_SECONDS = 2000;

    private static CacheManager manager;
    private boolean keepMonitoring;
    private ConsolePrintingCacheEventListener consolePrintingCacheEventListener;
    private String configurationFileName;
    private String cacheNameToMonitor;


    /**
     * Constructor
     *
     * @param configurationFileName e.g. app/config/ehcache.xml
     * @param cacheNameToMonitor    sampleDistributedCache1
     */
    public RemoteDebugger(String configurationFileName, String cacheNameToMonitor) {

        this.configurationFileName = configurationFileName;
        this.cacheNameToMonitor = cacheNameToMonitor;

        try {
            manager = new CacheManager(configurationFileName);
        } catch (CacheException e) {
            System.err.println("Exception starting CacheManager from configuration " + configurationFileName + ". " +
                    "Stack trace follows:");
            e.printStackTrace();
        }

    }

    /**
     * Initialises the debugger and starts the monitoring loop. Call {@link #dispose()} to programmtically exit.
     * @throws InterruptedException
     */
    public void init() throws InterruptedException {
        keepMonitoring = true;

        if (manager == null) {
            return;
        }

        String availableCaches = getDistributedCacheNamesAsString();

        System.out.println("Caches with replication configured which are available for monitoring are: " + availableCaches);
        if (cacheNameToMonitor == null) {
            System.out.println("No cache name specified for monitoring. Choose one and add it to the command line as " +
                    "the second argument.");
            return;
        } else {
            monitorCacheMessages(cacheNameToMonitor);
        }
    }

    private void monitorCacheMessages(String cacheNameToMonitor) throws InterruptedException {
        String cacheName = cacheNameToMonitor;
        Ehcache cache = manager.getCache(cacheName);
        if (cache == null) {
            System.out.println("No cache named " + cacheName + " exists in the CacheManager configuration.");
        } else {
            System.out.println("Monitoring cache: " + cacheName);
            System.out.println("Cache configuration is: " + cache);
            boolean configuredToReplicate = isConfiguredToReplicate(cache);
            if (!configuredToReplicate) {
                System.out.println("The cache " + cacheName + " is not configured to replicate therefore no messages" +
                        " will be received by it.");
            } else {
                consolePrintingCacheEventListener = new ConsolePrintingCacheEventListener();
                cache.getCacheEventNotificationService().registerListener(consolePrintingCacheEventListener);
                keepMonitoring = true;
            }

            while (keepMonitoring) {
                Thread.sleep(TWO_SECONDS);
                System.out.println("Cache: " + cacheName
                        + " Notifications received: " + consolePrintingCacheEventListener.getEventsReceivedCount()
                        + " Elements in cache: " + cache.getSize());
            }
        }
    }

    boolean isConfiguredToReplicate(Ehcache cache) {
        Iterator listenersIterator = cache.getCacheEventNotificationService().getCacheEventListeners().iterator();
        boolean configuredToReplicate = false;
        while (listenersIterator.hasNext()) {
            CacheEventListener cacheEventListener = (CacheEventListener) listenersIterator.next();
            if (cacheEventListener instanceof CacheReplicator) {
                configuredToReplicate = true;
            }
        }
        return configuredToReplicate;
    }

    /**
     * @return a space separated list of distributed cache names
     */
    String getDistributedCacheNamesAsString() {
        StringBuffer availableCaches = new StringBuffer();
        String[] cacheNames = getDistributedCacheNames();

        for (int i = 0; i < cacheNames.length; i++) {
            String name = cacheNames[i];
            if (isConfiguredToReplicate(manager.getCache(name))) {
                availableCaches.append(name).append(' ');
            }
        }
        return availableCaches.toString();
    }

    /**
     * Gets the distributed cache names
     *
     * @return a list of cache names
     */
    String[] getDistributedCacheNames() {
        String[] cacheNames = manager.getCacheNames();
        return cacheNames;
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

        RemoteDebugger remoteDebugger = null;

        if (args.length < 1 || args.length > 2) {
            System.out.println("Command line to list caches to monitor: java -jar ehcache-remote-debugger.jar path_to_ehcache.xml\n" +
                    "Command line to monitor a specific cache: java -jar ehcache-remote-debugger.jar path_to_ehcache.xml" +
                    " cacheName");
            return;
        } else if (args.length == 1) {
            System.out.println("Attempting to list caches from the specified configuration");
            remoteDebugger = new RemoteDebugger(args[0], null);
        } else {
            System.out.println("This debugger prints all ehcache debugging messages. Set your log handler to the most" +
                    " detailed level to see the messages.");
            remoteDebugger = new RemoteDebugger(args[0], args[1]);
        }
        remoteDebugger.init();
    }

    /**
     * Disposes
     */
    public void dispose() {
        keepMonitoring = false;
        manager.shutdown();
    }

    /**
     * Test access to the CacheManager
     *
     * @return the instance of the CacheManager created by the {@link #main} method.
     */
    static CacheManager getMonitoringCacheManager() {
        return manager;
    }

    /**
     * Call this method to stop monitoring.
     *
     * @param keepMonitoring whether to keep monitoring
     */
    public void stopMonitoring(boolean keepMonitoring) {
        this.keepMonitoring = keepMonitoring;
    }


    /**
     * The event listener listens for events received from the cluster
     *
     * @return the event listener
     */
    public ConsolePrintingCacheEventListener getConsolePrintingCacheEventListener() {
        return consolePrintingCacheEventListener;
    }
}
