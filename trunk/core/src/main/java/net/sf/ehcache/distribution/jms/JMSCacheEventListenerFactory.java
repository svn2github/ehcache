package net.sf.ehcache.distribution.jms;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;
import net.sf.ehcache.util.PropertyUtil;

public class JMSCacheEventListenerFactory extends CacheEventListenerFactory {

	private static final Log logger = LogFactory
			.getLog(JMSCacheEventListenerFactory.class);

	private static final String REPLICATE_PUTS = "replicatePuts";

	private static final String REPLICATE_UPDATES = "replicateUpdates";

	private static final String REPLICATE_UPDATES_VIA_COPY = "replicateUpdatesViaCopy";

	private static final String REPLICATE_REMOVALS = "replicateRemovals";

	private static final String REPLICATE_ASYNCHRONOUSLY = "replicateAsynchronously";

	private static final String ASYNCHRONOUS_REPLICATION_INTERVAL_MILLIS = "asynchronousReplicationIntervalMillis";

	@Override
	public CacheEventListener createCacheEventListener(Properties properties) {

		if (logger.isTraceEnabled()) {
			logger.trace("createCacheEventListener ( properties = " + properties + " ) called ");
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

	protected long extractAsynchronousReplicationIntervalMillis(
			Properties properties, String propertyName, long defaultValue) {
		String parsedString = PropertyUtil.extractAndLogProperty(propertyName,
				properties);
		if (parsedString != null) {

			try {
				Long longValue = new Long(parsedString);
				return longValue.longValue();
			} catch (NumberFormatException e) {
				logger
						.warn("Number format exception trying to set asynchronousReplicationIntervalMillis. "
								+ "Using the default instead. String value was: '"
								+ parsedString + "'");
			}

		}
		return defaultValue;
	}

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
