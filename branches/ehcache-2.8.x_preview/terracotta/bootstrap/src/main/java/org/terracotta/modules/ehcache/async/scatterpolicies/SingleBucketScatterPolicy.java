/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async.scatterpolicies;


import java.io.Serializable;

public class SingleBucketScatterPolicy<E extends Serializable> implements ItemScatterPolicy<E> {

  public int selectBucket(final int count, final E item) {
    return 0;
  }

}
