/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
 * @version $Id$
 */
public class RMICacheReplicatorFactory extends CacheEventListenerFactory {

    /**
     * A default for the amount of time the replication thread sleeps after it detects the replicationQueue is empty
     * before checking again.
     */
    protected static final int DEFAULT_ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS = 1000;

    private static final Logger LOG = LoggerFactory.getLogger(RMICacheReplicatorFactory.class.getName());
    private static final String REPLICATE_PUTS = "replicatePuts";
    private static final String REPLICATE_PUTS_VIA_COPY = "replicatePutsViaCopy";
    private static final String REPLICATE_UPDATES = "replicateUpdates";
    private static final String REPLICATE_UPDATES_VIA_COPY = "replicateUpdatesViaCopy";
    private static final String REPLICATE_REMOVALS = "replicateRemovals";
    private static final String REPLICATE_ASYNCHRONOUSLY = "replicateAsynchronously";
    private static final String ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS = "asynchronousReplicationIntervalMillis";
    private static final int MINIMUM_REASONABLE_INTERVAL = 10;

    /**
     * Create a <code>CacheEventListener</code> which is also a CacheReplicator.
     * <p/>
     * The defaults if properties are not specified are:
     * <ul>
     * <li>replicatePuts=true
     * <li>replicatePutsViaCopy=true
     * <li>replicateUpdates=true
     * <li>replicateUpdatesViaCopy=true
     * <li>replicateRemovals=true;
     * <li>replicateAsynchronously=true
     * <li>asynchronousReplicationIntervalMillis=1000
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
     *                   asynchronousReplicationIntervalMillis=1000
     *                   "/&gt;</code>
     * @return a constructed CacheEventListener
     */
    public final CacheEventListener createCacheEventListener(Properties properties) {
        boolean replicatePuts = extractReplicatePuts(properties);
        boolean replicatePutsViaCopy = extractReplicatePutsViaCopy(properties);
        boolean replicateUpdates = extractReplicateUpdates(properties);
        boolean replicateUpdatesViaCopy = extractReplicateUpdatesViaCopy(properties);
        boolean replicateRemovals = extractReplicateRemovals(properties);
        boolean replicateAsynchronously = extractReplicateAsynchronously(properties);
        int asynchronousReplicationIntervalMillis = extractReplicationIntervalMilis(properties);

        if (replicateAsynchronously) {
            return new RMIAsynchronousCacheReplicator(
                    replicatePuts,
                    replicatePutsViaCopy,
                    replicateUpdates,
                    replicateUpdatesViaCopy,
                    replicateRemovals,
                    asynchronousReplicationIntervalMillis);
        } else {
            return new RMISynchronousCacheReplicator(
                    replicatePuts,
                    replicatePutsViaCopy,
                    replicateUpdates,
                    replicateUpdatesViaCopy,
                    replicateRemovals);
        }
    }

    /**
     * Extracts the value of asynchronousReplicationIntervalMillis. Sets it to 1000ms if
     * either not set or there is a problem parsing the number
     * @param properties
     */
    protected int extractReplicationIntervalMilis(Properties properties) {
        int asynchronousReplicationIntervalMillis;
        String asynchronousReplicationIntervalMillisString =
                PropertyUtil.extractAndLogProperty(ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS, properties);
        if (asynchronousReplicationIntervalMillisString != null) {
            try {
                int asynchronousReplicationIntervalMillisCandidate =
                        Integer.parseInt(asynchronousReplicationIntervalMillisString);
                if (asynchronousReplicationIntervalMillisCandidate < MINIMUM_REASONABLE_INTERVAL) {
                    LOG.debug("Trying to set the asynchronousReplicationIntervalMillis to an unreasonable number." +
                            " Using the default instead.");
                    asynchronousReplicationIntervalMillis = DEFAULT_ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS;
                } else {
                    asynchronousReplicationIntervalMillis = asynchronousReplicationIntervalMillisCandidate;
                }
            } catch (NumberFormatException e) {
                LOG.warn("Number format exception trying to set asynchronousReplicationIntervalMillis. " +
                        "Using the default instead. String value was: '" + asynchronousReplicationIntervalMillisString + "'");
                asynchronousReplicationIntervalMillis = DEFAULT_ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS;
            }
        } else {
            asynchronousReplicationIntervalMillis = DEFAULT_ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS;
        }
        return asynchronousReplicationIntervalMillis;
    }

