package net.sf.ehcache.distribution.JMS;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.distribution.CacheManagerPeerListener;
import net.sf.ehcache.distribution.CacheManagerPeerListenerFactory;

public class JMSCacheManagerPeerListenerFactory extends
		CacheManagerPeerListenerFactory {

	private static final Log logger = LogFactory.getLog(JMSCacheManagerPeerProviderFactory.class);
	
	@Override
	public CacheManagerPeerListener createCachePeerListener(
			CacheManager cacheManager, Properties properties) {
		
		if (logger.isTraceEnabled()) {
			logger.trace("createCachePeerListener ( cacheManager = " + cacheManager + ", properties = " + properties + " ) called ");
		}
		
		return null;
	}

}
