/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind.snapshots;

import org.terracotta.annotations.InstrumentedClass;
import org.terracotta.cache.serialization.SerializationStrategy;

import java.io.IOException;

/**
 * Abstract base class to for creating and restoring snapshots of Ehcache keys
 *
 * @author Geert Bevin
 * @version $Id$
 */
@InstrumentedClass
public abstract class KeySnapshot {
  public abstract Object getKey(SerializationStrategy strategy) throws IOException, ClassNotFoundException;
}
