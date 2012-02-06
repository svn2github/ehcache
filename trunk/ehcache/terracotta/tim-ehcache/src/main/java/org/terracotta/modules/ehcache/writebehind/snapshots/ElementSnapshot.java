/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind.snapshots;

import net.sf.ehcache.Element;

import org.terracotta.annotations.InstrumentedClass;
import org.terracotta.cache.serialization.SerializationStrategy;

import java.io.IOException;

/**
 * Abstract base class to for creating and restoring snapshots of Ehcache elements
 *
 * @author Geert Bevin
 * @version $Id$
 */
@InstrumentedClass
public abstract class ElementSnapshot {
  private final long version;
  private final long hitCount;
  private final int timeToLive;
  private final int timeToIdle;
  private final long creationTime;
  private final long lastAccessTime;
  private final long lastUpdateTime;

  public ElementSnapshot(Element element) {
    this.version = element.getVersion();
    this.hitCount = element.getHitCount();
    this.timeToLive = element.getTimeToLive();
    this.timeToIdle = element.getTimeToIdle();
    this.creationTime = element.getCreationTime();
    this.lastAccessTime = element.getLastAccessTime();
    this.lastUpdateTime = element.getLastUpdateTime();
  }

  public final Element createElement(SerializationStrategy strategy) throws ClassNotFoundException, IOException {
    return new Element(getKey(strategy), getValue(strategy), version, creationTime, lastAccessTime, hitCount, false, timeToLive, timeToIdle, lastUpdateTime);
  }

  public abstract Object getKey(SerializationStrategy strategy) throws IOException, ClassNotFoundException;

  public abstract Object getValue(SerializationStrategy strategy) throws IOException, ClassNotFoundException;
}
