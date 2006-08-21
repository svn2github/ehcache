/**
 *  Copyright 2003-2006 Greg Luck
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
 * @version $Id$
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
     * @param args
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
        String cacheName = args[1];
        Ehcache cache = manager.getCache(cacheName);
        if (cache == null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < cacheNames.length; i++) {
                String name = cacheNames[i];
                sb.append(name).append(' ');
            }
            if (cacheName != null) {
                LOG.error("No cache named " + cacheName + " exists. Available caches are: " + sb);
            } else {
                LOG.info("Available caches are: " + sb);
            }
            System.exit(1);
        }
        LOG.info(args[1] + " " + cache);

        while (true) {
            Thread.sleep(TWO_SECONDS);
            LOG.info("Cache size: " + cache.getSize());
        }
    }
}
