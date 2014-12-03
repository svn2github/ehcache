/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import java.io.Serializable;
import java.util.List;

/**
 * An interface for implementing a filter for quarantined items before they're actually processed. By filtering the
 * outstanding items it's for example possible to remove scheduled work before it's actually executed.
 */

public interface ItemsFilter<E extends Serializable> {

  /**
   * Called before executing an assembled list of work items.
   * <p>
   * The attached list can be freely mutated and will be subsequently used as the list of items to be executed.
   * 
   * @param items list of items to be executed
   */
  public void filter(List<E> items);
}
