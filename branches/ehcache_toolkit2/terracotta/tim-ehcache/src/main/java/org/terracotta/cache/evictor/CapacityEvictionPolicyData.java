/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.cache.evictor;

/**
 * A datum attached to an entry providing comparison and storage of capacity eviction data.
 * <p>
 * It is important that implementations of
 * {@link CapacityEvictionPolicyData#compareTo(CapacityEvictionPolicyData)}
 * safely handle comparison with a {@code null} object.  Null objects represent
 * untouched values, hence <code>a.compareTo(null)</code> should under a
 * conventional eviction policy return a positive value (untouched values should
 * always be evicted first).
 * 
 * @author abhi.sanoujam
 */
public interface CapacityEvictionPolicyData extends Comparable<CapacityEvictionPolicyData> {

  public static interface Factory {
    /**
     * Creates a new CapacityEvictionPolicyData
     */
    CapacityEvictionPolicyData newCapacityEvictionPolicyData();

    /**
     * Returns true if the parameter capacityEvictionPolicyData is a product of this factory.
     */
    boolean isProductOfFactory(CapacityEvictionPolicyData capacityEvictionPolicyData);
  }

  public void markUsed(int usedAtTime);

}
