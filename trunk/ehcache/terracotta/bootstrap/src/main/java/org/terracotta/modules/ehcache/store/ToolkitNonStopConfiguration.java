/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;

import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields;

// Always sets nonstop behavior as EXCEPTION for toolkit to handle compound operations.
public class ToolkitNonStopConfiguration implements NonStopConfiguration {
  protected final NonstopConfiguration ehcacheNonStopConfig;

  public ToolkitNonStopConfiguration(final NonstopConfiguration ehcacheNonStopConfig) {
    this.ehcacheNonStopConfig = ehcacheNonStopConfig;
  }

  @Override
  public NonStopConfigurationFields.NonStopReadTimeoutBehavior getReadOpNonStopTimeoutBehavior() {
    return convertEhcacheBehaviorToToolkitReadBehavior();
  }

  @Override
  public NonStopConfigurationFields.NonStopWriteTimeoutBehavior getWriteOpNonStopTimeoutBehavior() {
    return convertEhcacheBehaviorToToolkitWriteBehavior();
  }

  @Override
  public long getTimeoutMillis() {
    return ehcacheNonStopConfig.getTimeoutMillis();
  }

  @Override
  public long getSearchTimeoutMillis() {
    return ehcacheNonStopConfig.getSearchTimeoutMillis();
  }

    @Override
  public boolean isEnabled() {
    return ehcacheNonStopConfig.isEnabled();
  }

  @Override
  public boolean isImmediateTimeoutEnabled() {
    return ehcacheNonStopConfig.isImmediateTimeout();
  }

  private NonStopConfigurationFields.NonStopReadTimeoutBehavior convertEhcacheBehaviorToToolkitReadBehavior() {
    TimeoutBehaviorConfiguration behaviorConfiguration = ehcacheNonStopConfig.getTimeoutBehavior();
    switch (behaviorConfiguration.getTimeoutBehaviorType()) {
      case EXCEPTION:
        return NonStopConfigurationFields.NonStopReadTimeoutBehavior.EXCEPTION;
      case LOCAL_READS:
      case LOCAL_READS_AND_EXCEPTION_ON_WRITES:
        return NonStopConfigurationFields.NonStopReadTimeoutBehavior.LOCAL_READS;
      case NOOP:
        return NonStopConfigurationFields.NonStopReadTimeoutBehavior.NO_OP;
      default:
        return NonStopConfigurationFields.DEFAULT_NON_STOP_READ_TIMEOUT_BEHAVIOR;
    }
  }

  private NonStopConfigurationFields.NonStopWriteTimeoutBehavior convertEhcacheBehaviorToToolkitWriteBehavior() {
    TimeoutBehaviorConfiguration behaviorConfiguration = ehcacheNonStopConfig.getTimeoutBehavior();
    switch (behaviorConfiguration.getTimeoutBehaviorType()) {
      case EXCEPTION:
      case LOCAL_READS_AND_EXCEPTION_ON_WRITES:
        return NonStopConfigurationFields.NonStopWriteTimeoutBehavior.EXCEPTION;
      case LOCAL_READS:
        return NonStopConfigurationFields.NonStopWriteTimeoutBehavior.NO_OP;
      case NOOP:
        return NonStopConfigurationFields.NonStopWriteTimeoutBehavior.NO_OP;
      default:
        return NonStopConfigurationFields.DEFAULT_NON_STOP_WRITE_TIMEOUT_BEHAVIOR;
    }
  }

}