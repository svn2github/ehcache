/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache;

import org.terracotta.cache.evictor.CapacityEvictionPolicyData;

public class IdentityModeElementData extends AbstractElementData {

  private transient CapacityEvictionPolicyData capacityEvictionPolicyData;

  public IdentityModeElementData(final Element element, long creationTime) {
    super(element.getObjectValue(), element.getVersion(), creationTime == Integer.MIN_VALUE ? element.getCreationTime()
        : creationTime, element.getLastAccessTime(), element.getHitCount(), element.usesCacheDefaultLifespan(), element
        .getTimeToLive(), element.getTimeToIdle(), element.getLastUpdateTime());
  }

  @Override
  public void setCapacityEvictionPolicyData(final CapacityEvictionPolicyData capacityEvictionPolicyData) {
    this.capacityEvictionPolicyData = capacityEvictionPolicyData;
  }

  @Override
  public CapacityEvictionPolicyData getCapacityEvictionPolicyData() {
    return this.capacityEvictionPolicyData;
  }

  @Override
  protected final CapacityEvictionPolicyData fastGetCapacityEvictionPolicyData() {
    return this.capacityEvictionPolicyData;
  }

}
