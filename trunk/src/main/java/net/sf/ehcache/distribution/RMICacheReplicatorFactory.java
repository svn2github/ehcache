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

import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;
import net.sf.ehcache.util.PropertyUtil;

import java.util.Properties;

/**
 * Creates an RMICacheReplicator using properties. Config lines look like:
 * <pre>&lt;cacheEventListenerFactory class="net.sf.ehcache.distribution.RMICacheReplicatorFactory"
 * properties="
 * replicateAsynchronously=true,
 * replicatePuts=true
 * replicateUpdates=true
 * replicateUpdatesViaCopy=true
 * replicateRemovals=true
 * "/&gt;</pre>
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: RMICacheReplicatorFactory.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public class RMICacheReplicatorFactory extends CacheEventListenerFactory {
    private static final String REPLICATE_PUTS = "replicatePuts";
    private static final String REPLICATE_UPDATES = "replicateUpdates";
    private static final String REPLICATE_UPDATES_VIA_COPY = "replicateUpdatesViaCopy";
    private static final String REPLICATE_REMOVALS = "replicateRemovals";
    private static final String REPLICATE_ASYNCHRONOUSLY = "replicateAsynchronously";

    /**
     * Create a <code>CacheEventListener</code> which is also a CacheReplicator.
     * <p/>
     * The defaults if properties are not specified are:
     * <ul>
     * <li>replicatePuts=true
     * <li>replicateUpdates=true
     * <li>replicateUpdatesViaCopy=true
     * <li>replicateRemovals=true;
     * <li>replicateAsynchronously=true
     * </ul>
     *
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml e.g.
     *                   <p/>
     *                   <code>
     *                   &lt;cacheEventListenerFactory class="net.sf.ehcache.distribution.RMICacheReplicatorFactory"
     *                   properties="
     *                   replicateAsynchronously=true,
     *                   replicatePuts=true
     *                   replicateUpdates=true
     *                   replicateUpdatesViaCopy=true
     *                   replicateRemovals=true
     *                   "/&gt;</code>
     * @return a constructed CacheEventListener
     */
    public CacheEventListener createCacheEventListener(Properties properties) {
        boolean replicatePuts = extractReplicatePuts(properties);
        boolean replicateUpdates = extractReplicateUpdates(properties);
        boolean replicateUpdatesViaCopy = extractReplicateUpdatesViaCopy(properties);
        boolean replicateRemovals = extractReplicateRemovals(properties);
        boolean replicateAsynchronously = extractReplicateAsynchronously(properties);

        if (replicateAsynchronously) {
            return new RMIAsynchronousCacheReplicator(
                    replicatePuts,
                    replicateUpdates,
                    replicateUpdatesViaCopy,
                    replicateRemovals);
        } else {
            return new RMISynchronousCacheReplicator(
                    replicatePuts,
                    replicateUpdates,
                    replicateUpdatesViaCopy,
                    replicateRemovals);
        }
    }

    private boolean extractReplicateAsynchronously(Properties properties) {
        boolean replicateAsynchronously;
        String replicateAsynchronouslyString = PropertyUtil.extractAndLogProperty(REPLICATE_ASYNCHRONOUSLY, properties);
        if (replicateAsynchronouslyString != null) {
            replicateAsynchronously = parseBoolean(replicateAsynchronouslyString);
        } else {
            replicateAsynchronously = true;
        }
        return replicateAsynchronously;
    }

    private boolean extractReplicateRemovals(Properties properties) {
        boolean replicateRemovals;
        String replicateRemovalsString = PropertyUtil.extractAndLogProperty(REPLICATE_REMOVALS, properties);
        if (replicateRemovalsString != null) {
            replicateRemovals = parseBoolean(replicateRemovalsString);
        } else {
            replicateRemovals = true;
        }
        return replicateRemovals;
    }

    private boolean extractReplicateUpdatesViaCopy(Properties properties) {
        boolean replicateUpdatesViaCopy;
        String replicateUpdatesViaCopyString = PropertyUtil.extractAndLogProperty(REPLICATE_UPDATES_VIA_COPY, properties);
        if (replicateUpdatesViaCopyString != null) {
            replicateUpdatesViaCopy = parseBoolean(replicateUpdatesViaCopyString);
        } else {
            replicateUpdatesViaCopy = true;
        }
        return replicateUpdatesViaCopy;
    }

    private boolean extractReplicateUpdates(Properties properties) {
        boolean replicateUpdates;
        String replicateUpdatesString = PropertyUtil.extractAndLogProperty(REPLICATE_UPDATES, properties);
        if (replicateUpdatesString != null) {
            replicateUpdates = parseBoolean(replicateUpdatesString);
        } else {
            replicateUpdates = true;
        }
        return replicateUpdates;
    }

    private boolean extractReplicatePuts(Properties properties) {
        boolean replicatePuts;
        String replicatePutsString = PropertyUtil.extractAndLogProperty(REPLICATE_PUTS, properties);
        if (replicatePutsString != null) {
            replicatePuts = parseBoolean(replicatePutsString);
        } else {
            replicatePuts = true;
        }
        return replicatePuts;
    }

    private static boolean parseBoolean(String name) {
        return ((name != null) && name.equalsIgnoreCase("true"));
    }
}
