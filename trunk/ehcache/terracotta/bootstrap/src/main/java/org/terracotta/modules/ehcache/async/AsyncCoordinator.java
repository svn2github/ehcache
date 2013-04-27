/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import org.terracotta.modules.ehcache.async.scatterpolicies.ItemScatterPolicy;

import java.io.Serializable;

public interface AsyncCoordinator<E extends Serializable> {

  /**
   * @throws IllegalArgumentException if processingConcurrency is less than 1 OR processor is null
   */
  public void start(final ItemProcessor<E> processor, final int processingConcurrency,
                    ItemScatterPolicy<? super E> policy);

  /**
   * @param item null item are ignored.
   */
  public void add(E item);

  /**
   * Stops and waits for the current processing to finish.<br>
   * Calling this multiple times will result in {@link IllegalStateException}
   */
  public void stop();

  /**
   * Sets a filter to filter out the items.
   */
  public void setOperationsFilter(ItemsFilter<E> filter);

  /**
   * @return the current items to be processed
   */
  public long getQueueSize();

  /**
   * Destroy all clustered state associated with the given async coordinator.
   */
  void destroy();
}
