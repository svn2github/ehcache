/**
 *  Copyright 2003-2009 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
 * @version $Id: ElementSnapshot.java 21899 2010-04-14 19:44:00Z gbevin $
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
