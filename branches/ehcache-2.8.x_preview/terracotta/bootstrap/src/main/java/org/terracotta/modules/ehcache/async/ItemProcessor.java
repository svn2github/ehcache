/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import org.terracotta.modules.ehcache.async.exceptions.ProcessingException;

import java.io.Serializable;
import java.util.Collection;

public interface ItemProcessor<E extends Serializable> {
  public void process(E item) throws ProcessingException;

  public void process(Collection<E> items) throws ProcessingException;

  public void throwAway(E item, RuntimeException e);
}
