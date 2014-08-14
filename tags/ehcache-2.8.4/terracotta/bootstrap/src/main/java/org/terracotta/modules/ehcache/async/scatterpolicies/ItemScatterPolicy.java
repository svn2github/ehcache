/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async.scatterpolicies;

import java.io.Serializable;

public interface ItemScatterPolicy<E extends Serializable> {

  /**
   * Returns a bucket index in the range 0 to (count - 1) to which an item should be assigned.
   * 
   * @param count exclusive maximum index
   * @param item the item we are scattering across the buckets
   * @return bucket index to use
   */
  int selectBucket(int count, E item);
}
