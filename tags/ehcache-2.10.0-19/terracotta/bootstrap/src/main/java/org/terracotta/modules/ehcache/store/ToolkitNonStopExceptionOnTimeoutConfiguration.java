/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.config.NonstopConfiguration;

import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopReadTimeoutBehavior;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopWriteTimeoutBehavior;

public class ToolkitNonStopExceptionOnTimeoutConfiguration extends ToolkitNonStopConfiguration {

  public ToolkitNonStopExceptionOnTimeoutConfiguration(NonstopConfiguration ehcacheNonStopConfig) {
    super(ehcacheNonStopConfig);
  }

  @Override
  public NonStopReadTimeoutBehavior getReadOpNonStopTimeoutBehavior() {
    return NonStopReadTimeoutBehavior.EXCEPTION;
  }

  @Override
  public NonStopWriteTimeoutBehavior getWriteOpNonStopTimeoutBehavior() {
    return NonStopWriteTimeoutBehavior.EXCEPTION;
  }

}
