/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.terracotta;

import net.sf.ehcache.config.TerracottaClientConfiguration;

import org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory;

public class StandaloneTerracottaClusteredInstanceFactory extends TerracottaClusteredInstanceFactory {

  public StandaloneTerracottaClusteredInstanceFactory(final TerracottaClientConfiguration terracottaConfig) {
    super(terracottaConfig);
  }
}