    /**
     * Extracts the value of replicateAsynchronously from the properties
     * @param properties
     */
    protected boolean extractReplicateAsynchronously(Properties properties) {
        boolean replicateAsynchronously;
        String replicateAsynchronouslyString = PropertyUtil.extractAndLogProperty(REPLICATE_ASYNCHRONOUSLY, properties);
        if (replicateAsynchronouslyString != null) {
            replicateAsynchronously = PropertyUtil.parseBoolean(replicateAsynchronouslyString);
        } else {
            replicateAsynchronously = true;
        }
        return replicateAsynchronously;
    }

    /**
     * Extracts the value of replicateRemovals from the properties
     * @param properties
     */
    protected boolean extractReplicateRemovals(Properties properties) {
        boolean replicateRemovals;
        String replicateRemovalsString = PropertyUtil.extractAndLogProperty(REPLICATE_REMOVALS, properties);
        if (replicateRemovalsString != null) {
            replicateRemovals = PropertyUtil.parseBoolean(replicateRemovalsString);
        } else {
            replicateRemovals = true;
        }
        return replicateRemovals;
    }

    /**
     * Extracts the value of replicateUpdatesViaCopy from the properties
     * @param properties
     */
    protected boolean extractReplicateUpdatesViaCopy(Properties properties) {
        boolean replicateUpdatesViaCopy;
        String replicateUpdatesViaCopyString = PropertyUtil.extractAndLogProperty(REPLICATE_UPDATES_VIA_COPY, properties);
        if (replicateUpdatesViaCopyString != null) {
            replicateUpdatesViaCopy = PropertyUtil.parseBoolean(replicateUpdatesViaCopyString);
        } else {
            replicateUpdatesViaCopy = true;
        }
        return replicateUpdatesViaCopy;
    }

    /**
     * Extracts the value of replicatePutsViaCopy from the properties
     * @param properties
     */
    protected boolean extractReplicatePutsViaCopy(Properties properties) {
        boolean replicatePutsViaCopy;
        String replicatePutsViaCopyString = PropertyUtil.extractAndLogProperty(REPLICATE_PUTS_VIA_COPY, properties);
        if (replicatePutsViaCopyString != null) {
            replicatePutsViaCopy = PropertyUtil.parseBoolean(replicatePutsViaCopyString);
        } else {
            replicatePutsViaCopy = true;
        }
        return replicatePutsViaCopy;
    }

    /**
     * Extracts the value of replicateUpdates from the properties
     * @param properties
     */
    protected boolean extractReplicateUpdates(Properties properties) {
        boolean replicateUpdates;
        String replicateUpdatesString = PropertyUtil.extractAndLogProperty(REPLICATE_UPDATES, properties);
        if (replicateUpdatesString != null) {
            replicateUpdates = PropertyUtil.parseBoolean(replicateUpdatesString);
        } else {
            replicateUpdates = true;
        }
        return replicateUpdates;
    }

    /**
     * Extracts the value of replicatePuts from the properties
     * @param properties
     */
    protected boolean extractReplicatePuts(Properties properties) {
        boolean replicatePuts;
        String replicatePutsString = PropertyUtil.extractAndLogProperty(REPLICATE_PUTS, properties);
        if (replicatePutsString != null) {
            replicatePuts = PropertyUtil.parseBoolean(replicatePutsString);
        } else {
            replicatePuts = true;
        }
        return replicatePuts;
    }


}
