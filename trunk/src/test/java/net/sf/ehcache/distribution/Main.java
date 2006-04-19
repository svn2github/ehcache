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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A main class for manual distributed testing on different computers.
 * <p/>
 * It uses ehcache-distributed1.xml and will in the cluster for the multicast group
 *
 *
 * @author Greg Luck
 * @version $Id$
 */
public final class Main {
    private static final int TWO_SECONDS = 2000;

    private static final Log LOG = LogFactory.getLog(Main.class.getName());

    /**
     * Utility class. No constructor
     */
    private Main() {
        //noop
    }


    /**
     * Designed to work with ehcache-distributed1.xml
     * @param args
     */
    public static void main(String[] args) throws InterruptedException {

        if (args.length != 1) {
            LOG.info("Usage: java -jar ehcache-test.jar path_to_ehcache.xml");
        }

        CacheManager manager = new CacheManager(args[0]);

        Cache cache = manager.getCache("sampleCache1");
        LOG.info("sampleCAche1" + cache);

        while (true) {

            Thread.sleep(TWO_SECONDS);
            LOG.info("Cache size: " + cache.getSize());

        }
    }
}
