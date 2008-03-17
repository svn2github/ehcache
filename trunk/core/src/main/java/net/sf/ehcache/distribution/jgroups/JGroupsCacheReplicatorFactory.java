/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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

package net.sf.ehcache.distribution.jgroups;

import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;
import net.sf.ehcache.util.PropertyUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;

/**
 * @author Pierre Monestie (pmonestie__REMOVE__THIS__@gmail.com)
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */

public class JGroupsCacheReplicatorFactory extends CacheEventListenerFactory {
    private static final String ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS = "asynchronousReplicationIntervalMillis";

    private static final Log LOG = LogFactory.getLog(JGroupsCacheReplicatorFactory.class);

    private static final String REPLICATE_PUTS = "replicatePuts";

    private static final String REPLICATE_UPDATES = "replicateUpdates";

    private static final String REPLICATE_UPDATES_VIA_COPY = "replicateUpdatesViaCopy";

    private static final String REPLICATE_REMOVALS = "replicateRemovals";

    private static final String REPLICATE_ASYNCHRONOUSLY = "replicateAsynchronously";

    /**
     * Empty arg constructor
     */
    public JGroupsCacheReplicatorFactory() {
    }

    /**
     * {@inheritDoc}
     */
    public CacheEventListener createCacheEventListener(Properties properties) {
        LOG.debug("making new cache rep");
        boolean replicatePuts = extractBooleanProperty(properties, REPLICATE_PUTS, true);
        boolean replicateUpdates = extractBooleanProperty(properties, REPLICATE_UPDATES, true);
        boolean replicateUpdatesViaCopy = extractBooleanProperty(properties, REPLICATE_UPDATES_VIA_COPY, false);
        boolean replicateRemovals = extractBooleanProperty(properties, REPLICATE_REMOVALS, true);
        boolean replicateAsync = extractBooleanProperty(properties, REPLICATE_ASYNCHRONOUSLY, true);
        long asyncTime = extractLongProperty(properties, ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS,
                JGroupsCacheReplicator.DEFAULT_ASYNC_INTERVAL);
        JGroupsCacheReplicator r = new JGroupsCacheReplicator(replicatePuts, replicateUpdates, replicateUpdatesViaCopy,
                replicateRemovals, replicateAsync);
        if (r.isReplicateAsync()) {
            r.setAsynchronousReplicationInterval(asyncTime);
        }
        // if
        // (p.containsKey("replicatePuts")&&p.get("replicatePuts").toString().equals("true"))
        // r.setReplicatePut(true);

        return r;
    }

    /**
     * Extract a long out of a string.
     * 
     * @param properties
     *            the property
     * @param propertyName
     *            the name of the property
     * @param defaultValue
     *            the default value if none is found
     * @return the extracted value
     */
    protected long extractLongProperty(Properties properties, String propertyName, long defaultValue) {
        boolean ret;
        String pString = PropertyUtil.extractAndLogProperty(propertyName, properties);
        if (pString != null) {

            try {
                Long l = new Long(pString);
                return l.longValue();
            } catch (NumberFormatException e) {

            }

        }
        return defaultValue;
    }

    /**
     * Extract a Boolean out of a Property
     * 
     * @param properties
     *            the properties
     * @param propertyName
     *            the name of the property
     * @param defaultValue
     *            the deulat value id none is found
     * @return the extracted property
     */
    protected boolean extractBooleanProperty(Properties properties, String propertyName, boolean defaultValue) {
        boolean ret;
        String pString = PropertyUtil.extractAndLogProperty(propertyName, properties);
        if (pString != null) {
            ret = PropertyUtil.parseBoolean(pString);
        } else {
            ret = defaultValue;
        }
        return ret;
    }
}
