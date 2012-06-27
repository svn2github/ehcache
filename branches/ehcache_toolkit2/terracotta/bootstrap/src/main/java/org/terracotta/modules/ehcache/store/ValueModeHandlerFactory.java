/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.config.CacheConfiguration;

public abstract class ValueModeHandlerFactory {

  // private static final Logger LOG = LoggerFactory.getLogger(ValueModeHandlerFactory.class.getName());

  public static ValueModeHandler createValueModeHandler(final ClusteredStore store,
                                                        final CacheConfiguration cacheConfiguration) {

    // TODO: fix this
    // final TerracottaConfiguration terracottaConfiguration = cacheConfiguration.getTerracottaConfiguration();
    // if (hibernateTypesPresent()) {
    // LOG.info("Hibernate types found on the classpath : Enabling Hibernate value mode optimizations");
    // return new ValueModeHandlerHibernate(store, cacheConfiguration.isCopyOnRead(),
    // terracottaConfiguration.isCompressionEnabled());
    // } else {
    // return new ValueModeHandlerSerialization(store, cacheConfiguration.isCopyOnRead(),
    // terracottaConfiguration.isCompressionEnabled());
    // }
    return new ValueModeHandlerSerialization();
  }

  // private static boolean hibernateTypesPresent() {
  // try {
  // Class.forName("org.hibernate.cache.CacheKey");
  // return true;
  // } catch (ClassNotFoundException ex) {
  // return false;
  // }
  // }
}
