/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.nonstop;

import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopReadTimeoutBehavior;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopWriteTimeoutBehavior;

public class ToolkitNonstopDisableConfig implements NonStopConfiguration {

  @Override
  public NonStopReadTimeoutBehavior getReadOpNonStopTimeoutBehavior() {
    return NonStopReadTimeoutBehavior.EXCEPTION;
  }

  @Override
  public NonStopWriteTimeoutBehavior getWriteOpNonStopTimeoutBehavior() {
    return NonStopWriteTimeoutBehavior.EXCEPTION;
  }

  @Override
  public long getTimeoutMillis() {
    return -1;
  }

  @Override
  public long getSearchTimeoutMillis() {
    return -1;
  }

    @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  public boolean isImmediateTimeoutEnabled() {
    return false;
  }

}