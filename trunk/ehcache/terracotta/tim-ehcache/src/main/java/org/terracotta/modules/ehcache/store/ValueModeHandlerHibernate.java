/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

/**
 * @author Chris Dennis
 */
public class ValueModeHandlerHibernate extends ValueModeHandlerSerialization {

  public ValueModeHandlerHibernate(ClusteredStore store, boolean copyOnRead, boolean compress) {
    super(store, copyOnRead, new HibernateElementSerializationStrategy(compress));
  }
}
