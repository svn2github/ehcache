/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

package net.sf.ehcache.distribution.jms;

import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;
import net.sf.ehcache.util.PropertyUtil;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author benoit.perroud@elca.ch
 * @author Greg Luck
 */
public class JMSCacheEventListenerFactory extends CacheEventListenerFactory {

    private static final Logger LOG = Logger.getLogger(JMSCacheEventListenerFactory.class.getName());

    private static final String REPLICATE_PUTS = "replicatePuts";

    private static final String REPLICATE_UPDATES = "replicateUpdates";

    private static final String REPLICATE_UPDATES_VIA_COPY = "replicateUpdatesViaCopy";

    private static final String REPLICATE_REMOVALS = "replicateRemovals";

    private static final String REPLICATE_ASYNCHRONOUSLY = "replicateAsynchronously";

    private static final String ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS = "asynchronousReplicationIntervalMillis";

    /**
     * Create a <code>CacheEventListener</code>
     *
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml
     * @return a constructed CacheEventListener
     */
    @Override
    public CacheEventListener createCacheEventListener(Properties properties) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("createCacheEventListener ( properties = " + properties + " ) called ");
        }

        boolean replicatePuts = extractBooleanProperty(properties,
                REPLICATE_PUTS, false);
        boolean replicateUpdates = extractBooleanProperty(properties,
                REPLICATE_UPDATES, true);
        boolean replicateUpdatesViaCopy = extractBooleanProperty(properties,
                REPLICATE_UPDATES_VIA_COPY, false);
        boolean replicateRemovals = extractBooleanProperty(properties,
                REPLICATE_REMOVALS, false);
        boolean replicateAsync = extractBooleanProperty(properties,
                REPLICATE_ASYNCHRONOUSLY, false);
        long asyncTime = extractAsynchronousReplicationIntervalMillis(
                properties, ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS,
                JMSCacheReplicator.DEFAULT_ASYNC_INTERVAL);

        return new JMSCacheReplicator(replicatePuts, replicateUpdates,
                replicateUpdatesViaCopy, replicateRemovals, replicateAsync, asyncTime);

    }

    /**
     *
     * @param properties
     * @param propertyName
     * @param defaultValue
     * @return
     */
    protected long extractAsynchronousReplicationIntervalMillis(
            Properties properties, String propertyName, long defaultValue) {
        String parsedString = PropertyUtil.extractAndLogProperty(propertyName,
                properties);
        if (parsedString != null) {

            try {
                Long longValue = new Long(parsedString);
                return longValue.longValue();
            } catch (NumberFormatException e) {
                LOG.warning("Number format exception trying to set asynchronousReplicationIntervalMillis. "
                        + "Using the default instead. String value was: '"
                        + parsedString + "'");
            }

        }
        return defaultValue;
    }

    /**
     *
     * @param properties
     * @param propertyName
     * @param defaultValue
     * @return
     */
    protected boolean extractBooleanProperty(Properties properties,
                                             String propertyName, boolean defaultValue) {
        boolean ret;
        String pString = PropertyUtil.extractAndLogProperty(propertyName,
                properties);
        if (pString != null) {
            ret = PropertyUtil.parseBoolean(pString);
        } else {
            ret = defaultValue;
        }
        return ret;
    }

}
