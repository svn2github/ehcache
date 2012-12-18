/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.config.NonstopConfiguration;

import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopReadTimeoutBehavior;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopWriteTimeoutBehavior;

// Always sets nonstop behavior as EXCEPTION for toolkit to handle compound operations.
public class ToolkitNonStopConfiguration implements NonStopConfiguration {
  private final NonstopConfiguration ehcacheNonStopConfig;

  public ToolkitNonStopConfiguration(final NonstopConfiguration ehcacheNonStopConfig) {
    this.ehcacheNonStopConfig = ehcacheNonStopConfig;
  }

  @Override
  public NonStopConfigurationFields.NonStopReadTimeoutBehavior getReadOpNonStopTimeoutBehavior() {
    return NonStopReadTimeoutBehavior.EXCEPTION;
  }

  @Override
  public NonStopConfigurationFields.NonStopWriteTimeoutBehavior getWriteOpNonStopTimeoutBehavior() {
    return NonStopWriteTimeoutBehavior.EXCEPTION;
  }

  @Override
  public long getTimeoutMillis() {
    return ehcacheNonStopConfig.getTimeoutMillis();
  }

  @Override
  public boolean isEnabled() {
    return ehcacheNonStopConfig.isEnabled();
  }

  @Override
  public boolean isImmediateTimeoutEnabled() {
    return ehcacheNonStopConfig.isImmediateTimeout();
  }

}