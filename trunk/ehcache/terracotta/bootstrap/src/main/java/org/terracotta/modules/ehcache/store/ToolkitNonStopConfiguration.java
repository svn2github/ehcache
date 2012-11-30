/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;

import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopTimeoutBehavior;

public class ToolkitNonStopConfiguration implements NonStopConfiguration {
  private final NonstopConfiguration ehcacheNonStopConfig;

  public ToolkitNonStopConfiguration(final NonstopConfiguration ehcacheNonStopConfig) {
    this.ehcacheNonStopConfig = ehcacheNonStopConfig;
  }

  @Override
  public NonStopTimeoutBehavior getImmutableOpNonStopTimeoutBehavior() {
    return convertEhcacheBehaviorToToolkitBehavior(false);
  }

  @Override
  public NonStopTimeoutBehavior getMutableOpNonStopTimeoutBehavior() {
    return convertEhcacheBehaviorToToolkitBehavior(true);
  }

  protected NonStopTimeoutBehavior convertEhcacheBehaviorToToolkitBehavior(boolean isMutateOp) {
    TimeoutBehaviorConfiguration behaviorConfiguration = ehcacheNonStopConfig.getTimeoutBehavior();
    switch (behaviorConfiguration.getTimeoutBehaviorType()) {
      case EXCEPTION:
        return NonStopTimeoutBehavior.EXCEPTION_ON_TIMEOUT;
      case LOCAL_READS:
        if (isMutateOp) return NonStopTimeoutBehavior.NO_OP;
        else return NonStopTimeoutBehavior.LOCAL_READS;
      case NOOP:
        return NonStopTimeoutBehavior.NO_OP;
      default:
        return NonStopTimeoutBehavior.EXCEPTION_ON_TIMEOUT;
    }
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