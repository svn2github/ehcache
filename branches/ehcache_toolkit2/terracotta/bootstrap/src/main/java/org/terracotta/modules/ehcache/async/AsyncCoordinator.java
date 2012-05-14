/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import org.terracotta.modules.ehcache.async.scatterpolicies.ItemScatterPolicy;

import java.io.Serializable;

public interface AsyncCoordinator<E extends Serializable> {

  public void start(final ItemProcessor<E> processor, final int processingConcurrency,
                    ItemScatterPolicy<? super E> policy);

  public void add(E item);

  public void stop();

  public void setOperationsFilter(ItemsFilter<E> filter);

  public long getQueueSize();
}
