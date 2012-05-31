/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.config.CacheConfiguration;

/**
 * @author Chris Dennis
 */
public class ValueModeHandlerHibernate extends ValueModeHandlerSerialization {

  public ValueModeHandlerHibernate(ClusteredStore store, CacheConfiguration cacheConfig) {
    super(store, cacheConfig, new HibernateElementSerializationStrategy(cacheConfig.getTerracottaConfiguration()
        .isCompressionEnabled()));
  }
}
